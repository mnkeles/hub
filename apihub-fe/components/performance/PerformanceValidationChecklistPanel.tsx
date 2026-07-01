'use client';

import { Box, Button, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import { PerformanceComparisonResult, PerformanceValidationChecklist, PerformanceValidationStatus } from '@/types/performance';

interface PerformanceValidationChecklistPanelProps {
    checklist?: PerformanceValidationChecklist | null;
    baselineComparison?: PerformanceComparisonResult | null;
    performanceResultId?: number;
    onSaveNote: (note: string) => Promise<void>;
}

function statusColor(status: PerformanceValidationStatus): 'success' | 'error' | 'warning' | 'default' {
    if (status === 'PASSED') {
        return 'success';
    }
    if (status === 'FAILED') {
        return 'error';
    }
    if (status === 'WARNING') {
        return 'warning';
    }
    return 'default';
}

function displayValue(value: unknown) {
    if (value === null || value === undefined || value === '') {
        return '-';
    }
    return String(value);
}

export default function PerformanceValidationChecklistPanel({
    checklist,
    baselineComparison,
    performanceResultId,
    onSaveNote,
}: PerformanceValidationChecklistPanelProps) {
    const t = useTranslations('performance');
    const [note, setNote] = useState(checklist?.manualNote ?? '');
    const [saving, setSaving] = useState(false);
    const items = checklist?.items ?? [];
    const metrics = baselineComparison?.metrics ?? [];

    useEffect(() => {
        setNote(checklist?.manualNote ?? '');
    }, [checklist?.manualNote]);

    const handleSave = async () => {
        if (!performanceResultId) {
            return;
        }
        setSaving(true);
        try {
            await onSaveNote(note);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Box>
                <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
                    {t('validationChecklist')}
                </Typography>
                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>{t('step')}</TableCell>
                                <TableCell>{t('status')}</TableCell>
                                <TableCell>{t('detail')}</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {items.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={3}>-</TableCell>
                                </TableRow>
                            ) : items.map((item) => (
                                <TableRow key={item.key}>
                                    <TableCell>{item.label}</TableCell>
                                    <TableCell>
                                        <Chip size="small" color={statusColor(item.status)} label={item.status} />
                                    </TableCell>
                                    <TableCell>{displayValue(item.message)}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Box>

            {metrics.length > 0 && (
                <Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
                        {t('baselineComparison')}
                    </Typography>
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>{t('step')}</TableCell>
                                    <TableCell>Base</TableCell>
                                    <TableCell>Target</TableCell>
                                    <TableCell>Delta</TableCell>
                                    <TableCell>Direction</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {metrics.map((metric) => (
                                    <TableRow key={metric.metricName}>
                                        <TableCell>{metric.metricName}</TableCell>
                                        <TableCell>{displayValue(metric.baseValue)}</TableCell>
                                        <TableCell>{displayValue(metric.targetValue)}</TableCell>
                                        <TableCell>{displayValue(metric.delta)}</TableCell>
                                        <TableCell>{displayValue(metric.direction)}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Box>
            )}

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <TextField
                    label={t('manualValidationNote')}
                    multiline
                    minRows={3}
                    value={note}
                    onChange={(event) => setNote(event.target.value.slice(0, 1000))}
                    helperText={`${note.length}/1000`}
                    fullWidth
                />
                <Box>
                    <Button variant="contained" onClick={handleSave} disabled={!performanceResultId || saving}>
                        {t('saveValidationNote')}
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}
