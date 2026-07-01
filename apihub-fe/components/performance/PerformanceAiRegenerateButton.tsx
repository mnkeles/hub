'use client';

import { useState } from 'react';
import RefreshIcon from '@mui/icons-material/Refresh';
import { Button, CircularProgress } from '@mui/material';
import { useTranslations } from 'next-intl';
import { performanceService } from '@/services/performanceService';
import { PerformanceAiManagementReport } from '@/types/performance';

interface Props {
    performanceResultId?: number;
    onRegenerated: (report: PerformanceAiManagementReport) => void;
    onSuccess?: (message: string) => void;
    onError?: (message: string) => void;
}

export default function PerformanceAiRegenerateButton({
    performanceResultId,
    onRegenerated,
    onSuccess,
    onError,
}: Props) {
    const t = useTranslations('performance');
    const [loading, setLoading] = useState(false);

    const handleClick = async () => {
        if (!performanceResultId) {
            return;
        }
        setLoading(true);
        try {
            const report = await performanceService.regenerateAiReport(performanceResultId);
            onRegenerated(report);
            onSuccess?.(t('aiReportRegenerated'));
        } catch {
            onError?.(t('aiReportRegenerationFailed'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <Button
            variant="outlined"
            size="small"
            onClick={handleClick}
            disabled={!performanceResultId || loading}
            startIcon={loading ? <CircularProgress size={14} /> : <RefreshIcon fontSize="small" />}
        >
            {loading ? t('regeneratingAiReport') : t('regenerateAiReport')}
        </Button>
    );
}
