'use client';
import { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { Box, CircularProgress } from '@mui/material';

interface ProtectedRouteProps {
    children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
    const { isAuthenticated, isLoading } = useAuth();
    const router = useRouter();
    const pathname = usePathname();
    const isPublicPath = pathname === '/' || pathname === '/login' || pathname === '/landing';

    useEffect(() => {
        if (!isLoading && !isAuthenticated && !isPublicPath) {
            router.replace('/login');
        }
    }, [isAuthenticated, isLoading, isPublicPath, router]);

    if (isLoading) {
        return (
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    minHeight: '100vh',
                }}
            >
                <CircularProgress />
            </Box>
        );
    }

    if (!isAuthenticated && !isPublicPath) {
        return null;
    }

    return <>{children}</>;
}
