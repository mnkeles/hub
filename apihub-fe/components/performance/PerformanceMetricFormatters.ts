import { PerformanceStatus } from '@/types/performance';

export function dash(value?: string | number | null): string {
    if (value === null || value === undefined || value === '') {
        return '-';
    }
    return String(value);
}

export function formatNumber(value?: number | null, digits = 2): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return '-';
    }
    return value.toFixed(digits);
}

export function formatDurationMs(value?: number | null): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return '-';
    }
    return `${value.toFixed(0)} ms`;
}

export function formatPercent(value?: number | null): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return '-';
    }
    return `${value.toFixed(2)}%`;
}

export function formatThroughput(value?: number | null): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return '-';
    }
    return `${value.toFixed(2)} req/s`;
}

export function formatDateTime(value?: string | null): string {
    if (!value) {
        return '-';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime()) || date.getFullYear() === 1970) {
        return '-';
    }
    return date.toLocaleString('tr-TR');
}

export function formatStatusLabel(status?: PerformanceStatus | string | null): string {
    if (!status) {
        return '-';
    }
    switch (status) {
        case 'COMPLETED_PASSED':
            return 'COMPLETED - PASSED';
        case 'COMPLETED_FAILED':
            return 'COMPLETED - FAILED';
        default:
            return String(status);
    }
}

export function formatDelta(value?: number | null, unit = ''): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return '-';
    }
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(2)}${unit}`;
}

export function comparisonColor(improvement?: boolean | null): 'success' | 'error' | 'default' {
    if (improvement === true) {
        return 'success';
    }
    if (improvement === false) {
        return 'error';
    }
    return 'default';
}
