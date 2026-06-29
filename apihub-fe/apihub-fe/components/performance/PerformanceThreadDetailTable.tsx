'use client';

import { Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceDetailResponse } from '@/types/performance';
import { dash, formatDateTime, formatDurationMs } from './PerformanceMetricFormatters';
import { applyPerformanceThreadFilters, PerformanceThreadDetailRow, PerformanceThreadFilterState } from './PerformanceThreadFilters';

interface PerformanceThreadDetailTableProps {
    detail: PerformanceDetailResponse | null;
    filters?: PerformanceThreadFilterState;
}

function statusColor(status?: string | null): 'success' | 'warning' | 'error' | 'info' | 'default' {
    switch (status) {
        case 'COMPLETED':
            return 'success';
        case 'RUNNING':
            return 'warning';
        case 'FAILED':
            return 'error';
        case 'STOPPED':
            return 'info';
        default:
            return 'default';
    }
}

function flattenRows(detail: PerformanceDetailResponse | null): PerformanceThreadDetailRow[] {
    return detail?.groups.flatMap((group) =>
        group.steps.map((step, index) => ({
            threadNumber: step.threadNumber ?? group.threadNumber,
            stepName: step.stepName || `Step ${index + 1}`,
            status: step.performanceItemStatus,
            elapsedTime: step.elapsedTime,
            errorMessage: step.errorMessage,
            startedAt: step.startedAt,
            finishedAt: step.finishedAt,
        }))
    ) ?? [];
}

export default function PerformanceThreadDetailTable({ detail, filters }: PerformanceThreadDetailTableProps) {
    const t = useTranslations('performance');
    const rows = filters ? applyPerformanceThreadFilters(flattenRows(detail), filters) : flattenRows(detail);

    return (
        <TableContainer sx={{ overflowX: 'auto' }}>
            <Table size="small">
                <TableHead>
                    <TableRow>
                        <TableCell>{t('threadDetail')}</TableCell>
                        <TableCell>{t('step')}</TableCell>
                        <TableCell>{t('status')}</TableCell>
                        <TableCell>{t('duration')}</TableCell>
                        <TableCell>Started</TableCell>
                        <TableCell>Finished</TableCell>
                        <TableCell>{t('error')}</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {rows.map((row, index) => (
                        <TableRow key={`${row.threadNumber}-${row.stepName}-${index}`}>
                            <TableCell>{dash(row.threadNumber)}</TableCell>
                            <TableCell>{dash(row.stepName)}</TableCell>
                            <TableCell>
                                <Chip label={dash(row.status)} color={statusColor(row.status)} size="small" />
                            </TableCell>
                            <TableCell>{formatDurationMs(row.elapsedTime)}</TableCell>
                            <TableCell>{formatDateTime(row.startedAt)}</TableCell>
                            <TableCell>{formatDateTime(row.finishedAt)}</TableCell>
                            <TableCell>
                                {row.errorMessage ? (
                                    <Tooltip title={row.errorMessage}>
                                        <Typography
                                            variant="caption"
                                            sx={{ display: 'block', maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                                        >
                                            {row.errorMessage}
                                        </Typography>
                                    </Tooltip>
                                ) : '-'}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
