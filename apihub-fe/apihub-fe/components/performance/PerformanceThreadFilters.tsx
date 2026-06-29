'use client';

import {
    Box,
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Select,
    TextField,
} from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceStatus } from '@/types/performance';

export interface PerformanceThreadFilterState {
    stepQuery: string;
    threadNumber: string;
    status: '' | PerformanceStatus;
    onlyFailed: boolean;
    minDurationMs: string;
}

export interface PerformanceThreadDetailRow {
    threadNumber?: number;
    stepName?: string | null;
    status?: PerformanceStatus | string | null;
    elapsedTime?: number | null;
    errorMessage?: string | null;
    startedAt?: string | null;
    finishedAt?: string | null;
}

interface PerformanceThreadFiltersProps {
    filters: PerformanceThreadFilterState;
    onChange: (filters: PerformanceThreadFilterState) => void;
}

export const defaultPerformanceThreadFilters: PerformanceThreadFilterState = {
    stepQuery: '',
    threadNumber: '',
    status: '',
    onlyFailed: false,
    minDurationMs: '',
};

export function applyPerformanceThreadFilters(rows: PerformanceThreadDetailRow[], filters: PerformanceThreadFilterState): PerformanceThreadDetailRow[] {
    const stepQuery = filters.stepQuery.trim().toLowerCase();
    const parsedThreadNumber = Number(filters.threadNumber);
    const hasThreadFilter = filters.threadNumber.trim() !== '' && !Number.isNaN(parsedThreadNumber);
    const parsedMinDuration = Number(filters.minDurationMs);
    const hasDurationFilter = filters.minDurationMs.trim() !== '' && !Number.isNaN(parsedMinDuration);

    return rows.filter((row) => {
        if (stepQuery && !(row.stepName || '').toLowerCase().includes(stepQuery)) {
            return false;
        }
        if (hasThreadFilter && row.threadNumber !== parsedThreadNumber) {
            return false;
        }
        if (filters.status && row.status !== filters.status) {
            return false;
        }
        if (filters.onlyFailed && row.status !== 'FAILED' && !row.errorMessage) {
            return false;
        }
        if (hasDurationFilter && (row.elapsedTime ?? 0) <= parsedMinDuration) {
            return false;
        }
        return true;
    });
}

export default function PerformanceThreadFilters({ filters, onChange }: PerformanceThreadFiltersProps) {
    const t = useTranslations('performance');

    const update = (partial: Partial<PerformanceThreadFilterState>) => {
        onChange({ ...filters, ...partial });
    };

    return (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr 1.5fr' }, gap: 1.5, alignItems: 'center' }}>
            <TextField
                size="small"
                label={t('step')}
                value={filters.stepQuery}
                onChange={(event) => update({ stepQuery: event.target.value })}
            />
            <TextField
                size="small"
                label={t('threadDetail')}
                type="number"
                value={filters.threadNumber}
                onChange={(event) => update({ threadNumber: event.target.value })}
            />
            <FormControl size="small">
                <InputLabel>{t('status')}</InputLabel>
                <Select
                    value={filters.status}
                    label={t('status')}
                    onChange={(event) => update({ status: event.target.value as '' | PerformanceStatus })}
                >
                    <MenuItem value="">-</MenuItem>
                    <MenuItem value="RUNNING">RUNNING</MenuItem>
                    <MenuItem value="COMPLETED">COMPLETED</MenuItem>
                    <MenuItem value="FAILED">FAILED</MenuItem>
                    <MenuItem value="STOPPED">STOPPED</MenuItem>
                </Select>
            </FormControl>
            <TextField
                size="small"
                label={t('durationGreaterThan')}
                type="number"
                value={filters.minDurationMs}
                onChange={(event) => update({ minDurationMs: event.target.value })}
            />
            <FormControlLabel
                control={<Checkbox checked={filters.onlyFailed} onChange={(event) => update({ onlyFailed: event.target.checked })} />}
                label={t('onlyFailed')}
            />
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {[1000, 3000, 5000].map((value) => (
                    <Button key={value} size="small" variant="outlined" onClick={() => update({ minDurationMs: String(value) })}>
                        &gt; {value} ms
                    </Button>
                ))}
                <Button size="small" onClick={() => onChange(defaultPerformanceThreadFilters)}>
                    {t('hide')}
                </Button>
            </Box>
        </Box>
    );
}
