'use client';

import { Box, Chip, Paper, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceSloScore } from '@/types/performance';
import { dash } from './PerformanceMetricFormatters';

interface PerformanceSloScorePanelProps {
    score?: PerformanceSloScore | null;
    compact?: boolean;
}

function gradeColor(grade?: string | null): 'success' | 'info' | 'warning' | 'error' | 'default' {
    if (grade === 'A') return 'success';
    if (grade === 'B') return 'info';
    if (grade === 'C') return 'warning';
    if (grade === 'D' || grade === 'F') return 'error';
    return 'default';
}

function ListBlock({ title, values }: { title: string; values?: string[] }) {
    const visible = values?.filter(Boolean) ?? [];
    return (
        <Box>
            <Typography variant="caption" color="text.secondary">{title}</Typography>
            <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
                {visible.length === 0 ? (
                    <Typography component="li" variant="body2">-</Typography>
                ) : visible.map((value, index) => (
                    <Typography component="li" variant="body2" key={`${title}-${index}`}>
                        {value}
                    </Typography>
                ))}
            </Box>
        </Box>
    );
}

export default function PerformanceSloScorePanel({ score, compact = false }: PerformanceSloScorePanelProps) {
    const t = useTranslations('performance');

    if (!score) {
        return null;
    }

    if (compact) {
        return (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, whiteSpace: 'nowrap' }}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>{score.score}/100</Typography>
                <Chip size="small" color={gradeColor(score.grade)} label={score.grade} />
            </Box>
        );
    }

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap', mb: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{t('sloScore')}</Typography>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Chip color={gradeColor(score.grade)} label={`${score.score}/100`} />
                    <Chip color={gradeColor(score.grade)} variant="outlined" label={`${t('sloGrade')}: ${score.grade}`} />
                    <Chip variant="outlined" label={`${t('sloStatus')}: ${score.status}`} />
                </Box>
            </Box>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2, mb: 2 }}>
                <ListBlock title={t('strengths')} values={score.strengths} />
                <ListBlock title={t('weaknesses')} values={score.weaknesses} />
                <ListBlock title={t('recommendations')} values={score.recommendations} />
            </Box>
            {score.metricScores && score.metricScores.length > 0 && (
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>{t('metricScores')}</TableCell>
                            <TableCell>{t('sloScore')}</TableCell>
                            <TableCell>{t('thresholdResult')}</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {score.metricScores.map((metric) => (
                            <TableRow key={metric.metricName}>
                                <TableCell>{metric.metricName}</TableCell>
                                <TableCell>{metric.score}/{metric.maxScore}</TableCell>
                                <TableCell>{dash(metric.message)}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            )}
        </Paper>
    );
}
