'use client';

import React, { useCallback, useEffect, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Chip,
    FormControlLabel,
    Paper,
    Switch,
    TextField,
    Typography,
} from '@mui/material';
import { useTranslations } from 'next-intl';
import { performanceScheduleService } from '@/services/performanceScheduleService';
import { PerformanceRequest, PerformanceResultDto, PerformanceSchedule } from '@/types/performance';
import { formatDateTime } from './PerformanceMetricFormatters';

interface PerformanceSchedulePanelProps {
    projectId?: number | null;
    processFlowId?: number | null;
    requestSnapshot: PerformanceRequest | null;
    onRunStarted: (result: PerformanceResultDto) => void;
}

function errorMessage(error: unknown, fallback: string) {
    return error instanceof Error ? error.message : fallback;
}

export default function PerformanceSchedulePanel({ projectId, processFlowId, requestSnapshot, onRunStarted }: PerformanceSchedulePanelProps) {
    const t = useTranslations('performance');
    const [items, setItems] = useState<PerformanceSchedule[]>([]);
    const [name, setName] = useState('');
    const [cronExpression, setCronExpression] = useState('0 0 9 * * *');
    const [timezone, setTimezone] = useState(() => Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul');
    const [enabled, setEnabled] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadSchedules = useCallback(async () => {
        if (!projectId || !processFlowId) {
            setItems([]);
            return;
        }
        try {
            setItems(await performanceScheduleService.list(projectId, processFlowId));
        } catch (err) {
            setError(errorMessage(err, 'Schedule listesi yuklenemedi'));
        }
    }, [processFlowId, projectId]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadSchedules();
    }, [loadSchedules]);

    const handleSave = async () => {
        if (!projectId || !processFlowId || !requestSnapshot || !name.trim()) {
            setError('Schedule kaydetmek icin ad, proje, akis ve test ayarlari gereklidir.');
            return;
        }
        setError(null);
        try {
            await performanceScheduleService.create({
                projectId,
                processFlowId,
                name: name.trim(),
                cronExpression,
                timezone,
                enabled,
                requestSnapshot,
            });
            setName('');
            await loadSchedules();
        } catch (err) {
            setError(errorMessage(err, 'Schedule kaydedilemedi'));
        }
    };

    const handleToggle = async (item: PerformanceSchedule, nextEnabled: boolean) => {
        await performanceScheduleService.setEnabled(item.scheduleId, nextEnabled);
        await loadSchedules();
    };

    const handleRunNow = async (item: PerformanceSchedule) => {
        const result = await performanceScheduleService.runNow(item.scheduleId);
        onRunStarted(result);
        await loadSchedules();
    };

    const handleDeactivate = async (item: PerformanceSchedule) => {
        await performanceScheduleService.deactivate(item.scheduleId);
        await loadSchedules();
    };

    return (
        <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>{t('schedule')}</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr auto' }, gap: 2, alignItems: 'center' }}>
                <TextField size="small" label={t('scheduleName')} value={name} onChange={(event) => setName(event.target.value)} />
                <TextField size="small" label={t('cronExpression')} value={cronExpression} onChange={(event) => setCronExpression(event.target.value)} />
                <TextField size="small" label={t('timezone')} value={timezone} onChange={(event) => setTimezone(event.target.value)} />
                <FormControlLabel control={<Switch checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />} label={enabled ? t('enabled') : t('disabled')} />
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Button size="small" variant="outlined" onClick={() => setCronExpression('0 0 * * * *')}>Hourly</Button>
                    <Button size="small" variant="outlined" onClick={() => setCronExpression('0 0 9 * * *')}>Daily</Button>
                    <Button size="small" variant="outlined" onClick={() => setCronExpression('0 0 9 * * MON')}>Weekly</Button>
                    <Button size="small" variant="contained" onClick={handleSave} disabled={!projectId || !processFlowId || !requestSnapshot || !name.trim()}>{t('saveSchedule')}</Button>
                </Box>
            </Box>

            <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
                {items.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">-</Typography>
                ) : items.map((item) => (
                    <Paper variant="outlined" sx={{ p: 1.5, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }} key={item.scheduleId}>
                        <Box>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{item.name}</Typography>
                            <Typography variant="caption" color="text.secondary">
                                {item.cronExpression} | {t('lastRun')}: {formatDateTime(item.lastRunAt)} | {t('nextRun')}: {formatDateTime(item.nextRunAt)}
                            </Typography>
                        </Box>
                        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                            <Chip size="small" label={item.lastStatus ?? '-'} />
                            <Switch size="small" checked={item.enabled} onChange={(event) => handleToggle(item, event.target.checked)} />
                            <Button size="small" variant="outlined" onClick={() => handleRunNow(item)}>{t('runNow')}</Button>
                            <Button size="small" color="error" variant="outlined" onClick={() => handleDeactivate(item)}>{t('disabled')}</Button>
                        </Box>
                    </Paper>
                ))}
            </Box>
        </Paper>
    );
}
