import { NextRequest, NextResponse } from 'next/server';
import { AUTH_SESSION_COOKIE_NAME, readAuthSession } from '@/lib/authSession';

const PUBLIC_PATHS = new Set(['/', '/landing', '/login', '/logout']);

function isPublicPath(pathname: string): boolean {
    if (PUBLIC_PATHS.has(pathname)) {
        return true;
    }

    if (pathname.startsWith('/api/')) {
        return true;
    }

    if (pathname.startsWith('/_next/')) {
        return true;
    }

    if (pathname === '/favicon.ico' || pathname === '/robots.txt' || pathname === '/sitemap.xml') {
        return true;
    }

    return pathname.includes('.');
}

function isSuspiciousRscRequest(request: NextRequest): boolean {
    const acceptHeader = request.headers.get('accept') || '';

    return request.headers.has('next-action')
        || request.nextUrl.searchParams.has('_rsc')
        || acceptHeader.includes('text/x-component');
}

function getClientAddress(request: NextRequest): string {
    return request.headers.get('x-forwarded-for')?.split(',')[0]?.trim()
        || request.headers.get('x-real-ip')
        || 'unknown';
}

export async function proxy(request: NextRequest) {
    const { pathname } = request.nextUrl;

    if (isPublicPath(pathname)) {
        return NextResponse.next();
    }

    const session = await readAuthSession(request.cookies.get(AUTH_SESSION_COOKIE_NAME)?.value);

    if (!session) {
        const suspiciousRscRequest = isSuspiciousRscRequest(request);

        if (suspiciousRscRequest) {
            console.warn('[security] Blocked unauthenticated RSC request', {
                method: request.method,
                pathname,
                clientAddress: getClientAddress(request),
                userAgent: request.headers.get('user-agent') || 'unknown',
            });

            return NextResponse.json({ message: 'Unauthorized' }, { status: 401 });
        }

        const loginUrl = new URL('/login', request.url);
        loginUrl.searchParams.set('redirect', pathname);
        return NextResponse.redirect(loginUrl);
    }

    return NextResponse.next();
}

export const config = {
    matcher: ['/((?!api|_next/static|_next/image|favicon.ico|robots.txt|sitemap.xml).*)'],
};
