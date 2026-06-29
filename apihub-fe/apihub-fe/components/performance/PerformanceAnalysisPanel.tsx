'use client';

import { Alert, Box, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import {
    PerformanceAnalysisSummary,
    PerformanceEnvironmentMetrics,
    PerformanceErrorAnalysis,
    PerformanceThresholdResult,
} from '@/types/performance';
import { dash, formatPercent } from './PerformanceMetricFormatters';
import PerformanceEnvironmentMetricsPanel from './PerformanceEnvironmentMetricsPanel';
import PerformanceFailureReasons from './PerformanceFailureReasons';
import PerformanceThresholdStatusChip from './PerformanceThresholdStatusChip';

interface PerformanceAnalysisPanelProps {
    analysis?: PerformanceAnalysisSummary | null;
    thresholdResult?: PerformanceThresholdResult | null;
    errorAnalysis?: PerformanceErrorAnalysis | null;
    environmentMetrics?: PerformanceEnvironmentMetrics | null;
}

function Field({ label, value }: { label: string; value?: string | null }) {
    return (
        <Box>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>{dash(value)}</Typography>
        </Box>
    );
}

export default function PerformanceAnalysisPanel({ analysis, thresholdResult, errorAnalysis, environmentMetrics }: PerformanceAnalysisPanelProps) {
    const t = useTranslations('performance');
    const effectiveThreshold = thresholdResult ?? analysis?.thresholdResult ?? null;

    if (!analysis && !effectiveThreshold && !errorAnalysis && !environmentMetrics) {
        return <Alert severity="info">{t('analysis')} -</Alert>;
    }

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Paper variant="outlined" sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{t('analysis')}</Typography>
                    <PerformanceThresholdStatusChip status={analysis?.status} thresholdResult={effectiveThreshold} />
                </Box>
                {analysis?.summaryText && (
                    <Typography variant="body2" sx={{ mb: 2 }}>{analysis.summaryText}</Typography>
                )}
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 1.5 }}>
                    <Field label={t('problemStep')} value={analysis?.problemStepName} />
                    <Field label={t('slowestStep')} value={analysis?.slowestStepName} />
                    <Field label={t('highestP95Step')} value={analysis?.highestP95StepName} />
                    <Field label={t('highestP99Step')} value={analysis?.highestP99StepName} />
                    <Field label={t('highestErrorStep')} value={analysis?.highestErrorStepName} />
                    <Field label={t('highestStdDeviationStep')} value={analysis?.highestStdDeviationStepName} />
                </Box>
                {analysis?.warnings && analysis.warnings.length > 0 && (
                    <Alert severity="warning" sx={{ mt: 2 }}>{analysis.warnings.join(' ')}</Alert>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{t('failureReasons')}</Typography>
                <PerformanceFailureReasons reasons={effectiveThreshold?.reasons} />
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{t('errorAnalysis')}</Typography>
                <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
                    <Field label={t('failedSamples')} value={dash(errorAnalysis?.totalErrorCount)} />
                    <Field label={t('errorRate')} value={formatPercent(errorAnalysis?.errorRate)} />
                    <Field label={t('lastError')} value={errorAnalysis?.lastError} />
                </Box>
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{t('environmentMetrics')}</Typography>
                <PerformanceEnvironmentMetricsPanel metrics={environmentMetrics} />
            </Paper>
        </Box>
    );
}
