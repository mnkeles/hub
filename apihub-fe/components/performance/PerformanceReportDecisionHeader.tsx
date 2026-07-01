'use client';

import { Alert, Box, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import {
    PerformanceAiManagementReport,
    PerformanceManagementReport,
    PerformanceManagementRiskLevel,
    PerformanceReleaseReadiness,
    PerformanceInsightReport,
} from '@/types/performance';

interface Props {
    managementReport?: PerformanceManagementReport | null;
    insightReport?: PerformanceInsightReport | null;
    aiReport?: PerformanceAiManagementReport | null;
}

function readinessColor(value?: PerformanceReleaseReadiness | null): 'success' | 'warning' | 'error' | 'default' {
    if (value === 'READY') return 'success';
    if (value === 'CONDITIONAL') return 'warning';
    if (value === 'BLOCKED') return 'error';
    return 'default';
}

function riskColor(value?: PerformanceManagementRiskLevel | null): 'success' | 'warning' | 'error' | 'default' {
    if (value === 'LOW') return 'success';
    if (value === 'MEDIUM') return 'warning';
    if (value === 'HIGH' || value === 'CRITICAL') return 'error';
    return 'default';
}

function formatNumber(value?: number | null, digits = 2): string {
    return typeof value === 'number' && Number.isFinite(value) ? value.toFixed(digits) : '-';
}

function readinessLabel(t: ReturnType<typeof useTranslations>, value?: PerformanceReleaseReadiness | null): string {
    if (value === 'READY') return t('ready');
    if (value === 'CONDITIONAL') return t('conditional');
    if (value === 'BLOCKED') return t('blocked');
    return t('unknown');
}

export default function PerformanceReportDecisionHeader({ managementReport, insightReport, aiReport }: Props) {
    const t = useTranslations('performance');

    if (!managementReport && !insightReport && !aiReport) {
        return <Alert severity="info">{t('reportUnavailable')}</Alert>;
    }

    const metrics = [
        { label: t('apdexScore'), value: formatNumber(insightReport?.apdexScore, 3) },
        { label: t('sloCompliance'), value: `${formatNumber(insightReport?.sloCompliancePercent)}%` },
        { label: t('regressionScore'), value: insightReport?.regressionAvailable ? formatNumber(insightReport?.regressionScore) : t('regressionUnavailable') },
        { label: t('anomalyScore'), value: formatNumber(insightReport?.anomalyScore) },
    ];

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2, flexWrap: 'wrap', mb: 2 }}>
                <Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                        {t('decisionSummary')}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        {managementReport?.stepAssessmentSummary || managementReport?.executiveSummary || t('reportUnavailable')}
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    {managementReport?.overallStatus && <Chip size="small" label={managementReport.overallStatus} />}
                    {managementReport?.riskLevel && (
                        <Chip size="small" color={riskColor(managementReport.riskLevel)} label={`${t('riskLevel')}: ${managementReport.riskLevel}`} />
                    )}
                    <Chip
                        size="small"
                        color={readinessColor(insightReport?.releaseReadiness)}
                        label={`${t('releaseReadiness')}: ${readinessLabel(t, insightReport?.releaseReadiness)}`}
                    />
                    <Chip
                        size="small"
                        color={aiReport?.generated ? 'success' : 'default'}
                        label={aiReport?.generated ? t('aiGenerated') : t('aiFallback')}
                    />
                </Box>
            </Box>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, minmax(0, 1fr))' }, gap: 1 }}>
                {metrics.map((metric) => (
                    <Box key={metric.label} sx={{ minWidth: 0 }}>
                        <Typography variant="caption" color="text.secondary">{metric.label}</Typography>
                        <Typography variant="body2" sx={{ fontWeight: 700, wordBreak: 'break-word' }}>{metric.value}</Typography>
                    </Box>
                ))}
            </Box>
        </Paper>
    );
}
