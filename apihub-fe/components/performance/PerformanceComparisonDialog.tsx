'use client';

import {
    Chip,
    Dialog,
    DialogContent,
    DialogTitle,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
} from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceComparisonMetric, PerformanceComparisonResult } from '@/types/performance';
import { comparisonColor, dash, formatDelta } from './PerformanceMetricFormatters';

interface PerformanceComparisonDialogProps {
    open: boolean;
    loading?: boolean;
    result?: PerformanceComparisonResult | null;
    onClose: () => void;
}

function formatValue(value?: PerformanceComparisonMetric['baseValue']) {
    if (typeof value === 'number') {
        return value.toFixed(2);
    }
    return dash(value === null || value === undefined ? null : String(value));
}

export default function PerformanceComparisonDialog({ open, loading = false, result, onClose }: PerformanceComparisonDialogProps) {
    const t = useTranslations('performance');

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{t('compare')}</DialogTitle>
            <DialogContent>
                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>Metric</TableCell>
                                <TableCell>Base</TableCell>
                                <TableCell>Target</TableCell>
                                <TableCell>Delta</TableCell>
                                <TableCell>Direction</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {loading ? (
                                <TableRow><TableCell colSpan={5}>Loading...</TableCell></TableRow>
                            ) : result?.metrics?.length ? (
                                result.metrics.map((metric) => (
                                    <TableRow key={metric.metricName}>
                                        <TableCell>{metric.metricName}</TableCell>
                                        <TableCell>{formatValue(metric.baseValue)}</TableCell>
                                        <TableCell>{formatValue(metric.targetValue)}</TableCell>
                                        <TableCell>{typeof metric.delta === 'number' ? formatDelta(metric.delta) : dash(metric.delta === null || metric.delta === undefined ? null : String(metric.delta))}</TableCell>
                                        <TableCell>
                                            <Chip label={dash(metric.direction)} color={comparisonColor(metric.improvement)} size="small" />
                                        </TableCell>
                                    </TableRow>
                                ))
                            ) : (
                                <TableRow><TableCell colSpan={5}>-</TableCell></TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </DialogContent>
        </Dialog>
    );
}
