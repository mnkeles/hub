import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053';

export async function GET() {
    let backendResponse: Response;

    try {
        backendResponse = await fetch(`${API_BASE_URL}/auth/public-key`, {
            method: 'GET',
            cache: 'no-store',
        });
    } catch {
        return NextResponse.json({ message: 'Authentication backend is unavailable.' }, { status: 502 });
    }

    if (!backendResponse.ok) {
        return NextResponse.json({ message: 'Public key could not be retrieved.' }, { status: backendResponse.status });
    }

    const data = await backendResponse.json();
    return NextResponse.json(data, {
        status: 200,
        headers: { 'Cache-Control': 'no-store' },
    });
}
