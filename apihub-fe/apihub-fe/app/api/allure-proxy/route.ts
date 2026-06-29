import { NextRequest, NextResponse } from 'next/server';
import { getErrorMessage } from '@/lib/errorUtils';

// Project to Allure port mapping
const PROJECT_ALLURE_PORTS: { [key: string]: number } = {
    'OMNI': 8090,
    'OPERA': 8086,
    'DARWIN': 8090, // Changed from 8090 to avoid conflict with OMNI
    'DHIRAAGU': 8084,
    'MTS': 8090, // Default to OMNI port if not specified
    'TOGG': 8090, // Default to OMNI port if not specified
    'XDSL': 4055,
};

// Project to Allure host mapping (for projects with different hosts)
const PROJECT_ALLURE_HOSTS: { [key: string]: string } = {
    'XDSL': '10.6.121.146',
};

export async function GET(request: NextRequest) {
    try {
        const searchParams = request.nextUrl.searchParams;
        const endpoint = searchParams.get('endpoint') || 'suites.json';
        const project = searchParams.get('project') || 'OMNI';
        
        // Get port for the project, default to 8090
        const port = PROJECT_ALLURE_PORTS[project.toUpperCase()] || 8090;
        
        // Get host for the project, default to localhost
        const host = PROJECT_ALLURE_HOSTS[project.toUpperCase()] || 'localhost';
        
        const allureUrl = `http://${host}:${port}/data/${endpoint}`;
        
        console.log('Fetching from Allure:', allureUrl, 'for project:', project);
        
        const response = await fetch(allureUrl, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
            },
        });

        console.log('Allure response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Allure error response:', errorText);
            return NextResponse.json(
                { error: `Failed to fetch from Allure: ${response.statusText}`, details: errorText },
                { status: response.status }
            );
        }

        const contentType = response.headers.get('content-type');
        console.log('Content-Type:', contentType);

        const data = await response.json();
        console.log('Allure data received:', JSON.stringify(data).substring(0, 200));
        
        return NextResponse.json(data, {
            headers: {
                'Cache-Control': 'no-store, max-age=0',
            },
        });
    } catch (error) {
        console.error('Allure proxy error:', error);
        return NextResponse.json(
            { error: getErrorMessage(error, 'Failed to fetch Allure data') },
            { status: 500 }
        );
    }
}
