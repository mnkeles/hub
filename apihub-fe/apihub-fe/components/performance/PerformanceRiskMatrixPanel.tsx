'use client';

import { Box, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import {
    PerformanceComparisonResult,
    PerformanceInsightReport,
    PerformanceInsightSeverity,
    PerformanceManagementReport,
    PerformanceManagementStepStatus,
} from '@/types/performance';

interface Props {
    managementReport?: PerformanceManagementReport | null;
    insightReport?: PerformanceInsightReport | null;
    baselineComparison?: PerformanceComparisonResult | null;
}

interface RiskRow {
    sourceType: 'Step' | 'Regression' | 'Root Cause' | 'Metric';
    name: string;
    level: 'Critical / High' | 'Watch' | 'Good / Info';
    explanation: string;
    score: number;
}

function stepScore(status?: PerformanceManagementStepStatus | null): number {
    if (status === 'NEEDS_IMPROVEMENT') return 100;
    if (status === 'WATCH') return 60;
    return 10;
}

function severityScore(severity?: PerformanceInsightSeverity | null): number {
    if (severity === 'CRITICAL') return 100;
    if (severity === 'HIGH') return 90;
    if (severity === 'WARNING') return 60;
    return 20;
}

function levelFromScore(score: number): RiskRow['level'] {
    if (score >= 85) return 'Critical / High';
    if (score >= 50) return 'Watch';
    return 'Good / Info';
}

function levelColor(level: RiskRow['level']): 'error' | 'warning' | 'default' {
    if (level === 'Critical / High') return 'error';
    if (level === 'Watch') return 'warning';
    return 'default';
}

function buildRiskRows(
    managementReport?: PerformanceManagementReport | null,
    insightReport?: PerformanceInsightReport | null,
    baselineComparison?: PerformanceComparisonResult | null
): RiskRow[] {
    const rows: RiskRow[] = [];

    (managementReport?.stepAssessments ?? []).forEach((assessment) => {
        const score = stepScore(assessment.status);
        rows.push({
            sourceType: 'Step',
            name: assessment.stepName || '-',
            level: levelFromScore(score),
            explanation: assessment.mainReason || assessment.evidence || '-',
            score,
        });
    });

    (insightReport?.metricInsights ?? []).forEach((insight) => {
        const score = severityScore(insight.severity);
        rows.push({
            sourceType: 'Metric',
            name: insight.metric || '-',
            level: levelFromScore(score),
            explanation: insight.explanation || `${insight.actual || '-'} / ${insight.expected || '-'}`,
            score,
        });
    });

    (insightReport?.rootCauseHints ?? []).forEach((hint) => {
        const score = severityScore(hint.severity);
        rows.push({
            sourceType: 'Root Cause',
            name: hint.signal || hint.category || '-',
            level: levelFromScore(score),
            explanation: hint.explanation || hint.recommendation || '-',
            score,
        });
    });

    (baselineComparison?.metrics ?? [])
        .filter((metric) => metric.improvement === false)
        .forEach((metric) => {
            rows.push({
                sourceType: 'Regression',
                name: metric.metricName,
                level: levelFromScore(85),
                explanation: `${metric.baseValue ?? '-'} -> ${metric.targetValue ?? '-'}`,
                score: 85,
            });
        });

    return rows.sort((left, right) => right.score - left.score).slice(0, 5);
}

export default function PerformanceRiskMatrixPanel({ managementReport, insightReport, baselineComparison }: Props) {
    const t = useTranslations('performance');
    const rows = buildRiskRows(managementReport, insightReport, baselineComparison);

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                {t('riskMatrix')}
            </Typography>
            {rows.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                    {t('noRiskSignals')}
                </Typography>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {rows.map((row, index) => (
                        <Box
                            key={`${row.sourceType}-${row.name}-${index}`}
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: { xs: '1fr', md: '120px minmax(120px, 1fr) 140px minmax(180px, 2fr)' },
                                gap: 1,
                                alignItems: 'center',
                                py: 1,
                                borderBottom: index === rows.length - 1 ? 0 : '1px solid',
                                borderColor: 'divider',
                            }}
                        >
                            <Box>
                                <Typography variant="caption" color="text.secondary">{t('sourceType')}</Typography>
                                <Typography variant="body2">{row.sourceType}</Typography>
                            </Box>
                            <Box sx={{ minWidth: 0 }}>
                                <Typography variant="caption" color="text.secondary">{t('riskSignal')}</Typography>
                                <Typography variant="body2" sx={{ fontWeight: 700, wordBreak: 'break-word' }}>{row.name}</Typography>
                            </Box>
                            <Chip size="small" color={levelColor(row.level)} label={row.level} sx={{ justifySelf: { xs: 'flex-start', md: 'stretch' } }} />
                            <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-word' }}>
                                {row.explanation}
                            </Typography>
                        </Box>
                    ))}
                </Box>
            )}
        </Paper>
    );
}
