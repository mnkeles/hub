'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    FormControl,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material';
import { useTranslations } from 'next-intl';
import { performanceDatasetService } from '@/services/performanceDatasetService';
import { PerformanceDataset, PerformanceDatasetPreview } from '@/types/performance';

interface PerformanceDatasetPanelProps {
    projectId?: number | null;
    processFlowParameters?: string[];
    selectedDatasetId?: number | null;
    datasetMapping: Record<string, string>;
    onDatasetChange: (datasetId: number | null) => void;
    onMappingChange: (mapping: Record<string, string>) => void;
}

function errorMessage(error: unknown, fallback: string) {
    return error instanceof Error ? error.message : fallback;
}

export default function PerformanceDatasetPanel({
    projectId,
    processFlowParameters = [],
    selectedDatasetId = null,
    datasetMapping,
    onDatasetChange,
    onMappingChange,
}: PerformanceDatasetPanelProps) {
    const t = useTranslations('performance');
    const [datasets, setDatasets] = useState<PerformanceDataset[]>([]);
    const [preview, setPreview] = useState<PerformanceDatasetPreview | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [uploadName, setUploadName] = useState('');
    const [uploadDescription, setUploadDescription] = useState('');
    const [uploadFile, setUploadFile] = useState<File | null>(null);
    const [manualRowJson, setManualRowJson] = useState('{\n  "key": "value"\n}');

    const datasetFields = useMemo(() => Object.keys(preview?.dataset.columnSchema ?? {}), [preview]);
    const mappingParameters = useMemo(() => {
        const merged = new Set<string>([...processFlowParameters, ...Object.keys(datasetMapping)]);
        return [...merged].filter(Boolean);
    }, [datasetMapping, processFlowParameters]);

    const loadDatasets = useCallback(async () => {
        if (!projectId) {
            setDatasets([]);
            setPreview(null);
            return;
        }
        setLoading(true);
        try {
            const data = await performanceDatasetService.list(projectId);
            setDatasets(data);
        } catch (err) {
            setError(errorMessage(err, 'Dataset listesi yuklenemedi'));
        } finally {
            setLoading(false);
        }
    }, [projectId]);

    const loadPreview = useCallback(async (datasetId: number | null) => {
        if (!datasetId) {
            setPreview(null);
            return;
        }
        try {
            const data = await performanceDatasetService.preview(datasetId);
            setPreview(data);
            if (Object.keys(datasetMapping).length === 0 && data.dataset.defaultMapping) {
                onMappingChange(data.dataset.defaultMapping);
            }
        } catch (err) {
            setError(errorMessage(err, 'Dataset onizleme yuklenemedi'));
        }
    }, [datasetMapping, onMappingChange]);

    useEffect(() => {
        loadDatasets();
    }, [loadDatasets]);

    useEffect(() => {
        loadPreview(selectedDatasetId ?? null);
    }, [loadPreview, selectedDatasetId]);

    const handleDatasetSelect = (value: string) => {
        const id = value ? Number(value) : null;
        onDatasetChange(id);
        if (!id) {
            onMappingChange({});
        }
    };

    const handleMappingChange = (parameter: string, field: string) => {
        const next = { ...datasetMapping };
        if (!field) {
            delete next[parameter];
        } else {
            next[parameter] = field;
        }
        onMappingChange(next);
    };

    const handleUpload = async () => {
        if (!projectId || !uploadFile || !uploadName.trim()) {
            setError('Dataset yuklemek icin ad, proje ve dosya gereklidir.');
            return;
        }
        setError(null);
        try {
            const created = await performanceDatasetService.upload(projectId, uploadName.trim(), uploadDescription.trim() || null, datasetMapping, uploadFile);
            await loadDatasets();
            onDatasetChange(created.datasetId);
            setUploadName('');
            setUploadDescription('');
            setUploadFile(null);
        } catch (err) {
            setError(errorMessage(err, 'Dataset yuklenemedi'));
        }
    };

    const handleAddManualRow = async () => {
        if (!selectedDatasetId) {
            setError('Manuel satir eklemek icin dataset secin.');
            return;
        }
        try {
            const parsed = JSON.parse(manualRowJson);
            if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
                setError('Satir verisi JSON object olmalidir.');
                return;
            }
            await performanceDatasetService.addRow(selectedDatasetId, { data: parsed });
            await loadPreview(selectedDatasetId);
        } catch (err) {
            setError(errorMessage(err, 'Satir eklenemedi'));
        }
    };

    return (
        <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>{t('dataset')}</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 2 }}>
                <FormControl fullWidth size="small">
                    <InputLabel>{t('selectDataset')}</InputLabel>
                    <Select value={selectedDatasetId ? String(selectedDatasetId) : ''} label={t('selectDataset')} onChange={(event) => handleDatasetSelect(event.target.value)}>
                        <MenuItem value="">{t('noDataset')}</MenuItem>
                        {datasets.map((dataset) => (
                            <MenuItem key={dataset.datasetId} value={String(dataset.datasetId)}>
                                {dataset.name} ({dataset.rowCount})
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <TextField size="small" label={t('dataset')} value={uploadName} onChange={(event) => setUploadName(event.target.value)} />
                <TextField size="small" label="Description" value={uploadDescription} onChange={(event) => setUploadDescription(event.target.value)} />
                <Button component="label" variant="outlined" disabled={!projectId}>
                    {uploadFile ? uploadFile.name : t('uploadDataset')}
                    <input hidden type="file" accept=".csv,.json" onChange={(event) => setUploadFile(event.target.files?.[0] ?? null)} />
                </Button>
                <Button variant="contained" onClick={handleUpload} disabled={loading || !projectId || !uploadFile || !uploadName.trim()}>
                    {t('uploadDataset')}
                </Button>
            </Box>

            {selectedDatasetId && (
                <Box sx={{ mt: 2, display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
                    <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{t('datasetMapping')}</Typography>
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
                            {mappingParameters.length === 0 ? (
                                <Typography variant="body2" color="text.secondary">-</Typography>
                            ) : mappingParameters.map((parameter) => (
                                <FormControl fullWidth size="small" key={parameter}>
                                    <InputLabel>{parameter}</InputLabel>
                                    <Select value={datasetMapping[parameter] ?? ''} label={parameter} onChange={(event) => handleMappingChange(parameter, event.target.value)}>
                                        <MenuItem value="">-</MenuItem>
                                        {datasetFields.map((field) => <MenuItem key={field} value={field}>{field}</MenuItem>)}
                                    </Select>
                                </FormControl>
                            ))}
                        </Box>
                    </Box>
                    <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{t('manualRows')}</Typography>
                        <TextField fullWidth multiline minRows={4} size="small" label={t('rowDataJson')} value={manualRowJson} onChange={(event) => setManualRowJson(event.target.value)} />
                        <Button sx={{ mt: 1 }} variant="outlined" onClick={handleAddManualRow}>{t('addRow')}</Button>
                    </Box>
                </Box>
            )}

            {preview && (
                <Box sx={{ mt: 2 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>{t('datasetPreview')}</Typography>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                {datasetFields.map((field) => <TableCell key={field}>{field}</TableCell>)}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {preview.rows.map((row) => (
                                <TableRow key={row.rowId}>
                                    {datasetFields.map((field) => (
                                        <TableCell key={`${row.rowId}-${field}`}>{String(row.data?.[field] ?? '')}</TableCell>
                                    ))}
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Box>
            )}
        </Paper>
    );
}
