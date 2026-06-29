'use client';

import { Alert, Box, Button, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { useCallback, useEffect, useState } from 'react';
import { performanceService } from '@/services/performanceService';
import { PerformanceHistoryItem, PerformanceLiveSnapshot, PerformanceStatus } from '@/types/performance';
import { dash, formatDurationMs, formatPercent, formatStatusLabel, formatThroughput } from './PerformanceMetricFormatters';
import PerformanceThresholdStatusChip from './PerformanceThresholdStatusChip';

interface PerformanceLiveMonitorProps {
    item: PerformanceHistoryItem;
    onFinished: () => void;
}

function isFinalStatus(status?: PerformanceStatus | string | null) {
    return status === 'COMPLETED_PASSED' || status === 'COMPLETED_FAILED' || status === 'STOPPED' || status === 'ERROR';
}

export default function PerformanceLiveMonitor({ item, onFinished }: PerformanceLiveMonitorProps) {
    const t = useTranslations('performance');
    const [snapshot, setSnapshot] = useState<PerformanceLiveSnapshot | null>(null);
    const [error, setError] = useState<string | null>(null);
    const performanceResultId = item.performanceResultId;

    const loadSnapshot = useCallback(async () => {
        if (!performanceResultId) {
            return;
        }
        try {
            const data = await performanceService.getLive(performanceResultId);
            setSnapshot(data);
            setError(null);
            if (isFinalStatus(data.status)) {
                onFinished();
            }
        } catch {
            setError(t('metricsUnavailable'));
        }
    }, [onFinished, performanceResultId, t]);

    useEffect(() => {
        if (!performanceResultId) {
            return;
        }
        const initialTimeout = window.setTimeout(loadSnapshot, 0);
        const interval = window.setInterval(() => {
            if (!isFinalStatus(snapshot?.status)) {
                loadSnapshot();
            }
        }, 5000);
        return () => {
            window.clearTimeout(initialTimeout);
            window.clearInterval(interval);
        };
    }, [loadSnapshot, performanceResultId, snapshot?.status]);

    const handleStop = async (force: boolean) => {
        if (!performanceResultId) {
            return;
        }
        const data = force ? await performanceService.forceStop(performanceResultId) : await performanceService.stop(performanceResultId);
        setSnapshot(data);
    };

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{t('liveMonitor')} #{performanceResultId}</Typography>
                <PerformanceThresholdStatusChip status={snapshot?.status ?? item.performanceStatus} />
            </Box>
            {error && <Alert severity="info" sx={{ mb: 2 }}>{error}</Alert>}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, 1fr)' }, gap: 1.5 }}>
                <Typography variant="body2">{t('activeThreads')}: {dash(snapshot?.activeThreadCount)} / {dash(snapshot?.totalThreadCount)}</Typography>
                <Typography variant="body2">{t('completedSamples')}: {dash(snapshot?.completedSamples)}</Typography>
                <Typography variant="body2">{t('successfulSamples')}: {dash(snapshot?.successfulSamples)}</Typography>
                <Typography variant="body2">{t('failedSamples')}: {dash(snapshot?.failedSamples)}</Typography>
                <Typography variant="body2">{t('errorRate')}: {formatPercent(snapshot?.errorRate)}</Typography>
                <Typography variant="body2">{t('throughput')}: {formatThroughput(snapshot?.throughputPerSecond)}</Typography>
                <Typography variant="body2">{t('average')}: {formatDurationMs(snapshot?.averageElapsedTime)}</Typography>
                <Typography variant="body2">{t('p95')}: {formatDurationMs(snapshot?.p95ElapsedTime)}</Typography>
                <Typography variant="body2">{t('duration')}: {formatDurationMs(snapshot?.elapsedTimeMs)}</Typography>
                <Typography variant="body2">{t('estimatedRemaining')}: {formatDurationMs(snapshot?.estimatedRemainingMs)}</Typography>
                <Typography variant="body2">{t('lastCompletedStep')}: {dash(snapshot?.lastCompletedStep)}</Typography>
                <Typography variant="body2">{t('status')}: {formatStatusLabel(snapshot?.status ?? item.performanceStatus)}</Typography>
            </Box>
            {snapshot?.lastError && <Alert severity="error" sx={{ mt: 2 }}>{snapshot.lastError}</Alert>}
            {snapshot?.warnings && snapshot.warnings.length > 0 && <Alert severity="warning" sx={{ mt: 2 }}>{snapshot.warnings.join(' ')}</Alert>}
            <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
                <Button variant="outlined" color="warning" onClick={() => handleStop(false)} disabled={!performanceResultId || isFinalStatus(snapshot?.status)}>
                    {t('stop')}
                </Button>
                <Button variant="outlined" color="error" onClick={() => handleStop(true)} disabled={!performanceResultId || isFinalStatus(snapshot?.status)}>
                    {t('forceStop')}
                </Button>
            </Box>
        </Paper>
    );
}
