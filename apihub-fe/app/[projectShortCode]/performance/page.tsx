'use client';

import { use, useState } from 'react';
import { Container, Fab, Typography } from '@mui/material';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import AiChatDialog from '@/components/AiChatDialog';
import PerformanceTestsContent from '@/components/performance/PerformanceTestsContent';

interface PageProps {
    params: Promise<{
        projectShortCode: string;
    }>;
}

export default function PerformancePage({ params }: PageProps) {
    const { projectShortCode } = use(params);
    const [chatOpen, setChatOpen] = useState(false);

    return (
        <Container maxWidth="xl" sx={{ py: 4 }}>
            <Typography variant="h4" component="h1" gutterBottom>
                Performans Test Runner
            </Typography>

            <PerformanceTestsContent
                key={projectShortCode}
                projectShortCode={projectShortCode}
                useDashboardProjectContext={false}
            />

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
                projectShortCode={projectShortCode}
            />
        </Container>
    );
}
