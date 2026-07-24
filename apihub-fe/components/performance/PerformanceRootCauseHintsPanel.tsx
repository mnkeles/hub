'use client';

import { Alert, Box, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceInsightReport, PerformanceInsightSeverity } from '@/types/performance';

interface Props {
    insightReport?: PerformanceInsightReport | null;
}

function severityColor(value?: PerformanceInsightSeverity | null): 'info' | 'warning' | 'error' | 'default' {
    if (value === 'INFO') return 'info';
    if (value === 'WARNING') return 'warning';
    if (value === 'HIGH' || value === 'CRITICAL') return 'error';
    return 'default';
}

export default function PerformanceRootCauseHintsPanel({ insightReport }: Props) {
    const t = useTranslations('performance');
    const hints = insightReport?.rootCauseHints ?? [];

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                {t('rootCauseHints')}
            </Typography>
            {hints.length === 0 ? (
                <Alert severity="info">{t('noRootCauseHints')}</Alert>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {hints.map((hint, index) => (
                        <Box key={`${hint.category ?? 'hint'}-${hint.signal ?? index}`}>
                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 0.5 }}>
                                <Chip size="small" color={severityColor(hint.severity)} label={hint.severity || '-'} />
                                <Chip size="small" label={hint.category || '-'} />
                                <Chip size="small" label={hint.signal || '-'} />
                            </Box>
                            <Typography variant="body2">{hint.explanation || '-'}</Typography>
                            <Typography variant="caption" color="text.secondary">{hint.recommendation || '-'}</Typography>
                        </Box>
                    ))}
                </Box>
            )}
        </Paper>
    );
}
