'use client';

import React, { useEffect, useState } from 'react';
import {
    Alert,
    Avatar,
    Box,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Divider,
    Paper,
    Typography,
} from '@mui/material';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import DashboardLayout from '@/components/DashboardLayout';
import { CurrentUser, userService } from '@/services/userService';

function formatAuthType(authType?: string): string {
    if (authType?.toLowerCase() === 'ldap') return 'LDAP';
    if (authType?.toLowerCase() === 'local') return 'Local';
    return authType || '-';
}

function getDisplayStatus(enabled?: number): { label: string; color: 'success' | 'default' } {
    return enabled === 1
        ? { label: 'Aktif', color: 'success' }
        : { label: 'Pasif', color: 'default' };
}

function getInitials(user: CurrentUser): string {
    const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim();
    const source = fullName || user.username || 'U';
    return source
        .split(/[\s.@_-]+/u)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0])
        .join('')
        .toUpperCase();
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
    return (
        <Box
            sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', sm: '180px 1fr' },
                gap: { xs: 0.5, sm: 2 },
                py: 1.5,
            }}
        >
            <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
                {label}
            </Typography>
            <Typography variant="body1" sx={{ wordBreak: 'break-word' }}>
                {value}
            </Typography>
        </Box>
    );
}

export default function UserPage() {
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let isMounted = true;

        const loadCurrentUser = async () => {
            setLoading(true);
            setError(null);

            try {
                const data = await userService.getCurrentUser();
                if (isMounted) {
                    setCurrentUser(data);
                }
            } catch (err) {
                console.error('Current user could not be loaded:', err);
                if (isMounted) {
                    setError('Kullanıcı bilgisi alınamadı');
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        };

        void loadCurrentUser();

        return () => {
            isMounted = false;
        };
    }, []);

    const status = currentUser ? getDisplayStatus(currentUser.enabled) : null;

    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
                    Kullanıcı Bilgileri
                </Typography>

                <Paper sx={{ p: { xs: 2, md: 3 }, maxWidth: 760 }}>
                    {loading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}>
                            <CircularProgress />
                        </Box>
                    ) : error ? (
                        <Alert severity="error">{error}</Alert>
                    ) : currentUser ? (
                        <Card variant="outlined" sx={{ borderRadius: 2 }}>
                            <CardContent sx={{ p: { xs: 2, md: 3 } }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                                    <Avatar sx={{ width: 64, height: 64, bgcolor: 'primary.main', fontSize: '1.25rem' }}>
                                        {getInitials(currentUser) || <AccountCircleIcon />}
                                    </Avatar>
                                    <Box>
                                        <Typography variant="h6" sx={{ fontWeight: 700 }}>
                                            {`${currentUser.firstName || ''} ${currentUser.lastName || ''}`.trim() || currentUser.username}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            {currentUser.username}
                                        </Typography>
                                    </Box>
                                </Box>

                                <Divider sx={{ mb: 1 }} />

                                <InfoRow label="Kullanıcı Adı" value={currentUser.username || '-'} />
                                <InfoRow label="Giriş Tipi" value={formatAuthType(currentUser.authType)} />
                                <InfoRow label="Ad" value={currentUser.firstName || ''} />
                                <InfoRow label="Soyad" value={currentUser.lastName || ''} />
                                <InfoRow
                                    label="Durum"
                                    value={status ? <Chip label={status.label} color={status.color} size="small" /> : '-'}
                                />
                            </CardContent>
                        </Card>
                    ) : (
                        <Alert severity="info">Kullanıcı bilgisi bulunamadı</Alert>
                    )}
                </Paper>
            </Box>
        </DashboardLayout>
    );
}

