'use client';

import {
    Alert,
    Box,
    Button,
    Chip,
    Divider,
    List,
    ListItem,
    ListItemText,
    Paper,
    Typography,
} from '@mui/material';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import {
    PerformanceManagementReport,
    PerformanceManagementRiskLevel,
    PerformanceManagementStepAssessment,
    PerformanceManagementStepStatus,
} from '@/types/performance';

interface PerformanceManagementReportPanelProps {
    report?: PerformanceManagementReport | null;
}

function riskColor(risk?: PerformanceManagementRiskLevel | null): 'success' | 'warning' | 'error' | 'default' {
    if (risk === 'LOW') return 'success';
    if (risk === 'MEDIUM') return 'warning';
    if (risk === 'HIGH' || risk === 'CRITICAL') return 'error';
    return 'default';
}

function stepStatusColor(status?: PerformanceManagementStepStatus | null): 'success' | 'warning' | 'error' | 'default' {
    if (status === 'GOOD') return 'success';
    if (status === 'WATCH') return 'warning';
    if (status === 'NEEDS_IMPROVEMENT') return 'error';
    return 'default';
}

function formatNumber(value?: number | null, suffix = ''): string {
    return typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(2)}${suffix}` : '-';
}

function formatPercent(value?: number | null): string {
    return typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(2)}%` : '-';
}

function formatMs(value?: number | null): string {
    return typeof value === 'number' && Number.isFinite(value) ? `${Math.round(value)} ms` : '-';
}

export default function PerformanceManagementReportPanel({ report }: PerformanceManagementReportPanelProps) {
    const t = useTranslations('performance');
    const [openStepDetails, setOpenStepDetails] = useState<Record<string, boolean>>({});
    const [reportDetailsOpen, setReportDetailsOpen] = useState(false);

    const riskLabels: Record<PerformanceManagementRiskLevel, string> = {
        LOW: t('riskLow'),
        MEDIUM: t('riskMedium'),
        HIGH: t('riskHigh'),
        CRITICAL: t('riskCritical'),
    };

    const stepStatusLabels: Record<PerformanceManagementStepStatus, string> = {
        NEEDS_IMPROVEMENT: t('needsImprovement'),
        WATCH: t('watch'),
        GOOD: t('goodCondition'),
    };

    const riskLabel = (risk?: PerformanceManagementRiskLevel | null) => (risk ? riskLabels[risk] : '-');
    const stepStatusLabel = (status?: PerformanceManagementStepStatus | null) => (status ? stepStatusLabels[status] : '-');

    if (!report) {
        return <Alert severity="info">{t('reportUnavailable')}</Alert>;
    }

    const stepAssessments = report.stepAssessments ?? [];
    const needsImprovement = stepAssessments.filter((step) => step.status === 'NEEDS_IMPROVEMENT');
    const watch = stepAssessments.filter((step) => step.status === 'WATCH');
    const good = stepAssessments.filter((step) => step.status === 'GOOD');
    const priorityStep = needsImprovement[0] ?? watch[0] ?? null;
    const slaSummary = report.slaSummary ?? [];
    const problemAreas = report.problemAreas ?? [];
    const recommendedActions = report.recommendedActions ?? [];

    const toggleStepDetails = (stepKey: string) => {
        setOpenStepDetails((current) => ({
            ...current,
            [stepKey]: !current[stepKey],
        }));
    };

    const renderStepGroup = (
        title: string,
        steps: PerformanceManagementStepAssessment[],
    ) => {
        if (steps.length === 0) {
            return null;
        }

        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                    {title} ({steps.length})
                </Typography>
                {steps.map((step, index) => {
                    const stepKey = `${step.status ?? 'UNKNOWN'}-${step.stepName ?? index}`;
                    const detailsOpen = Boolean(openStepDetails[stepKey]);

                    return (
                        <Paper key={stepKey} variant="outlined" sx={{ p: 1.5 }}>
                            <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1.5, flexWrap: 'wrap' }}>
                                <Box sx={{ minWidth: 0 }}>
                                    <Typography variant="body2" sx={{ fontWeight: 700, wordBreak: 'break-word' }}>
                                        {step.stepName || '-'}
                                    </Typography>
                                    <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 0.75 }}>
                                        <Chip
                                            size="small"
                                            color={stepStatusColor(step.status)}
                                            label={stepStatusLabel(step.status)}
                                        />
                                        {step.priority && (
                                            <Chip
                                                size="small"
                                                color={riskColor(step.priority)}
                                                label={`${t('priority')}: ${riskLabel(step.priority)}`}
                                            />
                                        )}
                                    </Box>
                                </Box>
                                <Button variant="outlined" size="small" onClick={() => toggleStepDetails(stepKey)}>
                                    {detailsOpen ? t('hideStepDetails') : t('showStepDetails')}
                                </Button>
                            </Box>

                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, mt: 1.25 }}>
                                <Typography variant="body2">
                                    <Box component="span" sx={{ fontWeight: 700 }}>{t('mainReason')}: </Box>
                                    {step.mainReason || '-'}
                                </Typography>
                                <Typography variant="body2">
                                    <Box component="span" sx={{ fontWeight: 700 }}>{t('evidence')}: </Box>
                                    {step.evidence || '-'}
                                </Typography>
                                <Typography variant="body2">
                                    <Box component="span" sx={{ fontWeight: 700 }}>{t('impact')}: </Box>
                                    {step.impact || '-'}
                                </Typography>
                                <Typography variant="body2">
                                    <Box component="span" sx={{ fontWeight: 700 }}>{t('recommendation')}: </Box>
                                    {step.recommendation || '-'}
                                </Typography>
                            </Box>

                            {detailsOpen && (
                                <Box sx={{ mt: 1.5, pt: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {t('stepDetails')}
                                    </Typography>
                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
                                            gap: 1,
                                        }}
                                    >
                                        <Typography variant="caption">{t('totalRequests')}: {step.sampleCount ?? '-'}</Typography>
                                        <Typography variant="caption">{t('successfulRequests')}: {step.successCount ?? '-'}</Typography>
                                        <Typography variant="caption">{t('failedRequests')}: {step.failureCount ?? '-'}</Typography>
                                        <Typography variant="caption">{t('errorRate')}: {formatPercent(step.errorRate)}</Typography>
                                        <Typography variant="caption">{t('averageResponseTime')}: {formatMs(step.averageMs)}</Typography>
                                        <Typography variant="caption">P90: {formatMs(step.p90Ms)}</Typography>
                                        <Typography variant="caption">P95: {formatMs(step.p95Ms)}</Typography>
                                        <Typography variant="caption">P99: {formatMs(step.p99Ms)}</Typography>
                                        <Typography variant="caption">{t('throughput')}: {formatNumber(step.throughputPerSecond, ' req/s')}</Typography>
                                    </Box>
                                    {step.lastError && step.lastError.trim() && (
                                        <Typography variant="caption" sx={{ display: 'block', mt: 1, color: 'error.main', wordBreak: 'break-word' }}>
                                            {t('lastError')}: {step.lastError}
                                        </Typography>
                                    )}
                                </Box>
                            )}
                        </Paper>
                    );
                })}
            </Box>
        );
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Paper variant="outlined" sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2, flexWrap: 'wrap' }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                        {t('managementReport')}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                        {report.overallStatus && <Chip size="small" label={report.overallStatus} />}
                        {report.riskLevel && (
                            <Chip size="small" color={riskColor(report.riskLevel)} label={`${t('riskLevel')}: ${riskLabel(report.riskLevel)}`} />
                        )}
                    </Box>
                </Box>
                <Typography variant="body2">
                    {report.stepAssessmentSummary || report.executiveSummary || t('reportUnavailable')}
                </Typography>
                {priorityStep && (
                    <Typography variant="body2" sx={{ mt: 1, fontWeight: 700 }}>
                        {t('mostCriticalStep')}: {priorityStep.stepName || '-'}
                    </Typography>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                    {t('stepAssessments')}
                </Typography>
                {stepAssessments.length === 0 ? (
                    <Alert severity="info">{t('noStepAssessments')}</Alert>
                ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                        {renderStepGroup(t('needsImprovement'), needsImprovement)}
                        {renderStepGroup(t('watch'), watch)}
                        {renderStepGroup(t('goodCondition'), good)}
                    </Box>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                    {t('slaSummary')}
                </Typography>
                {slaSummary.length === 0 ? (
                    <Alert severity="info">{t('reportUnavailable')}</Alert>
                ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        {slaSummary.map((item) => (
                            <Box key={item.metric} sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 700 }}>
                                        {item.metric}
                                    </Typography>
                                    <Chip
                                        size="small"
                                        color={item.passed ? 'success' : 'error'}
                                        label={item.passed ? t('passed') : t('failed')}
                                    />
                                </Box>
                                <Typography variant="caption" color="text.secondary">
                                    {t('expected')}: {item.expected || '-'} | {t('actual')}: {item.actual || '-'}
                                </Typography>
                                {item.explanation && (
                                    <Typography variant="body2">
                                        {item.explanation}
                                    </Typography>
                                )}
                            </Box>
                        ))}
                    </Box>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                    {t('problemAreas')}
                </Typography>
                {problemAreas.length === 0 ? (
                    <Alert severity="info">{t('reportUnavailable')}</Alert>
                ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        {problemAreas.map((area, index) => (
                            <Box key={`${area.title}-${area.stepName ?? index}`}>
                                {index > 0 && <Divider sx={{ mb: 1.5 }} />}
                                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                                    {area.title}
                                </Typography>
                                <Typography variant="body2">
                                    {area.stepName || '-'}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {area.metric || '-'}: {area.value || '-'}
                                </Typography>
                                <Typography variant="body2" sx={{ mt: 0.5 }}>
                                    {t('impact')}: {area.impact || '-'}
                                </Typography>
                            </Box>
                        ))}
                    </Box>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                    {t('trendSummary')}
                </Typography>
                <Typography variant="body2">
                    {report.trendSummary || t('reportUnavailable')}
                </Typography>
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                    {t('recommendedActions')}
                </Typography>
                {recommendedActions.length === 0 ? (
                    <Alert severity="info">{t('reportUnavailable')}</Alert>
                ) : (
                    <List dense disablePadding>
                        {recommendedActions.map((action) => (
                            <ListItem key={action} disableGutters>
                                <ListItemText primary={action} />
                            </ListItem>
                        ))}
                    </List>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Button variant="outlined" size="small" onClick={() => setReportDetailsOpen((value) => !value)}>
                    {reportDetailsOpen ? t('hideReportDetails') : t('showReportDetails')}
                </Button>
                {reportDetailsOpen && (
                    <Box sx={{ mt: 2 }}>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                            {t('detailExplanation')}
                        </Typography>
                        <Typography variant="body2">
                            {report.detailExplanation || t('reportUnavailable')}
                        </Typography>
                    </Box>
                )}
            </Paper>
        </Box>
    );
}
