'use client';

import { Alert, Box, Chip, Divider, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import {
    PerformanceInsightReport,
    PerformanceInsightSeverity,
    PerformanceManagementReport,
} from '@/types/performance';

interface Props {
    insightReport?: PerformanceInsightReport | null;
    managementReport?: PerformanceManagementReport | null;
}

function severityColor(value?: PerformanceInsightSeverity | null): 'info' | 'warning' | 'error' | 'default' {
    if (value === 'INFO') return 'info';
    if (value === 'WARNING') return 'warning';
    if (value === 'HIGH' || value === 'CRITICAL') return 'error';
    return 'default';
}

export default function PerformanceTechnicalFindingsPanel({ insightReport, managementReport }: Props) {
    const t = useTranslations('performance');
    const metricInsights = insightReport?.metricInsights ?? [];
    const problemAreas = managementReport?.problemAreas ?? [];

    if (!insightReport && !managementReport) {
        return <Alert severity="info">{t('noInsightReport')}</Alert>;
    }

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                {t('technicalFindings')}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
                <Chip size="small" label={`${t('bottleneckType')}: ${insightReport?.bottleneckType ?? '-'}`} />
                <Chip size="small" label={`${t('anomalyScore')}: ${insightReport?.anomalyScore?.toFixed(2) ?? '-'}`} />
            </Box>

            {metricInsights.length === 0 && problemAreas.length === 0 ? (
                <Alert severity="info">{t('noInsightReport')}</Alert>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {metricInsights.map((item, index) => (
                        <Box key={`${item.metric ?? 'metric'}-${index}`}>
                            {index > 0 && <Divider sx={{ mb: 1.5 }} />}
                            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                                <Typography variant="body2" sx={{ fontWeight: 700 }}>{item.metric || '-'}</Typography>
                                <Chip size="small" color={severityColor(item.severity)} label={item.severity || '-'} />
                            </Box>
                            <Typography variant="caption" color="text.secondary">
                                {t('actual')}: {item.actual || '-'} | {t('expected')}: {item.expected || '-'}
                            </Typography>
                            <Typography variant="body2">{item.explanation || '-'}</Typography>
                        </Box>
                    ))}
                    {problemAreas.map((area, index) => (
                        <Box key={`${area.title}-${area.stepName ?? index}`}>
                            {(metricInsights.length > 0 || index > 0) && <Divider sx={{ mb: 1.5 }} />}
                            <Typography variant="body2" sx={{ fontWeight: 700 }}>{area.title}</Typography>
                            <Typography variant="caption" color="text.secondary">
                                {area.stepName || '-'} | {area.metric || '-'}: {area.value || '-'}
                            </Typography>
                            <Typography variant="body2">{t('impact')}: {area.impact || '-'}</Typography>
                        </Box>
                    ))}
                </Box>
            )}
        </Paper>
    );
}
