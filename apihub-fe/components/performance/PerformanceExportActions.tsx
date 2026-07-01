'use client';

import { Box, Button } from '@mui/material';
import { useTranslations } from 'next-intl';
import { performanceService } from '@/services/performanceService';

interface PerformanceExportActionsProps {
    performanceResultId?: number;
    onSuccess?: (message: string) => void;
    onError?: (message: string) => void;
}

function downloadBlob(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
}

export default function PerformanceExportActions({ performanceResultId, onSuccess, onError }: PerformanceExportActionsProps) {
    const t = useTranslations('performance');
    const disabled = !performanceResultId;

    const handleJson = async () => {
        if (!performanceResultId) {
            return;
        }
        try {
            const payload = await performanceService.exportJson(performanceResultId);
            downloadBlob(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' }), `performance-run-${performanceResultId}-summary.json`);
            onSuccess?.(t('exportSucceeded'));
        } catch {
            onError?.(t('exportFailed'));
        }
    };

    const handleCsv = async () => {
        if (!performanceResultId) {
            return;
        }
        try {
            const blob = await performanceService.exportCsv(performanceResultId);
            downloadBlob(blob, `performance-run-${performanceResultId}-details.csv`);
            onSuccess?.(t('exportSucceeded'));
        } catch {
            onError?.(t('exportFailed'));
        }
    };

    return (
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button variant="outlined" disabled={disabled} onClick={handleCsv}>{t('exportCsv')}</Button>
            <Button variant="outlined" disabled={disabled} onClick={handleJson}>{t('exportJson')}</Button>
        </Box>
    );
}
