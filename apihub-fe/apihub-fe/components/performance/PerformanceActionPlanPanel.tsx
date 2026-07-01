'use client';

import { Alert, Box, Chip, List, ListItem, ListItemText, Paper, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceAiManagementReport, PerformanceManagementReport } from '@/types/performance';

interface Props {
    aiReport?: PerformanceAiManagementReport | null;
    managementReport?: PerformanceManagementReport | null;
}

function priorityColor(priority?: string | null): 'error' | 'warning' | 'info' | 'default' {
    if (priority === 'P0') return 'error';
    if (priority === 'P1') return 'warning';
    if (priority === 'P2') return 'info';
    return 'default';
}

export default function PerformanceActionPlanPanel({ aiReport, managementReport }: Props) {
    const t = useTranslations('performance');
    const aiActions = aiReport?.generated === true ? aiReport.recommendedActionPlan ?? [] : [];
    const fallbackActions = managementReport?.recommendedActions ?? [];

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                {t('actionPlan')}
            </Typography>
            {aiActions.length > 0 ? (
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' }, gap: 1.5 }}>
                    {aiActions.map((action, index) => (
                        <Paper key={`${action.priority}-${action.title ?? index}`} variant="outlined" sx={{ p: 1.5 }}>
                            <Chip size="small" color={priorityColor(action.priority)} label={action.priority || '-'} sx={{ mb: 1 }} />
                            <Typography variant="body2" sx={{ fontWeight: 700 }}>{action.title || '-'}</Typography>
                            <Typography variant="body2">{action.description || '-'}</Typography>
                            <Typography variant="caption" color="text.secondary">
                                {action.relatedStepName || '-'} | {action.relatedMetric || '-'}
                            </Typography>
                        </Paper>
                    ))}
                </Box>
            ) : fallbackActions.length > 0 ? (
                <List dense disablePadding>
                    {fallbackActions.map((action) => (
                        <ListItem key={action} disableGutters>
                            <ListItemText primary={action} />
                        </ListItem>
                    ))}
                </List>
            ) : (
                <Alert severity="info">{t('reportUnavailable')}</Alert>
            )}
        </Paper>
    );
}
