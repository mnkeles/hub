'use client';

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import HistoryIcon from '@mui/icons-material/History';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import FloatingChat from '@/components/FloatingChat';
import PerformanceAnalysisPanel from '@/components/performance/PerformanceAnalysisPanel';
import PerformanceChartsPanel from '@/components/performance/PerformanceChartsPanel';
import PerformanceComparisonDialog from '@/components/performance/PerformanceComparisonDialog';
import PerformanceErrorAnalysisPanel from '@/components/performance/PerformanceErrorAnalysisPanel';
import PerformanceExportActions from '@/components/performance/PerformanceExportActions';
import PerformanceLiveMonitor from '@/components/performance/PerformanceLiveMonitor';
import PerformanceManagementReportPanel from '@/components/performance/PerformanceManagementReportPanel';
import PerformanceRunSummaryTable from '@/components/performance/PerformanceRunSummaryTable';
import PerformanceStepSummaryTable from '@/components/performance/PerformanceStepSummaryTable';
import PerformanceThreadDetailTable from '@/components/performance/PerformanceThreadDetailTable';
import PerformanceThreadFilters, {
    defaultPerformanceThreadFilters,
    PerformanceThreadFilterState,
} from '@/components/performance/PerformanceThreadFilters';
import PerformanceThresholdPresetFields, { PERFORMANCE_THRESHOLD_PRESET_DEFAULTS } from '@/components/performance/PerformanceThresholdPresetFields';
import PerformanceValidationChecklistPanel from '@/components/performance/PerformanceValidationChecklistPanel';
import { useProject } from '@/contexts/ProjectContext';
import { generalWebSystemService } from '@/services/generalWebSystemService';
import { performanceService } from '@/services/performanceService';
import { processFlowService } from '@/services/processFlowService';
import { GeneralWebSystemDto, ProcessFlowDto } from '@/types/api';
import {
    PerformanceAiManagementReport,
    PerformanceComparisonResult,
    PerformanceDetailResponse,
    PerformanceExportPayload,
    PerformanceHistoryItem,
    PerformanceResultDto,
    PerformanceThresholdConfig,
    PerformanceThresholdPreset,
} from '@/types/performance';

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback;
}

function optionalNumber(value: number | ''): number | null {
    return value === '' ? null : Number(value);
}

const NORMAL_DEFAULTS: Required<PerformanceThresholdConfig> = PERFORMANCE_THRESHOLD_PRESET_DEFAULTS.NORMAL;

function hasCompleteThresholdConfig(config: PerformanceThresholdConfig) {
    return typeof config.maxErrorRatePercent === 'number'
        && typeof config.maxAverageMs === 'number'
        && typeof config.maxP95Ms === 'number'
        && typeof config.maxP99Ms === 'number'
        && typeof config.minThroughputPerSecond === 'number';
}

export default function PerformanceTestsPage() {
    const t = useTranslations('performance');
    const { selectedProject } = useProject();
    const [processFlows, setProcessFlows] = useState<ProcessFlowDto[]>([]);
    const [environments, setEnvironments] = useState<GeneralWebSystemDto[]>([]);
    const [results, setResults] = useState<PerformanceResultDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const [environment, setEnvironment] = useState('');
    const [processFlowId, setProcessFlowId] = useState<number>(0);
    const [threadCount, setThreadCount] = useState<number>(1);
    const [rampUpPeriod, setRampUpPeriod] = useState<number>(0);
    const [durationSeconds, setDurationSeconds] = useState<number | ''>('');
    const [loopCount, setLoopCount] = useState<number | ''>(1);
    const [thinkTimeMs, setThinkTimeMs] = useState<number | ''>('');
    const [timeoutMs, setTimeoutMs] = useState<number | ''>('');
    const [environmentBaseUrl, setEnvironmentBaseUrl] = useState('');
    const [thresholdPreset, setThresholdPreset] = useState<PerformanceThresholdPreset>('NORMAL');
    const [thresholdConfig, setThresholdConfig] = useState<PerformanceThresholdConfig>(NORMAL_DEFAULTS);

    const [detailDialogOpen, setDetailDialogOpen] = useState(false);
    const [selectedResult, setSelectedResult] = useState<PerformanceResultDto | null>(null);
    const [selectedHistoryItem, setSelectedHistoryItem] = useState<PerformanceHistoryItem | null>(null);
    const [detailData, setDetailData] = useState<PerformanceDetailResponse | null>(null);
    const [analysisData, setAnalysisData] = useState<PerformanceExportPayload | null>(null);
    const [detailTab, setDetailTab] = useState(0);
    const [threadFilters, setThreadFilters] = useState<PerformanceThreadFilterState>(defaultPerformanceThreadFilters);
    const [loadingDetail, setLoadingDetail] = useState(false);

    const [showHistory, setShowHistory] = useState(false);
    const [historyData, setHistoryData] = useState<PerformanceHistoryItem[]>([]);
    const [loadingHistory, setLoadingHistory] = useState(false);
    const [compareSelection, setCompareSelection] = useState<number[]>([]);
    const [comparisonOpen, setComparisonOpen] = useState(false);
    const [comparisonResult, setComparisonResult] = useState<PerformanceComparisonResult | null>(null);
    const [loadingComparison, setLoadingComparison] = useState(false);
    const [settingBaselineId, setSettingBaselineId] = useState<number | null>(null);
    const previousProcessFlowId = useRef(processFlowId);

    const selectedFlow = useMemo(
        () => processFlows.find((flow) => flow.processFlowId === processFlowId),
        [processFlowId, processFlows]
    );
    const selectedFlowStepCount = Math.max(1, selectedFlow?.processFlowStepList?.length ?? 1);
    const estimatedSamples = typeof loopCount === 'number' ? threadCount * loopCount * selectedFlowStepCount : null;

    const runningItems = useMemo<PerformanceHistoryItem[]>(() => results.map((result) => ({
        performanceResultId: result.performanceResultId,
        performanceStatus: result.performanceStatus,
        threadCount: result.threadCount,
        rampUpPeriod: result.rampUpPeriod,
        durationSeconds: result.durationSeconds,
        loopCount: result.loopCount,
        thinkTimeMs: result.thinkTimeMs,
        timeoutMs: result.timeoutMs,
        environmentBaseUrl: result.environmentBaseUrl,
        runSummary: result.runSummary ?? null,
        thresholdResult: result.thresholdResult ?? null,
        analysisSummary: result.analysisSummary ?? null,
        errorAnalysis: result.errorAnalysis ?? null,
        environmentMetrics: result.environmentMetrics ?? null,
        resultSchemaVersion: result.resultSchemaVersion ?? null,
        thresholdPreset: result.thresholdPreset ?? null,
        thresholdConfig: result.thresholdConfig ?? null,
        baseline: result.baseline ?? null,
        baselineResultId: result.baselineResultId ?? null,
        baselineComparison: result.baselineComparison ?? null,
        validationChecklist: result.validationChecklist ?? null,
        insightReport: result.insightReport ?? null,
        aiManagementReport: result.aiManagementReport ?? null,
        performanceSummaries: result.performanceSummaries ?? [],
        createdAt: result.createdAt ?? result.runSummary?.startedAt ?? new Date().toISOString(),
    })), [results]);

    const fetchProcessFlows = useCallback(async () => {
        if (!selectedProject) {
            return;
        }
        try {
            const data = await processFlowService.getAll();
            const filtered = data.filter((flow: ProcessFlowDto) => flow.projectId === selectedProject.projectId);
            setProcessFlows(filtered);
            if (filtered.length > 0 && filtered[0].processFlowId !== null) {
                setProcessFlowId(filtered[0].processFlowId);
            }
        } catch (err: unknown) {
            setError(getErrorMessage(err, 'Akışlar yüklenemedi'));
        }
    }, [selectedProject]);

    const fetchEnvironments = useCallback(async () => {
        if (!selectedProject) {
            return;
        }
        try {
            const data = await generalWebSystemService.getAll();
            const filtered = data.filter((sys: GeneralWebSystemDto) => sys.projectId === selectedProject.projectId && sys.actv);
            setEnvironments(filtered);
            if (filtered.length > 0) {
                setEnvironment(filtered[0].shortCode);
            }
        } catch (err: unknown) {
            setError(getErrorMessage(err, 'Ortamlar yüklenemedi'));
        }
    }, [selectedProject]);

    const refreshHistory = useCallback(async () => {
        if (!selectedProject || selectedProject.projectId === null || !processFlowId) {
            return;
        }

        const history = await performanceService.getHistory(selectedProject.projectId, processFlowId);
        const validHistory = history.filter((item) => {
            if (!item.createdAt) {
                return false;
            }
            const date = new Date(item.createdAt);
            return !Number.isNaN(date.getTime()) && date.getFullYear() !== 1970;
        });
        setHistoryData(validHistory);
    }, [selectedProject, processFlowId]);

    useEffect(() => {
        if (selectedProject) {
            fetchProcessFlows();
            fetchEnvironments();
        }
    }, [selectedProject, fetchProcessFlows, fetchEnvironments]);

    useEffect(() => {
        if (previousProcessFlowId.current !== processFlowId && showHistory) {
            previousProcessFlowId.current = processFlowId;
            setShowHistory(false);
            setHistoryData([]);
            setCompareSelection([]);
        } else {
            previousProcessFlowId.current = processFlowId;
        }
    }, [processFlowId, showHistory]);

    const handleRunTest = async () => {
        if (!selectedProject || selectedProject.projectId === null || !processFlowId || !environment) {
            setError('Lütfen proje, ortam ve akış seçin');
            return;
        }
        if (threadCount > 500 && !window.confirm('Thread sayısı 500 üzerinde. Devam etmek istiyor musunuz?')) {
            return;
        }
        if (threadCount >= 100 && rampUpPeriod === 0 && !window.confirm('Ramp Up 0 ve thread sayısı yüksek. Devam etmek istiyor musunuz?')) {
            return;
        }

        if (thresholdPreset === 'CUSTOM' && !hasCompleteThresholdConfig(thresholdConfig)) {
            setError('Custom threshold preset icin tum threshold degerlerini girin');
            return;
        }
        const thresholdSummary = `${t('thresholdPreset')}: ${thresholdPreset}, ${t('maxErrorRatePercent')}: ${thresholdConfig.maxErrorRatePercent ?? '-'}, ${t('maxAverageMs')}: ${thresholdConfig.maxAverageMs ?? '-'}, ${t('maxP95Ms')}: ${thresholdConfig.maxP95Ms ?? '-'}, ${t('maxP99Ms')}: ${thresholdConfig.maxP99Ms ?? '-'}, ${t('minThroughputPerSecond')}: ${thresholdConfig.minThroughputPerSecond ?? '-'}`;
        if ((threadCount > 500 || (threadCount >= 100 && rampUpPeriod === 0)) && !window.confirm(thresholdSummary)) {
            return;
        }

        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            const result = await performanceService.runPerformanceTest({
                environment,
                processFlowId,
                projectId: selectedProject.projectId,
                rampUpPeriod,
                threadCount,
                durationSeconds: optionalNumber(durationSeconds),
                loopCount: optionalNumber(loopCount),
                thinkTimeMs: optionalNumber(thinkTimeMs),
                timeoutMs: optionalNumber(timeoutMs),
                environmentBaseUrl: environmentBaseUrl || null,
                thresholdPreset,
                maxErrorRatePercent: thresholdConfig.maxErrorRatePercent ?? null,
                maxAverageMs: thresholdConfig.maxAverageMs ?? null,
                maxP95Ms: thresholdConfig.maxP95Ms ?? null,
                maxP99Ms: thresholdConfig.maxP99Ms ?? null,
                minThroughputPerSecond: thresholdConfig.minThroughputPerSecond ?? null,
            });

            setResults((prev) => [{ ...result, createdAt: result.createdAt ?? new Date().toISOString() }, ...prev]);
            setSuccess('Performans testi başlatıldı');
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: unknown) {
            setError(getErrorMessage(err, 'Test başlatılamadı'));
        } finally {
            setLoading(false);
        }
    };

    const handleViewHistory = async () => {
        if (!selectedProject || selectedProject.projectId === null || !processFlowId) {
            setError('Lütfen proje ve akış seçin');
            return;
        }

        if (showHistory) {
            setShowHistory(false);
            return;
        }

        setShowHistory(true);
        setLoadingHistory(true);

        try {
            await refreshHistory();
        } catch (err: unknown) {
            setError(getErrorMessage(err, 'Geçmiş yüklenemedi'));
        } finally {
            setLoadingHistory(false);
        }
    };

    const handleRunningFinished = useCallback(async (performanceResultId?: number) => {
        if (performanceResultId) {
            setResults((prev) => prev.filter((result) => result.performanceResultId !== performanceResultId));
            setSuccess(`Test #${performanceResultId} tamamlandı`);
            setTimeout(() => setSuccess(null), 3000);
        }
        if (showHistory) {
            await refreshHistory();
        }
    }, [refreshHistory, showHistory]);

    const handleViewDetail = async (item: PerformanceResultDto | PerformanceHistoryItem) => {
        const performanceResultId = item.performanceResultId;
        if (!performanceResultId) {
            setError('Performans sonucu bulunamadı');
            return;
        }

        if ('threadGroup' in item) {
            setSelectedResult(item);
            setSelectedHistoryItem(null);
        } else {
            setSelectedHistoryItem(item);
            setSelectedResult(null);
        }

        setDetailDialogOpen(true);
        setDetailTab(0);
        setThreadFilters(defaultPerformanceThreadFilters);
        setLoadingDetail(true);
        setDetailData(null);
        setAnalysisData(null);

        try {
            const [detail, analysis] = await Promise.all([
                performanceService.getPerformanceDetail(performanceResultId),
                performanceService.getAnalysis(performanceResultId).catch(() => null),
            ]);
            setDetailData(detail);
            setAnalysisData(analysis);
        } catch (err: unknown) {
            setError(getErrorMessage(err, 'Detaylar yüklenemedi'));
        } finally {
            setLoadingDetail(false);
        }
    };

    const handleToggleCompareSelection = (item: PerformanceHistoryItem) => {
        if (!item.performanceResultId) {
            return;
        }
        setCompareSelection((prev) => {
            if (prev.includes(item.performanceResultId as number)) {
                return prev.filter((id) => id !== item.performanceResultId);
            }
            return [...prev, item.performanceResultId as number].slice(-2);
        });
    };

    const handleCompare = async () => {
        if (compareSelection.length !== 2) {
            return;
        }
        setComparisonOpen(true);
        setLoadingComparison(true);
        setComparisonResult(null);
        try {
            const result = await performanceService.compare(compareSelection[0], compareSelection[1]);
            setComparisonResult(result);
        } catch (err: unknown) {
            setError(getErrorMessage(err, 'Karşılaştırma yüklenemedi'));
        } finally {
            setLoadingComparison(false);
        }
    };

    const handleSetBaseline = async (item: PerformanceHistoryItem) => {
        if (!item.performanceResultId) {
            return;
        }
        setSettingBaselineId(item.performanceResultId);
        setError(null);
        setSuccess(null);
        try {
            await performanceService.setBaseline(item.performanceResultId);
            await refreshHistory();
            setSuccess(t('baselineUpdated'));
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: unknown) {
            setError(getErrorMessage(err, t('baselineUpdateFailed')));
        } finally {
            setSettingBaselineId(null);
        }
    };

    const handleSaveValidationNote = async (note: string) => {
        const performanceResultId = detailTitleId;
        if (!performanceResultId) {
            return;
        }
        try {
            const validationChecklist = await performanceService.updateValidationNote(performanceResultId, note);
            setAnalysisData((prev) => prev ? { ...prev, validationChecklist } : prev);
            setSelectedHistoryItem((prev) => prev ? { ...prev, validationChecklist } : prev);
            setSelectedResult((prev) => prev ? { ...prev, validationChecklist } : prev);
            setSuccess(t('validationNoteSaved'));
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: unknown) {
            setError(getErrorMessage(err, t('baselineUpdateFailed')));
        }
    };

    const handleAiReportUpdated = (report: PerformanceAiManagementReport) => {
        setAnalysisData((prev) => prev ? { ...prev, aiManagementReport: report } : prev);
        setSelectedHistoryItem((prev) => prev ? { ...prev, aiManagementReport: report } : prev);
        setSelectedResult((prev) => prev ? { ...prev, aiManagementReport: report } : prev);
    };

    const detailTitleId = selectedHistoryItem?.performanceResultId ?? selectedResult?.performanceResultId;
    const detailSummaries = analysisData?.stepSummaries ?? selectedHistoryItem?.performanceSummaries ?? selectedResult?.performanceSummaries ?? [];
    const detailAnalysis = analysisData?.analysisSummary ?? selectedHistoryItem?.analysisSummary ?? selectedResult?.analysisSummary ?? null;
    const detailThreshold = analysisData?.thresholdResult ?? selectedHistoryItem?.thresholdResult ?? selectedResult?.thresholdResult ?? null;
    const detailErrorAnalysis = analysisData?.errorAnalysis ?? selectedHistoryItem?.errorAnalysis ?? selectedResult?.errorAnalysis ?? null;
    const detailEnvironmentMetrics = analysisData?.environmentMetrics ?? selectedHistoryItem?.environmentMetrics ?? selectedResult?.environmentMetrics ?? null;
    const detailValidationChecklist = analysisData?.validationChecklist ?? selectedHistoryItem?.validationChecklist ?? selectedResult?.validationChecklist ?? null;
    const detailBaselineComparison = analysisData?.baselineComparison ?? selectedHistoryItem?.baselineComparison ?? selectedResult?.baselineComparison ?? null;
    const detailManagementReport = analysisData?.managementReport ?? null;
    const detailInsightReport = analysisData?.insightReport
        ?? selectedHistoryItem?.insightReport
        ?? selectedResult?.insightReport
        ?? null;
    const detailAiManagementReport = analysisData?.aiManagementReport
        ?? selectedHistoryItem?.aiManagementReport
        ?? selectedResult?.aiManagementReport
        ?? null;

    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
                    {t('title')}
                </Typography>

                {error && (
                    <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                        {error}
                    </Alert>
                )}

                {success && (
                    <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
                        {success}
                    </Alert>
                )}

                <Paper sx={{ p: 3, mb: 3 }}>
                    <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                        {t('startNew')}
                    </Typography>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(5, 1fr)' }, gap: 2 }}>
                        <FormControl fullWidth size="small">
                            <InputLabel>{t('environment')}</InputLabel>
                            <Select value={environment || ''} label={t('environment')} onChange={(e) => setEnvironment(e.target.value)}>
                                {environments.length === 0 ? (
                                    <MenuItem disabled>Ortam bulunamadı</MenuItem>
                                ) : (
                                    environments.map((env) => (
                                        <MenuItem key={env.gnlWebSysId} value={env.shortCode}>
                                            {env.shortCode}
                                        </MenuItem>
                                    ))
                                )}
                            </Select>
                        </FormControl>
                        <FormControl fullWidth size="small">
                            <InputLabel>{t('flow')}</InputLabel>
                            <Select value={processFlowId || ''} label={t('flow')} onChange={(e) => setProcessFlowId(Number(e.target.value))}>
                                {processFlows.length === 0 ? (
                                    <MenuItem disabled>Akış bulunamadı</MenuItem>
                                ) : (
                                    processFlows.map((flow) => (
                                        <MenuItem key={flow.processFlowId || 0} value={flow.processFlowId || 0}>
                                            {flow.shortCode}
                                        </MenuItem>
                                    ))
                                )}
                            </Select>
                        </FormControl>
                        <TextField fullWidth size="small" label={t('threadCount')} type="number" value={threadCount} onChange={(e) => setThreadCount(parseInt(e.target.value) || 1)} inputProps={{ min: 1 }} />
                        <TextField fullWidth size="small" label={t('rampUp')} type="number" value={rampUpPeriod} onChange={(e) => setRampUpPeriod(parseInt(e.target.value) || 0)} inputProps={{ min: 0 }} />
                        <TextField fullWidth size="small" label={t('durationSeconds')} type="number" value={durationSeconds} onChange={(e) => setDurationSeconds(e.target.value === '' ? '' : Number(e.target.value))} inputProps={{ min: 0 }} />
                        <TextField fullWidth size="small" label={t('loopCount')} type="number" value={loopCount} onChange={(e) => setLoopCount(e.target.value === '' ? '' : Number(e.target.value))} inputProps={{ min: 1 }} />
                        <TextField fullWidth size="small" label={t('thinkTimeMs')} type="number" value={thinkTimeMs} onChange={(e) => setThinkTimeMs(e.target.value === '' ? '' : Number(e.target.value))} inputProps={{ min: 0 }} />
                        <TextField fullWidth size="small" label={t('timeoutMs')} type="number" value={timeoutMs} onChange={(e) => setTimeoutMs(e.target.value === '' ? '' : Number(e.target.value))} inputProps={{ min: 0 }} />
                        <TextField fullWidth size="small" label="Environment Base URL" value={environmentBaseUrl} onChange={(e) => setEnvironmentBaseUrl(e.target.value)} />
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Typography variant="body2" color="text.secondary">
                                {t('estimatedSamples')}: {estimatedSamples ?? '-'}
                            </Typography>
                        </Box>
                        <Button
                            fullWidth
                            variant="contained"
                            startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <PlayArrowIcon />}
                            onClick={handleRunTest}
                            disabled={loading || !selectedProject || !processFlowId || !environment}
                            sx={{ height: '40px' }}
                        >
                            {t('start')}
                        </Button>
                        <Button
                            fullWidth
                            variant={showHistory ? 'contained' : 'outlined'}
                            startIcon={<HistoryIcon />}
                            onClick={handleViewHistory}
                            disabled={!selectedProject || !processFlowId}
                            sx={{ height: '40px' }}
                        >
                            {showHistory ? t('hide') : t('history')}
                        </Button>
                    </Box>
                    <Box sx={{ mt: 2 }}>
                        <PerformanceThresholdPresetFields
                            thresholdPreset={thresholdPreset}
                            thresholdConfig={thresholdConfig}
                            onPresetChange={setThresholdPreset}
                            onThresholdConfigChange={setThresholdConfig}
                        />
                    </Box>
                </Paper>

                {runningItems.length > 0 && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
                        {runningItems.map((item) => (
                            <PerformanceLiveMonitor
                                key={item.performanceResultId}
                                item={item}
                                onFinished={() => handleRunningFinished(item.performanceResultId)}
                            />
                        ))}
                    </Box>
                )}

                {showHistory && (
                    <Box sx={{ mb: 3 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2, gap: 2 }}>
                            <Typography variant="h6" sx={{ fontWeight: 600 }}>
                                {t('performanceHistory')}
                            </Typography>
                            <Button variant="outlined" onClick={handleCompare} disabled={compareSelection.length !== 2}>
                                {t('compare')}
                            </Button>
                        </Box>

                        {loadingHistory ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                                <CircularProgress />
                            </Box>
                        ) : historyData.length > 0 ? (
                            <Paper sx={{ p: 3 }}>
                                <PerformanceRunSummaryTable
                                    items={historyData}
                                    onViewDetail={handleViewDetail}
                                    selectedIds={compareSelection}
                                    onToggleCompareSelection={handleToggleCompareSelection}
                                    onSetBaseline={handleSetBaseline}
                                    settingBaselineId={settingBaselineId}
                                />
                            </Paper>
                        ) : (
                            <Alert severity="info">
                                {t('noHistory')}
                            </Alert>
                        )}
                    </Box>
                )}

                <Dialog open={detailDialogOpen} onClose={() => setDetailDialogOpen(false)} maxWidth="xl" fullWidth>
                    <DialogTitle>
                        {t('testDetails')} - #{detailTitleId}
                    </DialogTitle>
                    <DialogContent>
                        {loadingDetail ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                                <CircularProgress />
                            </Box>
                        ) : (
                            <Box sx={{ mt: 2 }}>
                                <Tabs value={detailTab} onChange={(_, value) => setDetailTab(value)} sx={{ mb: 2 }}>
                                    <Tab label={t('report')} />
                                    <Tab label={t('analysis')} />
                                    <Tab label={t('validation')} />
                                    <Tab label={t('stepSummary')} />
                                    <Tab label={t('threadDetail')} />
                                    <Tab label={t('charts')} />
                                    <Tab label={t('errorAnalysis')} />
                                    <Tab label={t('export')} />
                                </Tabs>
                                {detailTab === 0 && (
                                    <PerformanceManagementReportPanel
                                        report={detailManagementReport}
                                        insightReport={detailInsightReport}
                                        aiReport={detailAiManagementReport}
                                        baselineComparison={detailBaselineComparison}
                                        performanceResultId={detailTitleId}
                                        onAiReportUpdated={handleAiReportUpdated}
                                        onSuccess={setSuccess}
                                        onError={setError}
                                    />
                                )}
                                {detailTab === 1 && (
                                    <PerformanceAnalysisPanel
                                        analysis={detailAnalysis}
                                        thresholdResult={detailThreshold}
                                        errorAnalysis={detailErrorAnalysis}
                                        environmentMetrics={detailEnvironmentMetrics}
                                    />
                                )}
                                {detailTab === 2 && (
                                    <PerformanceValidationChecklistPanel
                                        checklist={detailValidationChecklist}
                                        baselineComparison={detailBaselineComparison}
                                        performanceResultId={detailTitleId}
                                        onSaveNote={handleSaveValidationNote}
                                    />
                                )}
                                {detailTab === 3 && (
                                    <PerformanceStepSummaryTable summaries={detailSummaries} thresholdResult={detailThreshold} analysis={detailAnalysis} />
                                )}
                                {detailTab === 4 && (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        <PerformanceThreadFilters filters={threadFilters} onChange={setThreadFilters} />
                                        <PerformanceThreadDetailTable detail={detailData} filters={threadFilters} />
                                    </Box>
                                )}
                                {detailTab === 5 && (
                                    <PerformanceChartsPanel detail={detailData} summaries={detailSummaries} />
                                )}
                                {detailTab === 6 && (
                                    <PerformanceErrorAnalysisPanel errorAnalysis={detailErrorAnalysis} />
                                )}
                                {detailTab === 7 && (
                                    <PerformanceExportActions
                                        performanceResultId={detailTitleId}
                                        onSuccess={setSuccess}
                                        onError={setError}
                                    />
                                )}
                            </Box>
                        )}
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setDetailDialogOpen(false)}>{t('hide')}</Button>
                    </DialogActions>
                </Dialog>

                <PerformanceComparisonDialog
                    open={comparisonOpen}
                    loading={loadingComparison}
                    result={comparisonResult}
                    onClose={() => setComparisonOpen(false)}
                />

                <FloatingChat
                    title={t('assistantTitle')}
                    subtitle={t('assistantSubtitle')}
                    suggestions={[
                        t('assistantSuggestionSlowest'),
                        t('assistantSuggestionTrend'),
                        t('assistantSuggestionOptimize'),
                    ]}
                    position="bottom-right"
                    bottomOffset={96}
                    projectShortCode={selectedProject?.shortCode}
                    systemShortCode={environment || undefined}
                />
            </Box>
        </DashboardLayout>
    );
}
