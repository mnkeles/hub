import { NextResponse } from 'next/server';
import { getExpiredAuthSessionCookieOptions, shouldUseSecureAuthCookies } from '@/lib/authSession';

export async function POST(request: Request) {
    const secureCookies = shouldUseSecureAuthCookies(request);

    const response = NextResponse.json(
        { success: true },
        {
            headers: {
                'Cache-Control': 'no-store',
            },
        },
    );

    response.cookies.set({
        ...getExpiredAuthSessionCookieOptions(secureCookies),
        value: '',
    });

    return response;
}
