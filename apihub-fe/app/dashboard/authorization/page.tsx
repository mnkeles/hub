'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Checkbox,
    Chip,
    CircularProgress,
    Divider,
    Grid,
    List,
    ListItemButton,
    ListItemText,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import { useAuth } from '@/contexts/AuthContext';
import DashboardLayout from '@/components/DashboardLayout';
import {
    authorizationService,
    PermissionItem,
    ServiceAccountItem,
    ServiceTokenCreateResponse,
    UserSummary,
} from '@/services/authorizationService';

export default function AuthorizationPage() {
    const { hasAnyPermission } = useAuth();
    const canManageAuthorization = hasAnyPermission(['PERMISSION.MANAGE', 'SERVICE_ACCOUNT.MANAGE', 'AUDIT_LOG.VIEW']);
    const [permissions, setPermissions] = useState<PermissionItem[]>([]);
    const [users, setUsers] = useState<UserSummary[]>([]);
    const [serviceAccounts, setServiceAccounts] = useState<ServiceAccountItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [createdToken, setCreatedToken] = useState<ServiceTokenCreateResponse | null>(null);

    // User permission management state
    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [userPermissionIds, setUserPermissionIds] = useState<Set<number>>(new Set());
    const [userSearchQuery, setUserSearchQuery] = useState('');
    const [loadingUserPerms, setLoadingUserPerms] = useState(false);
    const [savingPerms, setSavingPerms] = useState(false);

    // Service account state
    const [serviceCode, setServiceCode] = useState('');
    const [serviceName, setServiceName] = useState('');
    const [serviceOwner, setServiceOwner] = useState('');
    const [selectedServiceAccountId, setSelectedServiceAccountId] = useState<number | null>(null);
    const [tokenName, setTokenName] = useState('');

    const groupedPermissions = useMemo(() => {
        return permissions.reduce<Record<string, PermissionItem[]>>((groups, permission) => {
            const category = permission.category || 'Other';
            groups[category] = groups[category] || [];
            groups[category].push(permission);
            return groups;
        }, {});
    }, [permissions]);

    const filteredUsers = useMemo(() => {
        if (!userSearchQuery.trim()) return users;
        const q = userSearchQuery.toLowerCase();
        return users.filter(u =>
            u.email.toLowerCase().includes(q) ||
            (u.firstName?.toLowerCase().includes(q) ?? false) ||
            (u.lastName?.toLowerCase().includes(q) ?? false)
        );
    }, [users, userSearchQuery]);

    const loadData = useCallback(async () => {
        if (!canManageAuthorization) {
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            setError(null);
            const [permissionData, userData, serviceAccountData] = await Promise.all([
                authorizationService.getPermissions(),
                authorizationService.getUsers(),
                authorizationService.getServiceAccounts(),
            ]);
            setPermissions(permissionData);
            setUsers(userData);
            setServiceAccounts(serviceAccountData);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Authorization data could not be loaded.';
            setError(message);
        } finally {
            setLoading(false);
        }
    }, [canManageAuthorization]);

    useEffect(() => {
        void loadData();
    }, [loadData]);

    const handleSelectUser = useCallback(async (userId: number) => {
        setSelectedUserId(userId);
        setLoadingUserPerms(true);
        setError(null);
        setSuccess(null);
        try {
            const response = await authorizationService.getUserPermissions(userId);
            setUserPermissionIds(new Set(response.permissions.map(p => p.permissionId)));
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Could not load user permissions.';
            setError(message);
            setUserPermissionIds(new Set());
        } finally {
            setLoadingUserPerms(false);
        }
    }, []);

    const togglePermission = (permissionId: number) => {
        setUserPermissionIds(prev => {
            const next = new Set(prev);
            if (next.has(permissionId)) {
                next.delete(permissionId);
            } else {
                next.add(permissionId);
            }
            return next;
        });
    };

    const handleSavePermissions = async () => {
        if (!selectedUserId) return;
        setSavingPerms(true);
        setError(null);
        setSuccess(null);
        try {
            await authorizationService.assignUserPermissions(selectedUserId, {
                permissionIds: Array.from(userPermissionIds),
            });
            setSuccess('Permissions updated successfully.');
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Could not save permissions.';
            setError(message);
        } finally {
            setSavingPerms(false);
        }
    };

    const handleCreateServiceAccount = async () => {
        if (!serviceCode.trim() || !serviceName.trim()) {
            setError('Service code and name are required.');
            return;
        }

        try {
            setError(null);
            await authorizationService.createServiceAccount({
                serviceCode: serviceCode.trim(),
                name: serviceName.trim(),
                owner: serviceOwner.trim() || undefined,
            });
            setServiceCode('');
            setServiceName('');
            setServiceOwner('');
            await loadData();
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Service account could not be created.';
            setError(message);
        }
    };

    const handleCreateServiceToken = async () => {
        if (!selectedServiceAccountId || !tokenName.trim()) {
            setError('Select a service account and enter a token name.');
            return;
        }

        try {
            setError(null);
            const token = await authorizationService.createServiceToken(selectedServiceAccountId, {
                tokenName: tokenName.trim(),
            });
            setTokenName('');
            setCreatedToken(token);
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Service token could not be created.';
            setError(message);
        }
    };

    if (!canManageAuthorization) {
        return (
            <DashboardLayout requiredPermission="MENU.AUTHORIZATION.VIEW">
                <Box sx={{ p: 3 }}>
                    <Typography variant="h5" color="error">Unauthorized</Typography>
                    <Typography variant="body2">You do not have permission to manage authorization.</Typography>
                </Box>
            </DashboardLayout>
        );
    }

    if (loading) {
        return (
            <DashboardLayout requiredPermission="MENU.AUTHORIZATION.VIEW">
                <Box sx={{ p: 3, display: 'flex', justifyContent: 'center' }}>
                    <CircularProgress />
                </Box>
            </DashboardLayout>
        );
    }

    const selectedUser = users.find(u => u.userId === selectedUserId);

    return (
        <DashboardLayout requiredPermission="MENU.AUTHORIZATION.VIEW">
            <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
            <Typography variant="h4" sx={{ mb: 1, fontWeight: 700 }}>Authorization</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Manage user permissions, service accounts, and tokens
            </Typography>

            {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }} onClose={() => setError(null)}>{error}</Alert>}
            {success && <Alert severity="success" sx={{ mb: 2, borderRadius: 2 }} onClose={() => setSuccess(null)}>{success}</Alert>}
            {createdToken && (
                <Alert severity="warning" sx={{ mb: 2, borderRadius: 2 }} onClose={() => setCreatedToken(null)}>
                    Copy this service token now. It will not be shown again: <strong>{createdToken.token}</strong>
                </Alert>
            )}

            <Grid container spacing={3}>
                {/* User List */}
                <Grid size={{ xs: 12, md: 3 }}>
                    <Card sx={{ borderRadius: 2, height: '70vh', display: 'flex', flexDirection: 'column' }}>
                        <CardContent sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                            <Typography variant="h6" sx={{ mb: 1.5, fontWeight: 600 }}>Users</Typography>
                            <TextField
                                placeholder="Search users..."
                                size="small"
                                fullWidth
                                value={userSearchQuery}
                                onChange={(e) => setUserSearchQuery(e.target.value)}
                                sx={{ mb: 1.5 }}
                            />
                            <Box sx={{ flex: 1, overflow: 'auto' }}>
                                <List dense>
                                    {filteredUsers.map((user) => (
                                        <ListItemButton
                                            key={user.userId}
                                            selected={selectedUserId === user.userId}
                                            onClick={() => handleSelectUser(user.userId)}
                                            sx={{
                                                borderRadius: 1,
                                                mb: 0.5,
                                                '&.Mui-selected': {
                                                    backgroundColor: 'primary.main',
                                                    color: 'primary.contrastText',
                                                    '&:hover': { backgroundColor: 'primary.dark' },
                                                },
                                            }}
                                        >
                                            <ListItemText
                                                primary={(
                                                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                                        {user.firstName || user.lastName
                                                            ? `${user.firstName || ''} ${user.lastName || ''}`.trim()
                                                            : user.email}
                                                    </Typography>
                                                )}
                                                secondary={(
                                                    <Typography variant="caption" sx={{ opacity: 0.7, display: 'block' }}>
                                                        {user.email}
                                                    </Typography>
                                                )}
                                            />
                                        </ListItemButton>
                                    ))}
                                </List>
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                {/* Permission Management */}
                <Grid size={{ xs: 12, md: 6 }}>
                    <Card sx={{ borderRadius: 2, height: '70vh', display: 'flex', flexDirection: 'column' }}>
                        <CardContent sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                                <Box>
                                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                                        {selectedUser ? `Permissions: ${selectedUser.email}` : 'Permissions'}
                                    </Typography>
                                    {selectedUser && (
                                        <Typography variant="caption" color="text.secondary">
                                            {userPermissionIds.size} permission(s) assigned
                                        </Typography>
                                    )}
                                </Box>
                                {selectedUserId && (
                                    <Button
                                        variant="contained"
                                        size="small"
                                        onClick={handleSavePermissions}
                                        disabled={savingPerms || loadingUserPerms}
                                    >
                                        {savingPerms ? <CircularProgress size={20} /> : 'Save'}
                                    </Button>
                                )}
                            </Box>

                            {!selectedUserId ? (
                                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1 }}>
                                    <Typography variant="body2" color="text.secondary">
                                        Select a user to manage their permissions
                                    </Typography>
                                </Box>
                            ) : loadingUserPerms ? (
                                <Box sx={{ display: 'flex', justifyContent: 'center', flex: 1, alignItems: 'center' }}>
                                    <CircularProgress size={24} />
                                </Box>
                            ) : (
                                <Box sx={{ flex: 1, overflow: 'auto' }}>
                                    <Stack spacing={2}>
                                        {Object.entries(groupedPermissions).map(([category, categoryPermissions]) => (
                                            <Box key={category}>
                                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1, color: 'primary.main' }}>
                                                    {category}
                                                </Typography>
                                                <Stack spacing={0.5}>
                                                    {categoryPermissions.map((permission) => {
                                                        const checked = userPermissionIds.has(permission.permissionId);
                                                        return (
                                                            <Box
                                                                key={permission.permissionId}
                                                                onClick={() => togglePermission(permission.permissionId)}
                                                                sx={{
                                                                    display: 'flex',
                                                                    alignItems: 'center',
                                                                    gap: 1,
                                                                    px: 1.5,
                                                                    py: 1,
                                                                    borderRadius: 1,
                                                                    cursor: 'pointer',
                                                                    '&:hover': { backgroundColor: 'action.hover' },
                                                                }}
                                                            >
                                                                <Checkbox
                                                                    checked={checked}
                                                                    size="small"
                                                                    sx={{ p: 0.5 }}
                                                                />
                                                                <Box sx={{ flex: 1 }}>
                                                                    <Typography variant="body2" sx={{ fontWeight: checked ? 600 : 400 }}>
                                                                        {permission.permissionKey}
                                                                    </Typography>
                                                                    {permission.description && (
                                                                        <Typography variant="caption" color="text.secondary">
                                                                            {permission.description}
                                                                        </Typography>
                                                                    )}
                                                                </Box>
                                                            </Box>
                                                        );
                                                    })}
                                                </Stack>
                                            </Box>
                                        ))}
                                    </Stack>
                                </Box>
                            )}
                        </CardContent>
                    </Card>
                </Grid>

                {/* Service Accounts */}
                <Grid size={{ xs: 12, md: 3 }}>
                    <Card sx={{ borderRadius: 2, height: '70vh', display: 'flex', flexDirection: 'column' }}>
                        <CardContent sx={{ p: 2, flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                            <Typography variant="h6" sx={{ mb: 1.5, fontWeight: 600 }}>Service Accounts</Typography>

                            <Box sx={{ flex: 1, overflow: 'auto', mb: 2 }}>
                                <Stack spacing={1}>
                                    {serviceAccounts.map((account) => (
                                        <Box
                                            key={account.serviceAccountId}
                                            onClick={() => setSelectedServiceAccountId(account.serviceAccountId)}
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'space-between',
                                                px: 1.5,
                                                py: 1,
                                                borderRadius: 1,
                                                cursor: 'pointer',
                                                border: '1px solid',
                                                borderColor: selectedServiceAccountId === account.serviceAccountId ? 'primary.main' : 'divider',
                                                backgroundColor: selectedServiceAccountId === account.serviceAccountId ? 'action.selected' : 'transparent',
                                                '&:hover': { borderColor: 'primary.main' },
                                            }}
                                        >
                                            <Box>
                                                <Typography variant="body2" sx={{ fontWeight: 600 }}>{account.serviceCode}</Typography>
                                                <Typography variant="caption" sx={{ opacity: 0.7 }}>{account.name}</Typography>
                                            </Box>
                                            <Chip
                                                size="small"
                                                label={account.enabled === 1 ? 'Active' : 'Disabled'}
                                                color={account.enabled === 1 ? 'success' : 'default'}
                                            />
                                        </Box>
                                    ))}
                                </Stack>
                            </Box>

                            <Divider sx={{ my: 1 }} />

                            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>New Service Account</Typography>
                            <Stack spacing={1} sx={{ mb: 2 }}>
                                <TextField label="Code" size="small" fullWidth value={serviceCode} onChange={(e) => setServiceCode(e.target.value)} />
                                <TextField label="Name" size="small" fullWidth value={serviceName} onChange={(e) => setServiceName(e.target.value)} />
                                <TextField label="Owner" size="small" fullWidth value={serviceOwner} onChange={(e) => setServiceOwner(e.target.value)} />
                                <Button variant="contained" fullWidth size="small" onClick={handleCreateServiceAccount}>Create</Button>
                            </Stack>

                            <Divider sx={{ my: 1 }} />

                            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>Generate Token</Typography>
                            <Stack spacing={1}>
                                <TextField
                                    label="Token Name"
                                    size="small"
                                    fullWidth
                                    value={tokenName}
                                    onChange={(e) => setTokenName(e.target.value)}
                                    disabled={!selectedServiceAccountId}
                                />
                                <Button
                                    variant="contained"
                                    fullWidth
                                    size="small"
                                    color="secondary"
                                    onClick={handleCreateServiceToken}
                                    disabled={!selectedServiceAccountId}
                                >
                                    Create Token
                                </Button>
                            </Stack>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>
            </Box>
        </DashboardLayout>
    );
}
