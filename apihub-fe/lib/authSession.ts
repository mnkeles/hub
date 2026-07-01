import { webcrypto as crypto } from 'node:crypto';

export const AUTH_SESSION_COOKIE_NAME = 'apihub-auth-session';

type AuthCookieRequestContext = {
    url: string;
    headers: Headers;
};

type AuthSessionPayload = {
    authenticated: true;
    issuedAt: number;
    expiresAt: number;
};

const DEFAULT_SESSION_TTL_MS = 30 * 60 * 1000;
const encoder = new TextEncoder();
const decoder = new TextDecoder();
const DEVELOPMENT_SESSION_SECRET = 'development-only-auth-session-secret-change-me';

let emergencyProductionSessionSecret: string | null = null;
let hasLoggedEmergencySecretWarning = false;

function getEmergencyProductionSessionSecret(): string {
    if (!emergencyProductionSessionSecret) {
        const randomBytes = new Uint8Array(48);
        crypto.getRandomValues(randomBytes);
        emergencyProductionSessionSecret = Buffer.from(randomBytes).toString('base64url');
    }

    if (!hasLoggedEmergencySecretWarning) {
        console.error(
            'AUTH_SESSION_SECRET is missing or shorter than 32 characters. Using an ephemeral in-memory fallback secret. Configure a stable 32+ character AUTH_SESSION_SECRET to avoid session resets after restart and multi-instance inconsistencies.',
        );
        hasLoggedEmergencySecretWarning = true;
    }

    return emergencyProductionSessionSecret;
}

function getSessionSecret(): string {
    const configuredSecret = process.env.AUTH_SESSION_SECRET?.trim();

    if (configuredSecret && configuredSecret.length >= 32) {
        return configuredSecret;
    }

    if (process.env.NODE_ENV !== 'production') {
        return DEVELOPMENT_SESSION_SECRET;
    }

    return getEmergencyProductionSessionSecret();
}

function toBase64Url(bytes: Uint8Array): string {
    return Buffer.from(bytes).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/u, '');
}

function fromBase64Url(value: string): Uint8Array {
    const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
    const paddingLength = (4 - (normalized.length % 4)) % 4;
    const padded = `${normalized}${'='.repeat(paddingLength)}`;
    return new Uint8Array(Buffer.from(padded, 'base64'));
}

function encodePayload(payload: AuthSessionPayload): string {
    return toBase64Url(encoder.encode(JSON.stringify(payload)));
}

function decodePayload(value: string): AuthSessionPayload {
    const json = decoder.decode(fromBase64Url(value));
    return JSON.parse(json) as AuthSessionPayload;
}

async function getSigningKey(): Promise<CryptoKey> {
    return crypto.subtle.importKey(
        'raw',
        encoder.encode(getSessionSecret()),
        { name: 'HMAC', hash: 'SHA-256' },
        false,
        ['sign', 'verify'],
    );
}

async function signValue(value: string): Promise<string> {
    const key = await getSigningKey();
    const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(value));
    return toBase64Url(new Uint8Array(signature));
}

function extractJwtExpiry(accessToken?: string): number | null {
    if (!accessToken) {
        return null;
    }

    const segments = accessToken.split('.');
    if (segments.length < 2) {
        return null;
    }

    try {
        const payload = JSON.parse(decoder.decode(fromBase64Url(segments[1]))) as { exp?: number };
        if (typeof payload.exp !== 'number') {
            return null;
        }

        const expiry = payload.exp * 1000;
        return Number.isFinite(expiry) ? expiry : null;
    } catch {
        return null;
    }
}

function resolveExpiry(accessToken?: string): number {
    const jwtExpiry = extractJwtExpiry(accessToken);

    if (jwtExpiry && jwtExpiry > Date.now()) {
        return jwtExpiry;
    }

    return Date.now() + DEFAULT_SESSION_TTL_MS;
}

export async function createAuthSession(accessToken?: string): Promise<{ value: string; expiresAt: number }> {
    const payload: AuthSessionPayload = {
        authenticated: true,
        issuedAt: Date.now(),
        expiresAt: resolveExpiry(accessToken),
    };
    const encodedPayload = encodePayload(payload);
    const signature = await signValue(encodedPayload);

    return {
        value: `${encodedPayload}.${signature}`,
        expiresAt: payload.expiresAt,
    };
}

export async function readAuthSession(value?: string | null): Promise<AuthSessionPayload | null> {
    if (!value) {
        return null;
    }

    const [encodedPayload, signature] = value.split('.');
    if (!encodedPayload || !signature) {
        return null;
    }

    try {
        const key = await getSigningKey();
        const isValid = await crypto.subtle.verify(
            'HMAC',
            key,
            fromBase64Url(signature),
            encoder.encode(encodedPayload),
        );

        if (!isValid) {
            return null;
        }

        const payload = decodePayload(encodedPayload);
        if (payload.authenticated !== true) {
            return null;
        }

        if (!Number.isFinite(payload.expiresAt) || payload.expiresAt <= Date.now()) {
            return null;
        }

        return payload;
    } catch {
        return null;
    }
}

export function shouldUseSecureAuthCookies(request: AuthCookieRequestContext): boolean {
    const forwardedProto = request.headers.get('x-forwarded-proto')?.split(',')[0]?.trim().toLowerCase();

    if (forwardedProto === 'https') {
        return true;
    }

    if (forwardedProto === 'http') {
        return false;
    }

    try {
        return new URL(request.url).protocol === 'https:';
    } catch {
        return process.env.NODE_ENV === 'production';
    }
}

export function getAuthSessionCookieOptions(expiresAt: number, secure: boolean) {
    return {
        name: AUTH_SESSION_COOKIE_NAME,
        httpOnly: true,
        sameSite: 'lax' as const,
        secure,
        expires: new Date(expiresAt),
        path: '/',
    };
}

export function getExpiredAuthSessionCookieOptions(secure: boolean) {
    return {
        name: AUTH_SESSION_COOKIE_NAME,
        httpOnly: true,
        sameSite: 'lax' as const,
        secure,
        expires: new Date(0),
        path: '/',
    };
}
