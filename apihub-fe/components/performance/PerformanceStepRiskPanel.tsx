'use client';

import { Alert, Box, Button, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import {
    PerformanceInsightReport,
    PerformanceManagementReport,
    PerformanceManagementRiskLevel,
    PerformanceManagementStepAssessment,
    PerformanceManagementStepStatus,
    PerformanceStepInsight,
} from '@/types/performance';

interface Props {
    managementReport?: PerformanceManagementReport | null;
    insightReport?: PerformanceInsightReport | null;
}

function statusColor(status?: PerformanceManagementStepStatus | null): 'success' | 'warning' | 'error' | 'default' {
    if (status === 'GOOD') return 'success';
    if (status === 'WATCH') return 'warning';
    if (status === 'NEEDS_IMPROVEMENT') return 'error';
    return 'default';
}

function priorityColor(priority?: PerformanceManagementRiskLevel | null): 'success' | 'warning' | 'error' | 'default' {
    if (priority === 'LOW') return 'success';
    if (priority === 'MEDIUM') return 'warning';
    if (priority === 'HIGH' || priority === 'CRITICAL') return 'error';
    return 'default';
}

function formatNumber(value?: number | null, suffix = ''): string {
    return typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(2)}${suffix}` : '-';
}

function formatMs(value?: number | null): string {
    return typeof value === 'number' && Number.isFinite(value) ? `${Math.round(value)} ms` : '-';
}

function statusLabel(t: ReturnType<typeof useTranslations>, status?: PerformanceManagementStepStatus | null): string {
    if (status === 'NEEDS_IMPROVEMENT') return t('needsImprovement');
    if (status === 'WATCH') return t('watch');
    if (status === 'GOOD') return t('goodCondition');
    return '-';
}

function findInsight(step: PerformanceManagementStepAssessment, insights: PerformanceStepInsight[]) {
    return insights.find((insight) => insight.stepName && insight.stepName === step.stepName);
}

export default function PerformanceStepRiskPanel({ managementReport, insightReport }: Props) {
    const t = useTranslations('performance');
    const [showGoodSteps, setShowGoodSteps] = useState(false);
    const stepAssessments = managementReport?.stepAssessments ?? [];
    const stepInsights = insightReport?.stepInsights ?? [];

    const needsImprovement = stepAssessments.filter((step) => step.status === 'NEEDS_IMPROVEMENT');
    const watch = stepAssessments.filter((step) => step.status === 'WATCH');
    const good = stepAssessments.filter((step) => step.status === 'GOOD');

    const renderStep = (step: PerformanceManagementStepAssessment, index: number) => {
        const insight = findInsight(step, stepInsights);
        const key = `${step.status ?? 'UNKNOWN'}-${step.stepName ?? index}`;
        const metrics = [
            `${t('average')}: ${formatMs(step.averageMs)}`,
            `P95: ${formatMs(step.p95Ms)}`,
            `P99: ${formatMs(step.p99Ms)}`,
            `${t('errorRate')}: ${formatNumber(step.errorRate, '%')}`,
            `${t('throughput')}: ${formatNumber(step.throughputPerSecond, ' req/s')}`,
        ];

        return (
            <Paper key={key} variant="outlined" sx={{ p: 1.5 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 1, flexWrap: 'wrap' }}>
                    <Box sx={{ minWidth: 0 }}>
                        <Typography variant="body2" sx={{ fontWeight: 700, wordBreak: 'break-word' }}>
                            {step.stepName || '-'}
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 0.75 }}>
                            <Chip size="small" color={statusColor(step.status)} label={statusLabel(t, step.status)} />
                            {step.priority && <Chip size="small" color={priorityColor(step.priority)} label={`${t('priority')}: ${step.priority}`} />}
                        </Box>
                    </Box>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, mt: 1 }}>
                    <Typography variant="body2"><Box component="span" sx={{ fontWeight: 700 }}>{t('mainReason')}: </Box>{step.mainReason || '-'}</Typography>
                    <Typography variant="body2"><Box component="span" sx={{ fontWeight: 700 }}>{t('evidence')}: </Box>{step.evidence || '-'}</Typography>
                    <Typography variant="body2"><Box component="span" sx={{ fontWeight: 700 }}>{t('impact')}: </Box>{step.impact || '-'}</Typography>
                    <Typography variant="body2"><Box component="span" sx={{ fontWeight: 700 }}>{t('recommendation')}: </Box>{step.recommendation || '-'}</Typography>
                </Box>
                <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 1 }}>
                    {metrics.map((metric) => <Chip key={metric} size="small" variant="outlined" label={metric} />)}
                </Box>
                {insight?.explanation && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                        {insight.explanation}
                    </Typography>
                )}
            </Paper>
        );
    };

    const renderGroup = (title: string, steps: PerformanceManagementStepAssessment[]) => {
        if (steps.length === 0) return null;
        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                    {title} ({steps.length})
                </Typography>
                {steps.map(renderStep)}
            </Box>
        );
    };

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1, flexWrap: 'wrap', mb: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                    {t('stepAssessments')}
                </Typography>
                {good.length > 0 && (
                    <Button size="small" variant="outlined" onClick={() => setShowGoodSteps((value) => !value)}>
                        {showGoodSteps ? t('hideGoodSteps') : t('showGoodSteps')}
                    </Button>
                )}
            </Box>
            {stepAssessments.length === 0 ? (
                <Alert severity="info">{t('noStepAssessments')}</Alert>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {renderGroup(t('needsImprovement'), needsImprovement)}
                    {renderGroup(t('watch'), watch)}
                    {showGoodSteps && renderGroup(t('goodCondition'), good)}
                </Box>
            )}
        </Paper>
    );
}
