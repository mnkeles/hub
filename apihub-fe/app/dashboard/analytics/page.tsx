'use client';

import React from 'react';
import {
    Box,
    Typography,
    Paper,
} from '@mui/material';
import DashboardLayout from '@/components/DashboardLayout';

export default function AnalyticsPage() {
    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
                    Analytics
                </Typography>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
                        <Paper sx={{ p: 3, height: 300, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <Typography variant="h6" color="text.secondary">
                                API Usage Chart
                            </Typography>
                        </Paper>
                        <Paper sx={{ p: 3, height: 300, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <Typography variant="h6" color="text.secondary">
                                Response Time Trends
                            </Typography>
                        </Paper>
                    </Box>
                    <Paper sx={{ p: 3, height: 300, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Typography variant="h6" color="text.secondary">
                            Error Rate Analysis
                        </Typography>
                    </Paper>
                </Box>
            </Box>
        </DashboardLayout>
    );
}
