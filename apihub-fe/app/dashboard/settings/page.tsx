'use client';

import React from 'react';
import {
    Box,
    Typography,
    Paper,
    TextField,
    Button,
    Switch,
    FormControlLabel,
    Divider,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import DashboardLayout from '@/components/DashboardLayout';

export default function SettingsPage() {
    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
                    Settings
                </Typography>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
                        <Paper sx={{ p: 3 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                General Settings
                            </Typography>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                <TextField
                                    label="Application Name"
                                    defaultValue="APIHUB"
                                    fullWidth
                                />
                                <TextField
                                    label="Admin Email"
                                    defaultValue="admin@example.com"
                                    fullWidth
                                />
                                <TextField
                                    label="Support Email"
                                    defaultValue="support@example.com"
                                    fullWidth
                                />
                                <Button
                                    variant="contained"
                                    startIcon={<SaveIcon />}
                                    sx={{ textTransform: 'none', mt: 2 }}
                                >
                                    Save Changes
                                </Button>
                            </Box>
                        </Paper>

                        <Paper sx={{ p: 3 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                Notification Settings
                            </Typography>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                <FormControlLabel
                                    control={<Switch defaultChecked />}
                                    label="Email notifications for failed tests"
                                />
                                <FormControlLabel
                                    control={<Switch defaultChecked />}
                                    label="Daily performance reports"
                                />
                                <FormControlLabel
                                    control={<Switch />}
                                    label="Weekly summary emails"
                                />
                                <FormControlLabel
                                    control={<Switch defaultChecked />}
                                    label="System alerts"
                                />
                            </Box>
                        </Paper>
                    </Box>

                    <Paper sx={{ p: 3 }}>
                        <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                            Performance Test Settings
                        </Typography>
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 2 }}>
                            <TextField
                                label="Default Timeout (ms)"
                                defaultValue="5000"
                                type="number"
                                fullWidth
                            />
                            <TextField
                                label="Max Concurrent Tests"
                                defaultValue="10"
                                type="number"
                                fullWidth
                            />
                            <TextField
                                label="Retry Attempts"
                                defaultValue="3"
                                type="number"
                                fullWidth
                            />
                        </Box>
                        <Divider sx={{ my: 3 }} />
                        <Button
                            variant="contained"
                            startIcon={<SaveIcon />}
                            sx={{ textTransform: 'none' }}
                        >
                            Save Settings
                        </Button>
                    </Paper>
                </Box>
            </Box>
        </DashboardLayout>
    );
}
