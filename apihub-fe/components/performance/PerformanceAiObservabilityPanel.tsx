'use client';

import { Box, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceAiManagementReport, PerformanceInsightReport } from '@/types/performance';

interface Props {
    aiReport?: PerformanceAiManagementReport | null;
    insightReport?: PerformanceInsightReport | null;
}

interface Row {
    label: string;
    value?: string | number | null;
}

function display(value?: string | number | null): string {
    if (value === null || value === undefined || value === '') {
        return '-';
    }
    return String(value);
}

export default function PerformanceAiObservabilityPanel({ aiReport, insightReport }: Props) {
    const t = useTranslations('performance');
    const tokenUsage = [aiReport?.promptTokens, aiReport?.completionTokens, aiReport?.totalTokens]
        .map((value) => (value === null || value === undefined ? '-' : value))
        .join(' / ');
    const rows: Row[] = [
        { label: t('schemaVersion'), value: aiReport?.schemaVersion ?? insightReport?.schemaVersion },
        { label: t('generatedByVersion'), value: aiReport?.generatedByVersion ?? insightReport?.generatedByVersion },
        { label: 'Model', value: aiReport?.model },
        { label: t('durationMsLabel'), value: aiReport?.durationMs },
        { label: t('attemptCount'), value: aiReport?.attemptCount },
        { label: t('failureReason'), value: aiReport?.failureReason },
        { label: t('validationErrors'), value: (aiReport?.validationErrors ?? []).join('; ') },
        { label: t('promptHash'), value: aiReport?.promptHash },
        { label: t('inputSummaryHash'), value: aiReport?.inputSummaryHash },
        { label: t('responseSize'), value: aiReport?.responseSize },
        { label: t('tokenUsage'), value: tokenUsage },
    ];

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                {t('aiObservability')}
            </Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(3, minmax(0, 1fr))' }, gap: 1.25 }}>
                {rows.map((row) => (
                    <Box key={row.label} sx={{ minWidth: 0 }}>
                        <Typography variant="caption" color="text.secondary">{row.label}</Typography>
                        <Typography variant="body2" sx={{ wordBreak: 'break-word' }}>{display(row.value)}</Typography>
                    </Box>
                ))}
            </Box>
        </Paper>
    );
}
