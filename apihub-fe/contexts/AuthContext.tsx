'use client';
import React, { createContext, useContext, useState, useEffect, useCallback, useMemo, ReactNode } from 'react';
import { authService, LoginCredentials, AuthUser } from '@/services/authService';
import { useRouter } from 'next/navigation';

interface AuthContextType {
    user: AuthUser | null;
    permissions: string[];
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
    hasPermission: (permissionKey: string) => boolean;
    hasAnyPermission: (permissionKeys: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<AuthUser | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const router = useRouter();

    const permissions = useMemo(() => user?.permissions || [], [user?.permissions]);

    const hasPermission = useCallback((permissionKey: string) => {
        return permissions.includes('*') || permissions.includes('SUPER_USER') || permissions.includes(permissionKey);
    }, [permissions]);

    const hasAnyPermission = useCallback((permissionKeys: string[]) => {
        return permissions.includes('*') || permissions.includes('SUPER_USER') || permissionKeys.some((permissionKey) => permissions.includes(permissionKey));
    }, [permissions]);

    const logout = useCallback(() => {
        authService.logout();
        setUser(null);
        router.push('/login');
    }, [router]);

    const refreshAccessToken = useCallback(async () => {
        try {
            const response = await authService.refreshToken();
            const savedUser = authService.getUser();
            if (response.user || savedUser) {
                setUser(savedUser);
            }
            console.log('Token refreshed successfully');
        } catch (error) {
            console.error('Token refresh failed:', error);
            logout();
        }
    }, [logout]);

    useEffect(() => {
        const token = authService.getToken();
        const refreshToken = authService.getRefreshToken();
        let initialRefreshTimeoutId: ReturnType<typeof setTimeout> | null = null;
        
        if (!token || !refreshToken) {
            return;
        }

        const REFRESH_INTERVAL = 25 * 60 * 1000;
        
        const tokenTimestamp = authService.getTokenTimestamp();
        if (tokenTimestamp) {
            const elapsed = Date.now() - tokenTimestamp;
            const remaining = REFRESH_INTERVAL - elapsed;
            
            if (remaining <= 0) {
                initialRefreshTimeoutId = setTimeout(() => {
                    void refreshAccessToken();
                }, 0);
            }
        }

        const intervalId = setInterval(() => {
            refreshAccessToken();
        }, REFRESH_INTERVAL);

        return () => {
            clearInterval(intervalId);

            if (initialRefreshTimeoutId) {
                clearTimeout(initialRefreshTimeoutId);
            }
        };
    }, [refreshAccessToken, user]);

    useEffect(() => {
        const token = authService.getToken();
        const refreshToken = authService.getRefreshToken();
        const savedUser = authService.getUser();

        const initializeAuth = async () => {
            if (token && refreshToken && savedUser) {
                setUser(savedUser);

                try {
                    await authService.refreshToken();
                    setUser(authService.getUser());
                } catch (error) {
                    console.error('Initial auth refresh failed:', error);
                    authService.logout();
                    setUser(null);
                }
            }

            setIsLoading(false);
        };

        void initializeAuth();
    }, []);

    const login = async (credentials: LoginCredentials) => {
        try {
            await authService.login(credentials);
            const savedUser = authService.getUser();
            if (savedUser) {
                setUser(savedUser);
            }
            window.location.href = '/dashboard';
        } catch (error) {
            console.error('Login failed:', error);
            throw error;
        }
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                permissions,
                isAuthenticated: !!user,
                isLoading,
                login,
                logout,
                hasPermission,
                hasAnyPermission,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
