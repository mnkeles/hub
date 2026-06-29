'use client';

import { Alert, Box, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography } from '@mui/material';
import { useTranslations } from 'next-intl';
import { PerformanceErrorAnalysis } from '@/types/performance';
import { dash, formatDurationMs, formatPercent } from './PerformanceMetricFormatters';

interface PerformanceErrorAnalysisPanelProps {
    errorAnalysis?: PerformanceErrorAnalysis | null;
}

export default function PerformanceErrorAnalysisPanel({ errorAnalysis }: PerformanceErrorAnalysisPanelProps) {
    const t = useTranslations('performance');

    if (!errorAnalysis || !errorAnalysis.totalErrorCount) {
        return <Alert severity="info">{t('noErrorsFound')}</Alert>;
    }

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
                <Typography variant="body2"><strong>{t('failedSamples')}:</strong> {dash(errorAnalysis.totalErrorCount)}</Typography>
                <Typography variant="body2"><strong>{t('errorRate')}:</strong> {formatPercent(errorAnalysis.errorRate)}</Typography>
                <Tooltip title={errorAnalysis.lastError || '-'}>
                    <Typography variant="body2" sx={{ maxWidth: 420, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        <strong>{t('lastError')}:</strong> {dash(errorAnalysis.lastError)}
                    </Typography>
                </Tooltip>
            </Box>

            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow><TableCell>Type</TableCell><TableCell>Count</TableCell></TableRow>
                        </TableHead>
                        <TableBody>
                            {(errorAnalysis.errorsByType ?? []).map((row) => (
                                <TableRow key={row.errorType}><TableCell>{row.errorType}</TableCell><TableCell>{row.count}</TableCell></TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow><TableCell>{t('step')}</TableCell><TableCell>Count</TableCell></TableRow>
                        </TableHead>
                        <TableBody>
                            {(errorAnalysis.errorsByStep ?? []).map((row) => (
                                <TableRow key={row.stepName}><TableCell>{row.stepName}</TableCell><TableCell>{row.count}</TableCell></TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Box>

            <TableContainer>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>{t('threadDetail')}</TableCell>
                            <TableCell>{t('step')}</TableCell>
                            <TableCell>{t('duration')}</TableCell>
                            <TableCell>Type</TableCell>
                            <TableCell>{t('error')}</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {(errorAnalysis.failedRequests ?? []).map((row, index) => (
                            <TableRow key={`${row.threadNumber}-${row.stepName}-${index}`}>
                                <TableCell>{dash(row.threadNumber)}</TableCell>
                                <TableCell>{dash(row.stepName)}</TableCell>
                                <TableCell>{formatDurationMs(row.elapsedTime)}</TableCell>
                                <TableCell>{dash(row.errorType)}</TableCell>
                                <TableCell>
                                    <Tooltip title={row.errorMessage || '-'}>
                                        <Typography variant="caption" sx={{ display: 'block', maxWidth: 420, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                            {dash(row.errorMessage)}
                                        </Typography>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}
