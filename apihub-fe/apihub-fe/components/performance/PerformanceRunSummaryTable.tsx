'use client';

import { Button, Checkbox, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceHistoryItem } from '@/types/performance';
import {
    dash,
    formatDateTime,
    formatDurationMs,
    formatPercent,
    formatThroughput,
} from './PerformanceMetricFormatters';
import PerformanceThresholdStatusChip from './PerformanceThresholdStatusChip';

interface PerformanceRunSummaryTableProps {
    items: PerformanceHistoryItem[];
    onViewDetail: (item: PerformanceHistoryItem) => void;
    selectedIds?: number[];
    onToggleCompareSelection?: (item: PerformanceHistoryItem) => void;
    onSetBaseline?: (item: PerformanceHistoryItem) => void;
    settingBaselineId?: number | null;
}

function FailurePreview({ reasons }: { reasons?: string[] }) {
    if (!reasons || reasons.length === 0) {
        return <>-</>;
    }
    return (
        <Tooltip title={reasons.join('\n')}>
            <Typography variant="caption" sx={{ display: 'block', maxWidth: 240, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {reasons[0]}
            </Typography>
        </Tooltip>
    );
}

export default function PerformanceRunSummaryTable({
    items,
    onViewDetail,
    selectedIds = [],
    onToggleCompareSelection,
    onSetBaseline,
    settingBaselineId = null,
}: PerformanceRunSummaryTableProps) {
    const t = useTranslations('performance');
    const selectable = Boolean(onToggleCompareSelection);

    return (
        <TableContainer sx={{ overflowX: 'auto' }}>
            <Table size="small">
                <TableHead>
                    <TableRow>
                        {selectable && <TableCell>{t('compare')}</TableCell>}
                        <TableCell>{t('date')}</TableCell>
                        <TableCell>{t('status')}</TableCell>
                        <TableCell>{t('baseline')}</TableCell>
                        <TableCell>{t('autoBaselineComparison')}</TableCell>
                        <TableCell>{t('threadCount')}</TableCell>
                        <TableCell>{t('rampUp')}</TableCell>
                        <TableCell>{t('durationSeconds')}</TableCell>
                        <TableCell>{t('loopCount')}</TableCell>
                        <TableCell>{t('totalSamples')}</TableCell>
                        <TableCell>{t('successRate')}</TableCell>
                        <TableCell>{t('errorRate')}</TableCell>
                        <TableCell>{t('throughput')}</TableCell>
                        <TableCell>{t('average')}</TableCell>
                        <TableCell>{t('p95')}</TableCell>
                        <TableCell>{t('p99')}</TableCell>
                        <TableCell>{t('slowestStep')}</TableCell>
                        <TableCell>{t('failureReasons')}</TableCell>
                        <TableCell>{t('setBaseline')}</TableCell>
                        <TableCell>{t('detail')}</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {items.map((item, index) => {
                        const runSummary = item.runSummary;
                        const status = item.performanceStatus ?? runSummary?.status;
                        const errorRate = runSummary?.errorRate;
                        const successRate = errorRate === undefined || errorRate === null ? null : 100 - errorRate;
                        const selected = item.performanceResultId !== undefined && selectedIds.includes(item.performanceResultId);
                        const isSettingBaseline = settingBaselineId !== null && settingBaselineId === item.performanceResultId;

                        return (
                            <TableRow key={`${item.performanceResultId ?? 'row'}-${index}`}>
                                {selectable && (
                                    <TableCell>
                                        <Checkbox
                                            size="small"
                                            checked={selected}
                                            disabled={!item.performanceResultId}
                                            onChange={() => onToggleCompareSelection?.(item)}
                                        />
                                    </TableCell>
                                )}
                                <TableCell>{formatDateTime(item.createdAt ?? runSummary?.startedAt)}</TableCell>
                                <TableCell>
                                    <PerformanceThresholdStatusChip status={status} thresholdResult={item.thresholdResult ?? null} />
                                </TableCell>
                                <TableCell>
                                    {item.baseline ? <Chip size="small" color="primary" label={t('currentBaseline')} /> : '-'}
                                </TableCell>
                                <TableCell>
                                    {item.baselineComparison ? <Chip size="small" color="info" label={t('autoBaselineComparison')} /> : '-'}
                                </TableCell>
                                <TableCell>{dash(runSummary?.threadCount ?? item.threadCount)}</TableCell>
                                <TableCell>{dash(runSummary?.rampUpPeriod ?? item.rampUpPeriod)}</TableCell>
                                <TableCell>{dash(item.durationSeconds)}</TableCell>
                                <TableCell>{dash(item.loopCount)}</TableCell>
                                <TableCell>{dash(runSummary?.totalSamples)}</TableCell>
                                <TableCell>{formatPercent(successRate)}</TableCell>
                                <TableCell>{formatPercent(errorRate)}</TableCell>
                                <TableCell>{formatThroughput(runSummary?.throughputPerSecond)}</TableCell>
                                <TableCell>{formatDurationMs(runSummary?.averageElapsedTime)}</TableCell>
                                <TableCell>{formatDurationMs(runSummary?.p95ElapsedTime)}</TableCell>
                                <TableCell>{formatDurationMs(runSummary?.p99ElapsedTime)}</TableCell>
                                <TableCell>{dash(runSummary?.slowestStepName ?? item.analysisSummary?.problemStepName)}</TableCell>
                                <TableCell><FailurePreview reasons={item.thresholdResult?.reasons} /></TableCell>
                                <TableCell>
                                    <Button
                                        size="small"
                                        variant="outlined"
                                        disabled={!item.performanceResultId || item.baseline === true || isSettingBaseline}
                                        onClick={() => onSetBaseline?.(item)}
                                    >
                                        {isSettingBaseline ? '...' : t('setBaseline')}
                                    </Button>
                                </TableCell>
                                <TableCell>
                                    <Button size="small" variant="outlined" onClick={() => onViewDetail(item)}>
                                        {t('detail')}
                                    </Button>
                                </TableCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
