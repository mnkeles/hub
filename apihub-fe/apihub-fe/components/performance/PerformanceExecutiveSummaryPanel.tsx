'use client';

import { Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceAiManagementReport, PerformanceManagementReport } from '@/types/performance';

interface Props {
    managementReport?: PerformanceManagementReport | null;
    aiReport?: PerformanceAiManagementReport | null;
}

export default function PerformanceExecutiveSummaryPanel({ managementReport, aiReport }: Props) {
    const t = useTranslations('performance');
    const summary = aiReport?.generated === true && aiReport.executiveNarrative
        ? aiReport.executiveNarrative
        : managementReport?.executiveSummary;

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                {t('executiveSummary')}
            </Typography>
            <Typography variant="body2">
                {summary || t('reportUnavailable')}
            </Typography>
        </Paper>
    );
}
