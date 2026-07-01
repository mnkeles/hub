'use client';
import React, { useState } from 'react';
import {
    Container,
    Box,
    Paper,
    TextField,
    Button,
    Typography,
    Alert,
    CircularProgress,
    InputAdornment,
    IconButton,
    Tabs,
    Tab,
} from '@mui/material';
import { Visibility, VisibilityOff, Login as LoginIcon } from '@mui/icons-material';
import { useAuth } from '@/contexts/AuthContext';
import type { AuthType } from '@/services/authService';

type TabIndex = 0 | 1;
const TAB_AUTH_TYPE: Record<TabIndex, AuthType> = { 0: 'ldap', 1: 'local' };

export default function LoginPage() {
    const [activeTab, setActiveTab] = useState<TabIndex>(0);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();

    const authType = TAB_AUTH_TYPE[activeTab];

    const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
        setActiveTab(newValue as TabIndex);
        setError('');
        setUsername('');
        setPassword('');
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await login({ username, password, authType });
        } catch (err: unknown) {
            const authErr = err as { response?: { data?: { message?: string } }; message?: string };
            setError(
                authErr.response?.data?.message ||
                authErr.message ||
                'Giriş başarısız. Kullanıcı adı veya şifre hatalı.'
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <Container maxWidth="sm">
            <Box
                sx={{
                    minHeight: '100vh',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                }}
            >
                <Paper
                    elevation={3}
                    sx={{
                        width: '100%',
                        borderRadius: 2,
                        overflow: 'hidden',
                    }}
                >
                    <Tabs
                        value={activeTab}
                        onChange={handleTabChange}
                        variant="fullWidth"
                        sx={{ borderBottom: 1, borderColor: 'divider' }}
                    >
                        <Tab label="LDAP User" disabled={loading} />
                        <Tab label="Local User" disabled={loading} />
                    </Tabs>

                    <Box sx={{ p: 4 }}>
                        <Box sx={{ textAlign: 'center', mb: 3 }}>
                            <LoginIcon sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
                            <Typography variant="h5" component="h1" gutterBottom>
                                Etiya Mock Platform
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {activeTab === 0
                                    ? 'LDAP / Active Directory hesabınızla giriş yapın'
                                    : 'Yerel hesabınızla giriş yapın'}
                            </Typography>
                        </Box>

                        {error && (
                            <Alert severity="error" sx={{ mb: 3 }}>
                                {error}
                            </Alert>
                        )}

                        <form onSubmit={handleSubmit}>
                            <TextField
                                fullWidth
                                label="Kullanıcı Adı"
                                variant="outlined"
                                margin="normal"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                autoComplete="username"
                                autoFocus
                                disabled={loading}
                                placeholder={
                                    activeTab === 0
                                        ? 'LDAP kullanıcı adı veya e-posta (necati.keles@etiya.com)'
                                        : 'Kullanıcı adı veya e-posta'
                                }
                            />

                            <TextField
                                fullWidth
                                label="Şifre"
                                type={showPassword ? 'text' : 'password'}
                                variant="outlined"
                                margin="normal"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                autoComplete="current-password"
                                disabled={loading}
                                slotProps={{
                                    input: {
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <IconButton
                                                    onClick={() => setShowPassword(!showPassword)}
                                                    edge="end"
                                                    disabled={loading}
                                                >
                                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    },
                                }}
                            />

                            <Button
                                type="submit"
                                fullWidth
                                variant="contained"
                                size="large"
                                disabled={loading}
                                sx={{ mt: 3, mb: 2, py: 1.5 }}
                            >
                                {loading ? (
                                    <CircularProgress size={24} color="inherit" />
                                ) : (
                                    'Giriş Yap →'
                                )}
                            </Button>
                        </form>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
}
