'use client';

import { Chip } from '@mui/material';
import { PerformanceStatus, PerformanceThresholdResult } from '@/types/performance';
import { formatStatusLabel } from './PerformanceMetricFormatters';

interface PerformanceThresholdStatusChipProps {
    status?: PerformanceStatus | string | null;
    thresholdResult?: PerformanceThresholdResult | null;
}

function statusColor(status?: string | null): 'success' | 'info' | 'error' | 'warning' | 'default' {
    switch (status) {
        case 'COMPLETED_PASSED':
            return 'success';
        case 'COMPLETED_FAILED':
        case 'FAILED':
        case 'ERROR':
            return 'error';
        case 'RUNNING':
        case 'STOPPING':
            return 'info';
        case 'STOPPED':
            return 'warning';
        default:
            return 'default';
    }
}

export default function PerformanceThresholdStatusChip({ status, thresholdResult }: PerformanceThresholdStatusChipProps) {
    const derivedStatus = status ?? (thresholdResult?.passed === true ? 'COMPLETED_PASSED' : thresholdResult?.passed === false ? 'COMPLETED_FAILED' : null);

    return (
        <Chip
            label={thresholdResult?.statusLabel ?? formatStatusLabel(derivedStatus)}
            color={statusColor(derivedStatus)}
            size="small"
        />
    );
}
