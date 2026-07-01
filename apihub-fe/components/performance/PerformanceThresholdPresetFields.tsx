'use client';

import { Box, FormControl, InputLabel, MenuItem, Select, TextField } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceThresholdConfig, PerformanceThresholdPreset } from '@/types/performance';

export const PERFORMANCE_THRESHOLD_PRESET_DEFAULTS: Record<Exclude<PerformanceThresholdPreset, 'CUSTOM'>, Required<PerformanceThresholdConfig>> = {
    SMOKE: {
        maxErrorRatePercent: 5,
        maxAverageMs: 3000,
        maxP95Ms: 8000,
        maxP99Ms: 12000,
        minThroughputPerSecond: 1,
    },
    NORMAL: {
        maxErrorRatePercent: 1,
        maxAverageMs: 1000,
        maxP95Ms: 3000,
        maxP99Ms: 5000,
        minThroughputPerSecond: 20,
    },
    STRESS: {
        maxErrorRatePercent: 2,
        maxAverageMs: 1500,
        maxP95Ms: 5000,
        maxP99Ms: 8000,
        minThroughputPerSecond: 50,
    },
    STRICT_SLA: {
        maxErrorRatePercent: 0.1,
        maxAverageMs: 500,
        maxP95Ms: 1000,
        maxP99Ms: 2000,
        minThroughputPerSecond: 50,
    },
};

interface PerformanceThresholdPresetFieldsProps {
    thresholdPreset: PerformanceThresholdPreset;
    thresholdConfig: PerformanceThresholdConfig;
    onPresetChange: (preset: PerformanceThresholdPreset) => void;
    onThresholdConfigChange: (config: PerformanceThresholdConfig) => void;
}

const PRESETS: PerformanceThresholdPreset[] = ['SMOKE', 'NORMAL', 'STRESS', 'STRICT_SLA', 'CUSTOM'];

function presetLabelKey(preset: PerformanceThresholdPreset) {
    return {
        SMOKE: 'thresholdPresetSmoke',
        NORMAL: 'thresholdPresetNormal',
        STRESS: 'thresholdPresetStress',
        STRICT_SLA: 'thresholdPresetStrictSla',
        CUSTOM: 'thresholdPresetCustom',
    }[preset];
}

function numberValue(value: number | undefined) {
    return value ?? '';
}

export default function PerformanceThresholdPresetFields({
    thresholdPreset,
    thresholdConfig,
    onPresetChange,
    onThresholdConfigChange,
}: PerformanceThresholdPresetFieldsProps) {
    const t = useTranslations('performance');

    const handlePresetChange = (preset: PerformanceThresholdPreset) => {
        onPresetChange(preset);
        if (preset !== 'CUSTOM') {
            onThresholdConfigChange(PERFORMANCE_THRESHOLD_PRESET_DEFAULTS[preset]);
        }
    };

    const handleNumberChange = (key: keyof PerformanceThresholdConfig, rawValue: string) => {
        onThresholdConfigChange({
            ...thresholdConfig,
            [key]: rawValue === '' ? undefined : Number(rawValue),
        });
    };

    return (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(6, 1fr)' }, gap: 2 }}>
            <FormControl fullWidth size="small">
                <InputLabel>{t('thresholdPreset')}</InputLabel>
                <Select
                    value={thresholdPreset}
                    label={t('thresholdPreset')}
                    onChange={(event) => handlePresetChange(event.target.value as PerformanceThresholdPreset)}
                >
                    {PRESETS.map((preset) => (
                        <MenuItem key={preset} value={preset}>
                            {t(presetLabelKey(preset))}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>
            <TextField fullWidth size="small" label={t('maxErrorRatePercent')} type="number" value={numberValue(thresholdConfig.maxErrorRatePercent)} onChange={(event) => handleNumberChange('maxErrorRatePercent', event.target.value)} inputProps={{ min: 0, step: 0.1 }} />
            <TextField fullWidth size="small" label={t('maxAverageMs')} type="number" value={numberValue(thresholdConfig.maxAverageMs)} onChange={(event) => handleNumberChange('maxAverageMs', event.target.value)} inputProps={{ min: 1 }} />
            <TextField fullWidth size="small" label={t('maxP95Ms')} type="number" value={numberValue(thresholdConfig.maxP95Ms)} onChange={(event) => handleNumberChange('maxP95Ms', event.target.value)} inputProps={{ min: 1 }} />
            <TextField fullWidth size="small" label={t('maxP99Ms')} type="number" value={numberValue(thresholdConfig.maxP99Ms)} onChange={(event) => handleNumberChange('maxP99Ms', event.target.value)} inputProps={{ min: 1 }} />
            <TextField fullWidth size="small" label={t('minThroughputPerSecond')} type="number" value={numberValue(thresholdConfig.minThroughputPerSecond)} onChange={(event) => handleNumberChange('minThroughputPerSecond', event.target.value)} inputProps={{ min: 0 }} />
        </Box>
    );
}
