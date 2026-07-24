'use client';

import { Box, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import FloatingChat from '@/components/FloatingChat';
import PerformanceTestsContent from '@/components/performance/PerformanceTestsContent';
import { useProject } from '@/contexts/ProjectContext';

export default function PerformanceTestsPage() {
    const t = useTranslations('performance');
    const { selectedProject } = useProject();

    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
                    {t('title')}
                </Typography>

                <PerformanceTestsContent
                    key={selectedProject?.projectId ?? 'no-project'}
                    useDashboardProjectContext
                />

                <FloatingChat
                    title={t('assistantTitle')}
                    subtitle={t('assistantSubtitle')}
                    suggestions={[
                        t('assistantSuggestionSlowest'),
                        t('assistantSuggestionTrend'),
                        t('assistantSuggestionOptimize'),
                    ]}
                    position="bottom-right"
                    bottomOffset={96}
                    projectShortCode={selectedProject?.shortCode}
                />
            </Box>
        </DashboardLayout>
    );
}
