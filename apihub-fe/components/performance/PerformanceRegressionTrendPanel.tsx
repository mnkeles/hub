'use client';

import { Alert, Box, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceComparisonMetric, PerformanceComparisonResult, PerformanceInsightReport } from '@/types/performance';

interface Props {
    insightReport?: PerformanceInsightReport | null;
    baselineComparison?: PerformanceComparisonResult | null;
    trendSummary?: string | null;
}

function groupMetrics(metrics: PerformanceComparisonMetric[]) {
    return {
        improved: metrics.filter((metric) => metric.improvement === true),
        regressed: metrics.filter((metric) => metric.improvement === false),
        informational: metrics.filter((metric) => metric.improvement === null || metric.improvement === undefined),
    };
}

function valueText(value: unknown): string {
    if (value === null || value === undefined || value === '') return '-';
    return String(value);
}

export default function PerformanceRegressionTrendPanel({ insightReport, baselineComparison, trendSummary }: Props) {
    const t = useTranslations('performance');
    const metrics = baselineComparison?.metrics ?? [];
    const groups = groupMetrics(metrics);

    const renderGroup = (title: string, items: PerformanceComparisonMetric[], color: 'success' | 'error' | 'default') => {
        if (items.length === 0) return null;
        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                <Typography variant="caption" color="text.secondary">{title}</Typography>
                {items.map((metric) => (
                    <Box key={`${metric.metricName}-${valueText(metric.delta)}`} sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                        <Chip size="small" color={color} label={metric.metricName} />
                        <Typography variant="body2">
                            {valueText(metric.baseValue)} {'->'} {valueText(metric.targetValue)} ({valueText(metric.delta)})
                        </Typography>
                    </Box>
                ))}
            </Box>
        );
    };

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                {t('trendSummary')}
            </Typography>
            <Typography variant="body2" sx={{ mb: 1 }}>
                {trendSummary || t('regressionUnavailable')}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700, mb: 1 }}>
                {t('regressionScore')}: {insightReport?.regressionAvailable ? insightReport.regressionScore?.toFixed(2) : t('regressionUnavailable')}
            </Typography>
            {metrics.length === 0 ? (
                <Alert severity="info">{t('regressionUnavailable')}</Alert>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {renderGroup(t('passed'), groups.improved, 'success')}
                    {renderGroup(t('failed'), groups.regressed, 'error')}
                    {renderGroup(t('analysis'), groups.informational, 'default')}
                </Box>
            )}
        </Paper>
    );
}
