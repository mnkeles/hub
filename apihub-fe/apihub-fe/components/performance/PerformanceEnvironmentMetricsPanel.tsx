'use client';

import { Alert, Box, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceEnvironmentMetrics } from '@/types/performance';
import { dash, formatDurationMs, formatPercent } from './PerformanceMetricFormatters';

interface PerformanceEnvironmentMetricsPanelProps {
    metrics?: PerformanceEnvironmentMetrics | null;
}

function MetricRow({ label, value }: { label: string; value: string }) {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, py: 0.5 }}>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>{value}</Typography>
        </Box>
    );
}

export default function PerformanceEnvironmentMetricsPanel({ metrics }: PerformanceEnvironmentMetricsPanelProps) {
    const t = useTranslations('performance');

    if (!metrics || metrics.metricsAvailable === false) {
        return <Alert severity="info">{metrics?.message || t('metricsUnavailable')}</Alert>;
    }

    return (
        <Box>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                <Box>
                    <MetricRow label="CPU Avg" value={formatPercent(metrics.cpuAvgPercent)} />
                    <MetricRow label="CPU Max" value={formatPercent(metrics.cpuMaxPercent)} />
                    <MetricRow label="Memory Avg" value={formatPercent(metrics.memoryAvgPercent)} />
                    <MetricRow label="Memory Max" value={formatPercent(metrics.memoryMaxPercent)} />
                    <MetricRow label="JVM Heap Max" value={formatPercent(metrics.jvmHeapMaxPercent)} />
                    <MetricRow label="GC Time" value={formatDurationMs(metrics.gcTimeMs)} />
                </Box>
                <Box>
                    <MetricRow label="DB Active" value={dash(metrics.dbActiveConnectionMax)} />
                    <MetricRow label="DB Pool" value={dash(metrics.dbConnectionPoolSize)} />
                    <MetricRow label="Slow SQL" value={dash(metrics.slowSqlCount)} />
                    <MetricRow label="HTTP 5xx" value={dash(metrics.http5xxCount)} />
                    <MetricRow label="Pod Restart" value={dash(metrics.podRestartCount)} />
                </Box>
            </Box>
            {metrics.warnings && metrics.warnings.length > 0 && (
                <Alert severity="warning" sx={{ mt: 2 }}>
                    {metrics.warnings.join(' ')}
                </Alert>
            )}
        </Box>
    );
}
