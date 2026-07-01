'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Box, CircularProgress, Typography } from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import { useTranslations } from 'next-intl';

export default function LogoutPage() {
    const t = useTranslations('logout');
    const router = useRouter();

    useEffect(() => {
        // Clear all auth data
        void fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'same-origin',
        }).catch(() => undefined);
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('tokenTimestamp');
        localStorage.removeItem('user');
        sessionStorage.clear();
        
        // Clear cookies (except NEXT_LOCALE for language preference)
        document.cookie.split(";").forEach((c) => {
            const cookieName = c.split("=")[0].trim();
            if (cookieName !== 'NEXT_LOCALE') {
                document.cookie = c
                    .replace(/^ +/, "")
                    .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
            }
        });

        // Redirect to login after a short delay
        const timer = setTimeout(() => {
            router.push('/login');
        }, 1500);

        return () => clearTimeout(timer);
    }, [router]);

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '100vh',
                backgroundColor: 'background.default',
                gap: 3
            }}
        >
            <LogoutIcon sx={{ fontSize: 64, color: 'primary.main' }} />
            <Typography variant="h5" sx={{ fontWeight: 600, color: 'text.primary' }}>
                {t('title')}
            </Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                {t('message')}
            </Typography>
            <CircularProgress size={40} />
        </Box>
    );
}
