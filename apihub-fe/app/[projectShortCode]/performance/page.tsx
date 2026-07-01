'use client';
import { useState } from 'react';
import { Container, Box, Typography, Fab } from '@mui/material';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PerformanceRunner from '@/components/PerformanceRunner';
import PerformanceResultsGrid from '@/components/PerformanceResultGrid';
import AiChatDialog from '@/components/AiChatDialog';

interface PageProps {
    params: {
        projectShortCode: string;
    };
}

export default function PerformancePage({ params }: PageProps) {
    const [refreshKey, setRefreshKey] = useState(0);
    const [chatOpen, setChatOpen] = useState(false);

    const handleTestComplete = () => {
        setRefreshKey(prev => prev + 1);
    };

    return (
        <Container maxWidth="xl" sx={{ py: 4 }}>
            <Typography variant="h4" component="h1" gutterBottom>
                Performans Test Runner
            </Typography>

            <Box sx={{ mb: 4 }}>
                <PerformanceRunner
                    projectShortCode={params.projectShortCode}
                    onTestComplete={handleTestComplete}
                />
            </Box>

            <PerformanceResultsGrid
                projectShortCode={params.projectShortCode}
                refreshKey={refreshKey}
            />

            {/* Floating AI Button */}
            <Fab
                color="primary"
                sx={{
                    position: 'fixed',
                    bottom: 30,
                    right: 30,
                    width: 60,
                    height: 60,
                }}
                onClick={() => setChatOpen(true)}
            >
                <SmartToyIcon />
            </Fab>

            <AiChatDialog
                open={chatOpen}
                onClose={() => setChatOpen(false)}
                projectShortCode={params.projectShortCode}
            />
        </Container>
    );
}