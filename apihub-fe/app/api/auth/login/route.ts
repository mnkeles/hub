import { NextResponse } from 'next/server';
import { createAuthSession, getAuthSessionCookieOptions, shouldUseSecureAuthCookies } from '@/lib/authSession';

type LoginRequestBody = {
    username?: string;
    password?: string;
    authType?: string;
};

type LoginResponseBody = {
    accessToken?: string;
    refreshToken?: string;
    [key: string]: unknown;
};

const API_BASE_URL = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053';

async function parseBackendResponse(response: Response): Promise<LoginResponseBody> {
    const contentType = response.headers.get('content-type') || '';

    if (contentType.includes('application/json')) {
        return response.json() as Promise<LoginResponseBody>;
    }

    const message = await response.text();
    return { message };
}

export async function POST(request: Request) {
    let body: LoginRequestBody;

    try {
        body = await request.json() as LoginRequestBody;
    } catch {
        return NextResponse.json({ message: 'Invalid login payload.' }, { status: 400 });
    }

    if (!body.username || !body.password) {
        return NextResponse.json({ message: 'Username and password are required.' }, { status: 400 });
    }

    if (!body.authType || !['local', 'ldap'].includes(body.authType)) {
        return NextResponse.json({ message: 'authType must be "local" or "ldap".' }, { status: 400 });
    }

    let backendResponse: Response;

    try {
        backendResponse = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            cache: 'no-store',
            body: JSON.stringify(body),
        });
    } catch {
        return NextResponse.json({ message: 'Authentication backend is unavailable.' }, { status: 502 });
    }

    const responseBody = await parseBackendResponse(backendResponse);

    if (!backendResponse.ok) {
        return NextResponse.json(responseBody, { status: backendResponse.status });
    }

    if (!responseBody.accessToken || !responseBody.refreshToken) {
        return NextResponse.json({ message: 'Authentication response did not include session tokens.' }, { status: 502 });
    }

    let authSession: Awaited<ReturnType<typeof createAuthSession>>;

    try {
        authSession = await createAuthSession(responseBody.accessToken);
    } catch {
        return NextResponse.json({ message: 'Auth session configuration is invalid.' }, { status: 500 });
    }

    const secureCookies = shouldUseSecureAuthCookies(request);

    const response = NextResponse.json(responseBody, {
        status: backendResponse.status,
        headers: {
            'Cache-Control': 'no-store',
        },
    });

    response.cookies.set({
        ...getAuthSessionCookieOptions(authSession.expiresAt, secureCookies),
        value: authSession.value,
    });

    return response;
}
