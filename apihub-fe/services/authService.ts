export type AuthType = 'local' | 'ldap';

export interface LoginCredentials {
    username: string;
    password: string;
    authType: AuthType;
}

type AuthServiceErrorResponse = {
    message?: string;
    [key: string]: unknown;
};

type AuthServiceError = Error & {
    response?: {
        status: number;
        data: AuthServiceErrorResponse;
    };
};

export interface AuthUser {
    id: string;
    username: string;
    email?: string;
    authType?: string;
    permissions?: string[];
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    user?: AuthUser;
}

export interface RefreshTokenResponse {
    accessToken: string;
    refreshToken: string;
    user?: AuthUser;
}

async function parseAuthResponse<T>(response: Response): Promise<T> {
    const contentType = response.headers.get('content-type') || '';

    if (contentType.includes('application/json')) {
        return response.json() as Promise<T>;
    }

    const text = await response.text();
    return { message: text } as T;
}

function createAuthError(status: number, data: AuthServiceErrorResponse): AuthServiceError {
    const errorMessage = typeof data.message === 'string' && data.message.length > 0
        ? data.message
        : 'Authentication request failed';
    const error = new Error(errorMessage) as AuthServiceError;
    error.response = {
        status,
        data,
    };
    return error;
}

const getPublicKey = async (): Promise<string | null> => {
    try {
        const response = await fetch('/api/auth/public-key');
        if (!response.ok) return null;
        const data = await response.json() as { publicKey: string };
        return data.publicKey;
    } catch {
        return null;
    }
};

const encryptPassword = async (password: string, publicKeyPem: string): Promise<string> => {
    const { JSEncrypt } = await import('jsencrypt');
    const encrypt = new JSEncrypt();
    encrypt.setPublicKey(publicKeyPem);
    const encrypted = encrypt.encrypt(password);
    if (!encrypted) throw new Error('RSA şifreleme başarısız');
    return encrypted;
};

const normalizeUser = (user: AuthUser | undefined, username: string): AuthUser => ({
    id: user?.id || 'user',
    username: user?.username || username,
    email: user?.email,
    authType: user?.authType,
    permissions: user?.permissions || [],
});

export const authService = {
    login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
        const publicKey = await getPublicKey();
        const finalPassword = publicKey
            ? await encryptPassword(credentials.password, publicKey)
            : credentials.password;

        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                username: credentials.username,
                password: finalPassword,
                authType: credentials.authType,
            }),
        });
        const responseData = await parseAuthResponse<LoginResponse | AuthServiceErrorResponse>(response);

        if (!response.ok) {
            throw createAuthError(response.status, responseData as AuthServiceErrorResponse);
        }

        const loginResponse = responseData as LoginResponse;

        if (loginResponse.accessToken && loginResponse.refreshToken) {
            localStorage.setItem('token', loginResponse.accessToken);
            localStorage.setItem('refreshToken', loginResponse.refreshToken);
            localStorage.setItem('user', JSON.stringify(normalizeUser(loginResponse.user, credentials.username)));
            localStorage.setItem('tokenTimestamp', Date.now().toString());
        }
        return loginResponse;
    },

    refreshToken: async (): Promise<RefreshTokenResponse> => {
        const refreshToken = authService.getRefreshToken();
        if (!refreshToken) {
            throw new Error('No refresh token available');
        }

        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'same-origin',
            body: JSON.stringify({ refreshToken }),
        });
        const responseData = await parseAuthResponse<RefreshTokenResponse | AuthServiceErrorResponse>(response);

        if (!response.ok) {
            throw createAuthError(response.status, responseData as AuthServiceErrorResponse);
        }

        const refreshResponse = responseData as RefreshTokenResponse;

        if (refreshResponse.accessToken && refreshResponse.refreshToken) {
            localStorage.setItem('token', refreshResponse.accessToken);
            localStorage.setItem('refreshToken', refreshResponse.refreshToken);
            if (refreshResponse.user) {
                localStorage.setItem('user', JSON.stringify(normalizeUser(refreshResponse.user, refreshResponse.user.username)));
            }
            localStorage.setItem('tokenTimestamp', Date.now().toString());
        }

        return refreshResponse;
    },

    logout: () => {
        if (typeof window !== 'undefined') {
            void fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
            }).catch(() => undefined);
        }
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        localStorage.removeItem('tokenTimestamp');
    },

    getToken: (): string | null => {
        if (typeof window !== 'undefined') {
            return localStorage.getItem('token');
        }
        return null;
    },

    getRefreshToken: (): string | null => {
        if (typeof window !== 'undefined') {
            return localStorage.getItem('refreshToken');
        }
        return null;
    },

    getTokenTimestamp: (): number | null => {
        if (typeof window !== 'undefined') {
            const timestamp = localStorage.getItem('tokenTimestamp');
            return timestamp ? parseInt(timestamp, 10) : null;
        }
        return null;
    },

    getUser: (): AuthUser | null => {
        if (typeof window !== 'undefined') {
            const userStr = localStorage.getItem('user');
            return userStr ? JSON.parse(userStr) : null;
        }
        return null;
    },

    getPermissions: (): string[] => authService.getUser()?.permissions || [],

    hasPermission: (permission: string): boolean => {
        const permissions = authService.getPermissions();
        return permissions.includes('*') || permissions.includes(permission);
    },

    hasAnyPermission: (permissions: string[]): boolean => permissions.some((permission) => authService.hasPermission(permission)),

    isAuthenticated: (): boolean => {
        return !!authService.getToken();
    }
};
