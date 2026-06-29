'use client';

import React from 'react';
import {
    Box,
    Typography,
    Paper,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Avatar,
    Chip,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import DashboardLayout from '@/components/DashboardLayout';

const users = [
    { id: 1, name: 'Ahmet Yılmaz', email: 'ahmet.yilmaz@example.com', role: 'Admin', status: 'active', lastLogin: '2 hours ago' },
    { id: 2, name: 'Ayşe Demir', email: 'ayse.demir@example.com', role: 'Developer', status: 'active', lastLogin: '5 hours ago' },
    { id: 3, name: 'Mehmet Kaya', email: 'mehmet.kaya@example.com', role: 'Developer', status: 'active', lastLogin: '1 day ago' },
    { id: 4, name: 'Fatma Şahin', email: 'fatma.sahin@example.com', role: 'Viewer', status: 'active', lastLogin: '3 days ago' },
    { id: 5, name: 'Ali Çelik', email: 'ali.celik@example.com', role: 'Developer', status: 'inactive', lastLogin: '2 weeks ago' },
];

export default function UsersPage() {
    const getRoleColor = (role: string) => {
        switch (role) {
            case 'Admin':
                return 'error';
            case 'Developer':
                return 'primary';
            case 'Viewer':
                return 'default';
            default:
                return 'default';
        }
    };

    const getInitials = (name: string) => {
        return name
            .split(' ')
            .map((n) => n[0])
            .join('')
            .toUpperCase();
    };

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                        Users
                    </Typography>
                    <Button
                        variant="contained"
                        startIcon={<PersonAddIcon />}
                        sx={{ textTransform: 'none' }}
                    >
                        Add User
                    </Button>
                </Box>

                <Paper sx={{ width: '100%', overflow: 'hidden' }}>
                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                                    <TableCell sx={{ fontWeight: 600 }}>User</TableCell>
                                    <TableCell sx={{ fontWeight: 600 }}>Email</TableCell>
                                    <TableCell sx={{ fontWeight: 600 }}>Role</TableCell>
                                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                                    <TableCell sx={{ fontWeight: 600 }}>Last Login</TableCell>
                                    <TableCell sx={{ fontWeight: 600 }}>Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {users.map((user) => (
                                    <TableRow key={user.id} hover>
                                        <TableCell>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                                <Avatar sx={{ width: 40, height: 40, bgcolor: 'primary.main' }}>
                                                    {getInitials(user.name)}
                                                </Avatar>
                                                <Typography variant="body1" sx={{ fontWeight: 500 }}>
                                                    {user.name}
                                                </Typography>
                                            </Box>
                                        </TableCell>
                                        <TableCell>{user.email}</TableCell>
                                        <TableCell>
                                            <Chip
                                                label={user.role}
                                                color={getRoleColor(user.role)}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Chip
                                                label={user.status}
                                                color={user.status === 'active' ? 'success' : 'default'}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>{user.lastLogin}</TableCell>
                                        <TableCell>
                                            <Button size="small" sx={{ textTransform: 'none' }}>
                                                Edit
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Paper>
            </Box>
        </DashboardLayout>
    );
}
