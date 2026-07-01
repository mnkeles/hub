'use client';

import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceAnalysisSummary, PerformanceStepSummary, PerformanceThresholdResult } from '@/types/performance';
import { dash, formatDurationMs, formatPercent, formatThroughput } from './PerformanceMetricFormatters';
import PerformanceDurationBuckets from './PerformanceDurationBuckets';

interface PerformanceStepSummaryTableProps {
    summaries: PerformanceStepSummary[];
    thresholdResult?: PerformanceThresholdResult | null;
    analysis?: PerformanceAnalysisSummary | null;
}

function isProblemRow(summary: PerformanceStepSummary, analysis?: PerformanceAnalysisSummary | null): boolean {
    return summary.stepName === analysis?.problemStepName || summary.stepName === analysis?.slowestStepName;
}

function metricCellSx(isBad: boolean) {
    return isBad ? { color: 'error.main', fontWeight: 700 } : undefined;
}

export default function PerformanceStepSummaryTable({ summaries, thresholdResult, analysis }: PerformanceStepSummaryTableProps) {
    const t = useTranslations('performance');
    const thresholds = thresholdResult?.thresholds;

    return (
        <TableContainer sx={{ overflowX: 'auto' }}>
            <Table size="small">
                <TableHead>
                    <TableRow>
                        <TableCell rowSpan={2}>{t('step')}</TableCell>
                        <TableCell colSpan={4} align="center">Test Result</TableCell>
                        <TableCell colSpan={4} align="center">Performance</TableCell>
                        <TableCell colSpan={3} align="center">Percentile</TableCell>
                        <TableCell colSpan={3} align="center">{t('analysis')}</TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>{t('sampleCount')}</TableCell>
                        <TableCell>{t('successfulSamples')}</TableCell>
                        <TableCell>{t('failedSamples')}</TableCell>
                        <TableCell>{t('errorRate')}</TableCell>
                        <TableCell>{t('throughput')}</TableCell>
                        <TableCell>{t('average')}</TableCell>
                        <TableCell>{t('minimum')}</TableCell>
                        <TableCell>{t('maximum')}</TableCell>
                        <TableCell>{t('p90')}</TableCell>
                        <TableCell>{t('p95')}</TableCell>
                        <TableCell>{t('p99')}</TableCell>
                        <TableCell>{t('standardDeviation')}</TableCell>
                        <TableCell>{t('buckets')}</TableCell>
                        <TableCell>{t('lastError')}</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {summaries.map((summary, index) => {
                        const p95Bad = thresholds?.maxP95Ms !== undefined && (summary.p95ElapsedTime ?? 0) > thresholds.maxP95Ms;
                        const p99Bad = thresholds?.maxP99Ms !== undefined && (summary.p99ElapsedTime ?? 0) > thresholds.maxP99Ms;
                        const errorBad = (summary.errorRate ?? 0) > 0;

                        return (
                            <TableRow
                                key={`${summary.stepName}-${index}`}
                                sx={isProblemRow(summary, analysis) ? { bgcolor: 'warning.light' } : undefined}
                            >
                                <TableCell>{dash(summary.stepName)}</TableCell>
                                <TableCell>{dash(summary.sampleCount)}</TableCell>
                                <TableCell>{dash(summary.successCount)}</TableCell>
                                <TableCell>{dash(summary.failureCount)}</TableCell>
                                <TableCell sx={metricCellSx(errorBad)}>{formatPercent(summary.errorRate)}</TableCell>
                                <TableCell>{formatThroughput(summary.throughputPerSecond)}</TableCell>
                                <TableCell>{formatDurationMs(summary.averageElapsedTime)}</TableCell>
                                <TableCell>{formatDurationMs(summary.minElapsedTime)}</TableCell>
                                <TableCell>{formatDurationMs(summary.maxElapsedTime)}</TableCell>
                                <TableCell>{formatDurationMs(summary.p90ElapsedTime)}</TableCell>
                                <TableCell sx={metricCellSx(p95Bad)}>{formatDurationMs(summary.p95ElapsedTime)}</TableCell>
                                <TableCell sx={metricCellSx(p99Bad)}>{formatDurationMs(summary.p99ElapsedTime)}</TableCell>
                                <TableCell>{formatDurationMs(summary.standardDeviation)}</TableCell>
                                <TableCell><PerformanceDurationBuckets buckets={summary.responseTimeBuckets} /></TableCell>
                                <TableCell>
                                    {summary.lastError ? (
                                        <Tooltip title={summary.lastError}>
                                            <Typography
                                                variant="caption"
                                                sx={{ display: 'block', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                                            >
                                                {summary.lastError}
                                            </Typography>
                                        </Tooltip>
                                    ) : '-'}
                                </TableCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
