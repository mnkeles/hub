'use client';

import { Alert, Box, Chip, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceAiManagementReport } from '@/types/performance';

interface Props {
    aiReport?: PerformanceAiManagementReport | null;
}

export default function PerformanceAiNarrativePanel({ aiReport }: Props) {
    const t = useTranslations('performance');

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1, flexWrap: 'wrap', mb: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                    {t('aiNarrative')}
                </Typography>
                <Chip size="small" color={aiReport?.generated ? 'success' : 'default'} label={aiReport?.generated ? t('aiGenerated') : t('aiFallback')} />
            </Box>
            {!aiReport ? (
                <Alert severity="info">{t('noAiReport')}</Alert>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {aiReport.errorMessage && <Alert severity="info">{aiReport.errorMessage}</Alert>}
                    {aiReport.failureReason && <Alert severity="warning">{aiReport.failureReason}</Alert>}
                    {(aiReport.validationErrors ?? []).length > 0 && (
                        <Alert severity="warning">
                            <Typography variant="caption" sx={{ fontWeight: 700 }}>{t('validationErrors')}</Typography>
                            {(aiReport.validationErrors ?? []).map((error) => (
                                <Typography key={error} variant="body2">- {error}</Typography>
                            ))}
                        </Alert>
                    )}
                    <Box>
                        <Typography variant="caption" color="text.secondary">{t('technicalFindings')}</Typography>
                        <Typography variant="body2">{aiReport.technicalNarrative || '-'}</Typography>
                    </Box>
                    <Box>
                        <Typography variant="caption" color="text.secondary">{t('rootCauseHints')}</Typography>
                        <Typography variant="body2">{aiReport.rootCauseNarrative || '-'}</Typography>
                    </Box>
                    <Box>
                        <Typography variant="caption" color="text.secondary">{t('releaseReadiness')}</Typography>
                        <Typography variant="body2">{aiReport.releaseReadinessNarrative || '-'}</Typography>
                    </Box>
                    {(aiReport.limitations ?? []).length > 0 && (
                        <Box>
                            <Typography variant="caption" color="text.secondary">{t('limitations')}</Typography>
                            {(aiReport.limitations ?? []).map((limitation) => (
                                <Typography key={limitation} variant="body2">- {limitation}</Typography>
                            ))}
                        </Box>
                    )}
                </Box>
            )}
        </Paper>
    );
}
