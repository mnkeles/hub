'use client';

import { Alert, Box, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceAiReport, PerformanceSloScore } from '@/types/performance';
import { dash, formatDateTime } from './PerformanceMetricFormatters';
import PerformanceSloScorePanel from './PerformanceSloScorePanel';

interface PerformanceAiReportPanelProps {
    report?: PerformanceAiReport | null;
    sloScore?: PerformanceSloScore | null;
}

function TextBlock({ label, value }: { label: string; value?: string | null }) {
    return (
        <Box>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{dash(value)}</Typography>
        </Box>
    );
}

function ListBlock({ label, values }: { label: string; values?: string[] | null }) {
    const visibleValues = values?.filter((value) => value && value.trim().length > 0) ?? [];

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{label}</Typography>
            {visibleValues.length === 0 ? (
                <Typography variant="body2" color="text.secondary">-</Typography>
            ) : (
                <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
                    {visibleValues.map((value, index) => (
                        <Typography component="li" variant="body2" key={`${label}-${index}`} sx={{ mb: 0.5 }}>
                            {value}
                        </Typography>
                    ))}
                </Box>
            )}
        </Paper>
    );
}

export default function PerformanceAiReportPanel({ report, sloScore }: PerformanceAiReportPanelProps) {
    const t = useTranslations('performance');

    if (!report && !sloScore) {
        return <Alert severity="info">{t('reportNotGenerated')}</Alert>;
    }

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <PerformanceSloScorePanel score={sloScore} />
            {!report && <Alert severity="info">{t('reportNotGenerated')}</Alert>}
            {report && (
                <>
            <Paper variant="outlined" sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1.5, flexWrap: 'wrap', mb: 2 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{t('aiReport')}</Typography>
                    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                        <Chip size="small" color={report.overallStatus === 'PASSED' ? 'success' : report.overallStatus === 'FAILED' ? 'error' : 'default'} label={dash(report.overallStatus)} />
                        <Chip size="small" variant="outlined" label={`${t('reportSource')}: ${dash(report.source)}`} />
                        <Chip size="small" variant="outlined" label={`${t('generatedAt')}: ${formatDateTime(report.generatedAt)}`} />
                    </Box>
                </Box>
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                    <TextBlock label={t('executiveSummary')} value={report.executiveSummary} />
                    <TextBlock label={t('businessImpact')} value={report.businessImpact} />
                </Box>
            </Paper>

            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                <ListBlock label={t('goodPoints')} values={report.goodPoints} />
                <ListBlock label={t('badPoints')} values={report.badPoints} />
                <ListBlock label={t('risks')} values={report.risks} />
                <ListBlock label={t('recommendedActions')} values={report.recommendedActions} />
            </Box>

            <Paper variant="outlined" sx={{ p: 2 }}>
                <TextBlock label={t('technicalDetails')} value={report.technicalDetails} />
            </Paper>
                </>
            )}
        </Box>
    );
}
