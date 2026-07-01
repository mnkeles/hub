'use client';

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslations } from 'next-intl';
import {
    Alert,
    Box,
    Breadcrumbs,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Divider,
    Link,
    MenuItem,
    Paper,
    Step,
    StepLabel,
    Stepper,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import LinkIcon from '@mui/icons-material/Link';
import PersonIcon from '@mui/icons-material/Person';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import StopIcon from '@mui/icons-material/Stop';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import DashboardLayout from '@/components/DashboardLayout';
import CodeEditor from '@/components/CodeEditor';
import HarCreateApiDialog from '@/components/har/HarCreateApiDialog';
import HarLogicalStepReviewCard from '@/components/har/HarLogicalStepReviewCard';
import { useProject } from '@/contexts/ProjectContext';
import { useStreamingChat } from '@/hooks/useStreamingChat';
import { apiInformationService } from '@/services/apiInformationService';
import { harAnalysisService } from '@/services/harAnalysisService';
import { processFlowService } from '@/services/processFlowService';
import { processFlowStepService } from '@/services/processFlowStepService';
import { ApiInformationDto, ProcessFlowDto, ProcessFlowStepDto, ProcessFlowStepParmDto } from '@/types/api';
import {
    HarAnalysisResponse,
    HarAnalysisStatistics,
    HarApiInformationMatch,
    HarReviewedChatContext,
    HarLogicalStep,
    HarReviewedContinuationBlocker,
    HarReviewedContinuationContext,
    HarReviewedContinuationStep,
    HarReviewedDraft,
    HarReviewedDraftStep,
} from '@/types/harAnalysis';
import { useRouter } from 'next/navigation';

const HAR_REVIEW_SESSION_KEY = 'apihub.har-review-draft';
const MAX_HAR_FILE_SIZE_BYTES = 100 * 1024 * 1024;

type InputMode = 'file' | 'text';

const getErrorMessage = (error: unknown, fallbackMessage: string): string => {
    const maybeError = error as {
        response?: {
            data?: {
                message?: string;
                error?: string;
            } | string;
            statusText?: string;
        };
        message?: string;
    };

    const responseData = maybeError?.response?.data;

    if (typeof responseData === 'string' && responseData.trim()) {
        return responseData;
    }

    if (responseData && typeof responseData === 'object') {
        if (typeof responseData.message === 'string' && responseData.message.trim()) {
            return responseData.message;
        }

        if (typeof responseData.error === 'string' && responseData.error.trim()) {
            return responseData.error;
        }
    }

    if (typeof maybeError?.response?.statusText === 'string' && maybeError.response.statusText.trim()) {
        return maybeError.response.statusText;
    }

    if (typeof maybeError?.message === 'string' && maybeError.message.trim()) {
        return maybeError.message;
    }

    return fallbackMessage;
};

const getApiId = (api?: Partial<ApiInformationDto> | null): number | null => {
    if (!api) {
        return null;
    }

    if (typeof api.gnlApiInformationId === 'number' && Number.isFinite(api.gnlApiInformationId)) {
        return api.gnlApiInformationId;
    }

    if (typeof api.id === 'number' && Number.isFinite(api.id)) {
        return api.id;
    }

    return null;
};

const extractNumericId = (value: unknown): number | null => {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
    }

    if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) {
        return Number(value);
    }

    if (!value || typeof value !== 'object') {
        return null;
    }

    const candidates = [
        (value as Record<string, unknown>).data,
        (value as Record<string, unknown>).processFlowId,
        (value as Record<string, unknown>).id,
    ];

    for (const candidate of candidates) {
        const parsed = extractNumericId(candidate);
        if (parsed !== null) {
            return parsed;
        }
    }

    return null;
};

const buildProcessFlowStepParameters = (step: HarReviewedDraftStep): ProcessFlowStepParmDto[] => {
    const parameters: ProcessFlowStepParmDto[] = [];

    for (const parameter of step.processFlowStepParmList || []) {
        const shortCode = parameter.shortCode?.trim();
        if (!shortCode) {
            continue;
        }

        parameters.push({
            processFlowStepParmId: null,
            shortCode,
            value: parameter.value,
            valExpression: parameter.valExpression,
            paramOrder: parameter.paramOrder || parameters.length + 1,
            useContext: parameter.useContext ?? false,
            sql: parameter.sql,
            code: parameter.code,
        });
    }

    return parameters;
};

const findMatchingApi = (
    apis: ApiInformationDto[],
    projectId: number,
    shortCode: string,
    apiShortCode: string,
    servicePath: string
): ApiInformationDto | null => {
    const normalizedShortCode = shortCode.toLowerCase();
    const normalizedApiShortCode = apiShortCode.toLowerCase();
    const normalizedServicePath = servicePath.toLowerCase();

    const matchingApis = apis
        .filter((api) => api.projectId === projectId)
        .filter((api) => {
            const sameShortCode =
                (api.shortCode || '').toLowerCase() === normalizedShortCode ||
                (api.apiShortCode || '').toLowerCase() === normalizedApiShortCode;
            const samePath = (api.srvcName || '').toLowerCase() === normalizedServicePath;
            return sameShortCode && samePath;
        })
        .sort((left, right) => (right.gnlApiInformationId || right.id || 0) - (left.gnlApiInformationId || left.id || 0));

    return matchingApis[0] || null;
};

const mergeCandidateMatches = (step: HarLogicalStep): HarApiInformationMatch[] => {
    const combined = [
        ...(step.matchedApiInformation ? [step.matchedApiInformation] : []),
        ...(step.candidateApiInformation || []),
    ];

    return combined.filter((candidate, index, source) => {
        const candidateId = getApiId(candidate);
        if (candidateId === null) {
            const candidateKey = `${candidate.name || ''}-${candidate.apiShortCode || candidate.shortCode || ''}-${candidate.srvcName || candidate.url || ''}`;
            return index === source.findIndex((item) => {
                const itemKey = `${item.name || ''}-${item.apiShortCode || item.shortCode || ''}-${item.srvcName || item.url || ''}`;
                return itemKey === candidateKey;
            });
        }

        return index === source.findIndex((item) => getApiId(item) === candidateId);
    });
};

const getStepPath = (step: HarReviewedDraftStep): string | undefined => {
    return step.path || step.originalAnalysisReference.normalizedPath || step.originalAnalysisReference.path || step.originalAnalysisReference.url;
};

const resolveSelectedApiInformation = (
    step: HarReviewedDraftStep,
    availableApis: ApiInformationDto[]
): Partial<ApiInformationDto> | null => {
    if (!step.selectedApiInformationId) {
        return null;
    }

    const candidates: Partial<ApiInformationDto>[] = [
        ...availableApis,
        ...(step.matchedApiInformation ? [step.matchedApiInformation] : []),
        ...step.candidateApiInformation,
    ];

    return candidates.find((candidate) => getApiId(candidate) === step.selectedApiInformationId) || null;
};

const buildContinuationStep = (
    step: HarReviewedDraftStep,
    availableApis: ApiInformationDto[]
): HarReviewedContinuationStep => {
    const selectedApiInformation = resolveSelectedApiInformation(step, availableApis);

    return {
        analysisStepOrder: step.analysisStepOrder,
        included: step.included,
        stepShortCode: step.stepShortCode,
        stepType: step.stepType,
        method: step.method,
        path: getStepPath(step),
        servicePathTemplate: step.servicePathTemplate,
        statusCode: step.statusCode,
        responseTime: step.responseTime,
        dependsOnStepOrders: step.dependsOnStepOrders,
        sourceRequestOrders: step.sourceRequestOrders,
        selectedApiInformationId: step.selectedApiInformationId,
        selectedApiShortCode:
            selectedApiInformation?.apiShortCode ||
            selectedApiInformation?.shortCode ||
            step.matchedApiShortCode,
        selectedApiName: selectedApiInformation?.name || null,
        unresolved: step.included && !step.selectedApiInformationId,
    };
};

const buildReviewedDraft = (analysis: HarAnalysisResponse): HarReviewedDraft => {
    const draftStepsByOrder = new Map((analysis.processFlowDraft?.steps || []).map((step) => [step.stepOrder, step]));

    const steps: HarReviewedDraftStep[] = (analysis.logicalSteps || []).map((logicalStep) => {
        const draftStep = draftStepsByOrder.get(logicalStep.stepOrder);
        const selectedApiInformationId =
            draftStep?.gnlApiInformationId ||
            getApiId(logicalStep.matchedApiInformation) ||
            null;

        return {
            analysisStepOrder: logicalStep.stepOrder,
            included: true,
            selectedApiInformationId,
            stepShortCode: draftStep?.stepShortCode || logicalStep.stepShortCode || `STEP_${logicalStep.stepOrder}`,
            plIn: draftStep?.plIn || logicalStep.plIn || logicalStep.apiInformationDraft?.plIn || '',
            preHeader: draftStep?.preHeader || logicalStep.preHeader || '',
            headerExtractor: draftStep?.headerExtractor || logicalStep.headerExtractor || '',
            parameterExtractor: draftStep?.parameterExtractor || logicalStep.parameterExtractor || '',
            stepType: draftStep?.stepType || logicalStep.stepType,
            method: draftStep?.method || logicalStep.method,
            path: draftStep?.path || logicalStep.path || logicalStep.normalizedPath || logicalStep.url,
            servicePathTemplate: draftStep?.servicePathTemplate || logicalStep.servicePathTemplate || logicalStep.apiInformationDraft?.servicePathTemplate,
            statusCode: logicalStep.statusCode,
            responseTime: logicalStep.responseTime,
            dependsOnStepOrders: draftStep?.dependsOnStepOrders || logicalStep.dependsOnStepOrders || [],
            sourceRequestOrders: logicalStep.sourceRequestOrders || [],
            requestPayloadTemplate: draftStep?.requestPayloadTemplate || logicalStep.requestPayloadTemplate,
            requestSample: draftStep?.requestSample || logicalStep.requestSample,
            responseSample: draftStep?.responseSample || logicalStep.responseSample,
            processFlowStepParmList: draftStep?.processFlowStepParmList || logicalStep.processFlowStepParmList || [],
            matchedApiShortCode: draftStep?.matchedApiShortCode,
            candidateApiInformation: mergeCandidateMatches(logicalStep),
            matchedApiInformation: logicalStep.matchedApiInformation || null,
            apiInformationDraft: draftStep?.apiInformationDraft || logicalStep.apiInformationDraft || null,
            originalAnalysisReference: logicalStep,
        };
    });

    return {
        flowShortCode:
            analysis.processFlowDraft?.shortCode ||
            analysis.flowShortCodeSuggestion ||
            `${analysis.projectShortCode || 'PROJECT'}_${analysis.systemShortCode || 'SYS'}_FLOW`,
        projectId: analysis.processFlowDraft?.projectId ?? analysis.projectId ?? null,
        projectShortCode: analysis.processFlowDraft?.projectShortCode || analysis.projectShortCode,
        systemShortCode: analysis.processFlowDraft?.systemShortCode || analysis.systemShortCode,
        isActive: analysis.processFlowDraft?.isActive ?? true,
        steps,
    };
};

export default function ImportHarPage() {
    const router = useRouter();
    const t = useTranslations('harImport');
    const chatUiT = useTranslations('chatUi');
    const flowsT = useTranslations('flows');
    const { selectedProject } = useProject();
    const {
        messages: chatMessages,
        currentAiMessage,
        currentAiReasoning,
        currentAiUsage,
        isStreaming,
        streamActivity,
        error: chatError,
        sendMessage,
        cancelStream,
        clearMessages,
    } = useStreamingChat();
    const chatMessagesEndRef = useRef<HTMLDivElement>(null);
    const [inputMode, setInputMode] = useState<InputMode>('file');
    const [projectShortCode, setProjectShortCode] = useState('');
    const [systemShortCode, setSystemShortCode] = useState('');
    const [harFile, setHarFile] = useState<File | null>(null);
    const [harContent, setHarContent] = useState('');
    const [analysis, setAnalysis] = useState<HarAnalysisResponse | null>(null);
    const [reviewedDraft, setReviewedDraft] = useState<HarReviewedDraft | null>(null);
    const [availableApis, setAvailableApis] = useState<ApiInformationDto[]>([]);
    const [activeWizardStep, setActiveWizardStep] = useState(0);
    const [reviewTab, setReviewTab] = useState(0);
    const [loadingApis, setLoadingApis] = useState(false);
    const [analyzing, setAnalyzing] = useState(false);
    const [savingFlow, setSavingFlow] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [restoredDraftNotice, setRestoredDraftNotice] = useState<string | null>(null);
    const [createApiTargetStep, setCreateApiTargetStep] = useState<HarReviewedDraftStep | null>(null);
    const [continuationMessage, setContinuationMessage] = useState('');

    const availableSystems = selectedProject?.generalWebSystemDtoList || [];
    const wizardSteps = [
        t('steps.uploadAnalyze'),
        t('steps.reviewAnalysis'),
        t('steps.continue'),
    ];

    useEffect(() => {
        if (!projectShortCode.trim() && selectedProject?.shortCode) {
            setProjectShortCode(selectedProject.shortCode);
        }

        if (!systemShortCode.trim() && availableSystems.length === 1) {
            setSystemShortCode(availableSystems[0].shortCode);
        }
    }, [availableSystems, projectShortCode, selectedProject, systemShortCode]);

    useEffect(() => {
        if (typeof window === 'undefined') {
            return;
        }

        const rawDraft = sessionStorage.getItem(HAR_REVIEW_SESSION_KEY);
        if (!rawDraft) {
            return;
        }

        try {
            const parsed = JSON.parse(rawDraft) as {
                analysis?: HarAnalysisResponse;
                reviewedDraft?: HarReviewedDraft;
                projectShortCode?: string;
                systemShortCode?: string;
                activeWizardStep?: number;
                reviewTab?: number;
                continuationMessage?: string;
            };

            if (parsed.analysis && parsed.reviewedDraft) {
                setAnalysis(parsed.analysis);
                setReviewedDraft(parsed.reviewedDraft);
                setProjectShortCode(parsed.projectShortCode || parsed.analysis.projectShortCode || '');
                setSystemShortCode(parsed.systemShortCode || parsed.analysis.systemShortCode || '');
                setActiveWizardStep(Math.min(parsed.activeWizardStep || 1, 2));
                setReviewTab(parsed.reviewTab || 0);
                setContinuationMessage(parsed.continuationMessage || '');
                setRestoredDraftNotice(t('messages.restoredDraftNotice'));
            }
        } catch {
            sessionStorage.removeItem(HAR_REVIEW_SESSION_KEY);
        }
    }, [t]);

    useEffect(() => {
        if (typeof window === 'undefined' || !analysis || !reviewedDraft) {
            return;
        }

        sessionStorage.setItem(
            HAR_REVIEW_SESSION_KEY,
            JSON.stringify({
                analysis,
                reviewedDraft,
                projectShortCode,
                systemShortCode,
                activeWizardStep,
                reviewTab,
                continuationMessage,
            })
        );
    }, [activeWizardStep, analysis, continuationMessage, projectShortCode, reviewTab, reviewedDraft, systemShortCode]);

    const loadProjectApis = useCallback(async (targetProjectShortCode: string, targetProjectId?: number | null) => {
        if (!targetProjectShortCode.trim()) {
            setAvailableApis([]);
            return;
        }

        try {
            setLoadingApis(true);
            const projectApis = await apiInformationService.getByProject(targetProjectShortCode.trim());
            setAvailableApis(projectApis);
        } catch {
            try {
                const allApis = await apiInformationService.getAll();
                const filteredApis = targetProjectId
                    ? allApis.filter((api) => api.projectId === targetProjectId)
                    : allApis;
                setAvailableApis(filteredApis);
            } catch {
                setAvailableApis([]);
                setError(t('messages.apiOptionsLoadFailed'));
            }
        } finally {
            setLoadingApis(false);
        }
    }, [t]);

    useEffect(() => {
        const analysisProjectShortCode = analysis?.projectShortCode;
        if (analysisProjectShortCode) {
            loadProjectApis(analysisProjectShortCode, analysis?.projectId);
        }
    }, [analysis?.projectId, analysis?.projectShortCode, loadProjectApis]);

    const includedSteps = useMemo(() => {
        return reviewedDraft?.steps.filter((step) => step.included) || [];
    }, [reviewedDraft]);

    const dependencyIssues = useMemo(() => {
        if (!reviewedDraft) {
            return {} as Record<number, string>;
        }

        const includedStepOrders = new Set(reviewedDraft.steps.filter((step) => step.included).map((step) => step.analysisStepOrder));

        return reviewedDraft.steps.reduce<Record<number, string>>((accumulator, step) => {
            if (!step.included) {
                return accumulator;
            }

            const missingDependencies = step.dependsOnStepOrders.filter((dependency) => !includedStepOrders.has(dependency));
            if (missingDependencies.length > 0) {
                accumulator[step.analysisStepOrder] = t('messages.excludedDependencies', { dependencies: missingDependencies.join(', ') });
            }

            return accumulator;
        }, {});
    }, [reviewedDraft, t]);

    const duplicateStepCodes = useMemo(() => {
        const normalizedCounts = new Map<string, number>();
        for (const step of includedSteps) {
            const normalizedCode = step.stepShortCode.trim().toLowerCase();
            if (!normalizedCode) {
                continue;
            }
            normalizedCounts.set(normalizedCode, (normalizedCounts.get(normalizedCode) || 0) + 1);
        }

        return new Set(
            Array.from(normalizedCounts.entries())
                .filter(([, count]) => count > 1)
                .map(([code]) => code)
        );
    }, [includedSteps]);

    const unresolvedIncludedSteps = includedSteps.filter((step) => !step.selectedApiInformationId);
    const blankShortCodeSteps = includedSteps.filter((step) => !step.stepShortCode.trim());
    const dependencyBlockedSteps = includedSteps.filter((step) => dependencyIssues[step.analysisStepOrder]);
    const duplicateShortCodeSteps = includedSteps.filter((step) => duplicateStepCodes.has(step.stepShortCode.trim().toLowerCase()));
    const hasBlankFlowShortCode = !reviewedDraft?.flowShortCode.trim();
    const hasBlankSystemShortCode = !reviewedDraft?.systemShortCode.trim();
    const continuationBlockers = useMemo<HarReviewedContinuationBlocker[]>(() => {
        if (!reviewedDraft) {
            return [];
        }

        const blockers: HarReviewedContinuationBlocker[] = [];
        const includedStepOrders = new Set(includedSteps.map((step) => step.analysisStepOrder));

        if (includedSteps.length === 0) {
            blockers.push({ reason: 'no_included_steps' });
        }

        if (!reviewedDraft.flowShortCode.trim()) {
            blockers.push({ reason: 'missing_flow_short_code' });
        }

        if (!reviewedDraft.systemShortCode.trim()) {
            blockers.push({ reason: 'missing_system_short_code' });
        }

        for (const step of includedSteps) {
            if (!step.selectedApiInformationId) {
                blockers.push({
                    analysisStepOrder: step.analysisStepOrder,
                    stepShortCode: step.stepShortCode || undefined,
                    reason: 'missing_api_mapping',
                });
            }

            if (!step.stepShortCode.trim()) {
                blockers.push({
                    analysisStepOrder: step.analysisStepOrder,
                    reason: 'missing_step_short_code',
                });
            }

            const missingDependencies = step.dependsOnStepOrders.filter((dependency) => !includedStepOrders.has(dependency));
            if (missingDependencies.length > 0) {
                blockers.push({
                    analysisStepOrder: step.analysisStepOrder,
                    stepShortCode: step.stepShortCode || undefined,
                    reason: `excluded_dependencies:${missingDependencies.join(',')}`,
                });
            }

            const normalizedCode = step.stepShortCode.trim().toLowerCase();
            if (normalizedCode && duplicateStepCodes.has(normalizedCode)) {
                blockers.push({
                    analysisStepOrder: step.analysisStepOrder,
                    stepShortCode: step.stepShortCode,
                    reason: 'duplicate_step_short_code',
                });
            }
        }

        return blockers;
    }, [duplicateStepCodes, includedSteps, reviewedDraft]);

    const flowCreationBlockers = continuationBlockers.filter((blocker) => blocker.reason !== 'missing_api_mapping');

    const reviewedHarContext = useMemo<HarReviewedContinuationContext | null>(() => {
        if (!analysis || !reviewedDraft) {
            return null;
        }

        const summarizedSteps = reviewedDraft.steps.map((step) => buildContinuationStep(step, availableApis));
        const includedSummaries = summarizedSteps.filter((step) => step.included);
        const excludedSummaries = summarizedSteps.filter((step) => !step.included);

        return {
            projectShortCode: reviewedDraft.projectShortCode.trim() || analysis.projectShortCode,
            systemShortCode: reviewedDraft.systemShortCode.trim() || analysis.systemShortCode,
            analysisSummary: {
                projectId: analysis.projectId,
                projectName: analysis.projectName,
                flowShortCodeSuggestion: analysis.flowShortCodeSuggestion,
                summary: analysis.summary,
                baseUrls: analysis.baseUrls,
                recommendations: analysis.recommendations,
                warnings: analysis.warnings,
                statistics: analysis.statistics,
                endpointCount: analysis.endpoints?.length || 0,
                logicalStepCount: analysis.logicalSteps?.length || 0,
                relationshipCount: analysis.relationships?.length || 0,
                ignoredRequestCount: analysis.ignoredRequests?.length || 0,
            },
            reviewedDraft: {
                flowShortCode: reviewedDraft.flowShortCode,
                projectId: reviewedDraft.projectId,
                isActive: reviewedDraft.isActive,
            },
            includedSteps: includedSummaries,
            excludedSteps: excludedSummaries,
            selectedApiMappings: includedSummaries
                .filter((step) => step.selectedApiInformationId !== null)
                .map((step) => ({
                    analysisStepOrder: step.analysisStepOrder,
                    stepShortCode: step.stepShortCode,
                    selectedApiInformationId: step.selectedApiInformationId!,
                    selectedApiShortCode: step.selectedApiShortCode,
                    selectedApiName: step.selectedApiName,
                    method: step.method,
                    path: step.path,
                })),
            unresolvedSteps: includedSummaries.filter((step) => step.unresolved),
            blockers: continuationBlockers,
        };
    }, [analysis, availableApis, continuationBlockers, reviewedDraft]);

    const displayedChatMessages = useMemo(() => {
        return chatMessages.filter((message) => message.role !== 'system');
    }, [chatMessages]);

    useEffect(() => {
        chatMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [displayedChatMessages, currentAiMessage, currentAiReasoning, currentAiUsage, streamActivity]);

    const renderReasoningBlock = (
        reasoning?: string | null,
        placeholderText?: string | null
    ) => {
        const normalizedReasoning = reasoning?.trim() || '';
        const displayText = normalizedReasoning || placeholderText || '';
        const sectionTitle = normalizedReasoning ? chatUiT('labels.thinking') : chatUiT('labels.processing');

        if (!displayText) {
            return null;
        }

        return (
            <Paper
                variant="outlined"
                sx={{
                    mt: 1,
                    p: 1.25,
                    backgroundColor: 'rgba(255, 152, 0, 0.08)',
                    borderStyle: 'dashed',
                    borderColor: 'warning.main',
                }}
            >
                <Typography variant="caption" sx={{ fontWeight: 700, color: 'warning.dark', display: 'block', mb: 0.75 }}>
                    {sectionTitle}
                </Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', color: 'text.secondary', lineHeight: 1.5 }}>
                    {displayText}
                </Typography>
            </Paper>
        );
    };

    const renderUsageFooter = (usage?: { promptTokens?: number | null; completionTokens?: number | null; totalTokens?: number | null } | null) => {
        const hasUsage = !!usage && (
            usage.promptTokens != null ||
            usage.completionTokens != null ||
            usage.totalTokens != null
        );

        if (!hasUsage) {
            return null;
        }

        return (
            <Typography variant="caption" sx={{ display: 'block', mt: 0.75, color: 'text.secondary' }}>
                {chatUiT('labels.tokens')}: P {usage?.promptTokens ?? '-'} | C {usage?.completionTokens ?? '-'} | T {usage?.totalTokens ?? '-'}
            </Typography>
        );
    };

    const getStreamActivityLabel = () => {
        switch (streamActivity) {
            case 'submitting':
                return chatUiT('activity.submitting');
            case 'waiting':
                return chatUiT('activity.waiting');
            case 'thinking':
                return chatUiT('activity.thinking');
            case 'responding':
                return chatUiT('activity.responding');
            default:
                return null;
        }
    };

    const overviewStats = useMemo(() => {
        const logicalStepCount = reviewedDraft?.steps.length || analysis?.logicalSteps?.length || 0;
        const ignoredRequestCount = analysis?.ignoredRequests?.length || 0;
        const matchedCount = reviewedDraft?.steps.filter((step) => !!step.selectedApiInformationId).length || 0;
        const unmatchedCount = reviewedDraft?.steps.filter((step) => !step.selectedApiInformationId).length || 0;
        const includedCount = includedSteps.length;
        const statistics: HarAnalysisStatistics = analysis?.statistics || {};

        return [
            { label: t('stats.totalRequests'), value: statistics.totalRequests ?? logicalStepCount + ignoredRequestCount },
            { label: t('stats.ignoredRequests'), value: statistics.ignoredRequests ?? ignoredRequestCount },
            { label: t('stats.logicalSteps'), value: statistics.logicalSteps ?? logicalStepCount },
            { label: t('stats.matchedSteps'), value: statistics.matchedSteps ?? matchedCount },
            { label: t('stats.unmatchedSteps'), value: statistics.unmatchedSteps ?? unmatchedCount },
            { label: t('stats.includedSteps'), value: includedCount },
        ];
    }, [analysis?.ignoredRequests?.length, analysis?.statistics, includedSteps.length, reviewedDraft, t]);
    const showStreamingAssistantBubble = isStreaming && streamActivity !== 'idle';
    const streamingThinkingPlaceholder = !currentAiReasoning && !currentAiMessage ? getStreamActivityLabel() : null;

    const handleResetWorkflow = () => {
        setHarFile(null);
        setHarContent('');
        setAnalysis(null);
        setReviewedDraft(null);
        setActiveWizardStep(0);
        setReviewTab(0);
        setAvailableApis([]);
        setCreateApiTargetStep(null);
        setContinuationMessage('');
        setSavingFlow(false);
        setError(null);
        setSuccess(null);
        setRestoredDraftNotice(null);
        clearMessages();
        if (typeof window !== 'undefined') {
            sessionStorage.removeItem(HAR_REVIEW_SESSION_KEY);
        }
    };

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const nextFile = event.target.files?.[0] || null;
        if (!nextFile) {
            setHarFile(null);
            return;
        }

        const lowerName = nextFile.name.toLowerCase();
        const validType = lowerName.endsWith('.har') || lowerName.endsWith('.json') || nextFile.type.includes('json');
        if (!validType) {
            setError(t('messages.invalidHarFile'));
            setHarFile(null);
            return;
        }

        if (nextFile.size > MAX_HAR_FILE_SIZE_BYTES) {
            setError(t('messages.harFileTooLarge'));
            setHarFile(null);
            return;
        }

        setError(null);
        setHarFile(nextFile);
    };

    const handleStepChange = (stepOrder: number, updates: Partial<HarReviewedDraftStep>) => {
        setReviewedDraft((prev) => {
            if (!prev) {
                return prev;
            }

            return {
                ...prev,
                steps: prev.steps.map((step) => {
                    if (step.analysisStepOrder !== stepOrder) {
                        return step;
                    }

                    return {
                        ...step,
                        ...updates,
                    };
                }),
            };
        });
    };

    const handleAnalyze = async () => {
        if (!projectShortCode.trim()) {
            setError(t('messages.projectShortCodeRequired'));
            return;
        }

        if (!systemShortCode.trim()) {
            setError(t('messages.systemShortCodeRequired'));
            return;
        }

        if (inputMode === 'file' && !harFile) {
            setError(t('messages.harFileRequired'));
            return;
        }

        if (inputMode === 'file' && harFile && harFile.size > MAX_HAR_FILE_SIZE_BYTES) {
            setError(t('messages.harFileTooLarge'));
            return;
        }

        if (inputMode === 'text' && !harContent.trim()) {
            setError(t('messages.harContentRequired'));
            return;
        }

        try {
            setAnalyzing(true);
            setError(null);
            setSuccess(null);

            const response = inputMode === 'file' && harFile
                ? await harAnalysisService.uploadAndAnalyze(harFile, projectShortCode.trim(), systemShortCode.trim())
                : await harAnalysisService.analyzeHarContent(harContent, projectShortCode.trim(), systemShortCode.trim());

            setAnalysis(response);
            setReviewedDraft(buildReviewedDraft(response));
            setActiveWizardStep(1);
            setReviewTab(0);
            setRestoredDraftNotice(null);
            setContinuationMessage('');
            clearMessages();
            setSuccess(t('messages.analysisCompleted'));
        } catch (analysisError) {
            setError(getErrorMessage(analysisError, t('messages.unexpectedError')));
        } finally {
            setAnalyzing(false);
        }
    };

    const handleSaveDraftLocally = () => {
        if (!analysis || !reviewedDraft || typeof window === 'undefined') {
            return;
        }

        sessionStorage.setItem(
            HAR_REVIEW_SESSION_KEY,
            JSON.stringify({
                analysis,
                reviewedDraft,
                projectShortCode,
                systemShortCode,
                activeWizardStep,
                reviewTab,
                continuationMessage,
            })
        );
        setSuccess(t('messages.draftSavedToBrowser'));
    };

    const handleCreateApiCreated = async (api: ApiInformationDto) => {
        const apiId = api.gnlApiInformationId || api.id || null;
        if (createApiTargetStep && apiId) {
            handleStepChange(createApiTargetStep.analysisStepOrder, { selectedApiInformationId: apiId });
        }

        const reviewedProjectShortCode = reviewedDraft?.projectShortCode;
        if (reviewedProjectShortCode) {
            await loadProjectApis(reviewedProjectShortCode, reviewedDraft?.projectId);
        }

        setSuccess(t('messages.apiCreatedLinked', { name: api.apiShortCode || api.shortCode || api.name }));
    };

    const createApiInformationFromStepDraft = async (
        step: HarReviewedDraftStep,
        projectId: number,
        projectShortCodeForLookup: string
    ): Promise<number> => {
        const draft = step.apiInformationDraft;
        const shortCode = draft?.apiShortCode || draft?.shortCode || step.stepShortCode;
        const servicePath = draft?.srvcName || draft?.url || step.servicePathTemplate || step.path || '';

        if (!shortCode.trim() || !servicePath.trim()) {
            throw new Error(t('messages.apiDraftMissingForStep', { step: step.stepShortCode || `#${step.analysisStepOrder}` }));
        }

        const payload: ApiInformationDto = {
            name: draft?.name || shortCode || servicePath,
            shortCode: (draft?.shortCode || shortCode).trim(),
            apiShortCode: shortCode.trim(),
            srvcName: servicePath.trim(),
            httpMethod: draft?.httpMethod || draft?.method || step.method || 'GET',
            active: 'Aktif',
            mediaType: draft?.mediaType || 'application/json',
            projectId,
            statusCode: draft?.statusCode || step.statusCode || 200,
            plIn: draft?.plIn || step.plIn || step.requestPayloadTemplate || '',
            headerParameters: draft?.headerParameters || '',
            headerVal: draft?.headerVal || '',
            grpc: false,
            isTokenApi: false,
            externalApi: false,
            sqlQuery: false,
        };

        const result = await apiInformationService.save(payload);
        if (result && result.success === false) {
            throw new Error(result.message || t('messages.apiCreateFailedForStep', { step: step.stepShortCode }));
        }

        if (projectShortCodeForLookup.trim()) {
            const projectApis = await apiInformationService.getByProject(projectShortCodeForLookup.trim());
            const createdApi = findMatchingApi(projectApis, projectId, payload.shortCode, payload.apiShortCode || payload.shortCode, payload.srvcName);
            const createdApiId = getApiId(createdApi);
            if (createdApiId !== null) {
                return createdApiId;
            }
        }

        const allApis = await apiInformationService.getAll();
        const createdApi = findMatchingApi(allApis, projectId, payload.shortCode, payload.apiShortCode || payload.shortCode, payload.srvcName);
        const createdApiId = getApiId(createdApi);
        if (createdApiId === null) {
            throw new Error(t('messages.apiCreatedButIdResolveFailed', { step: step.stepShortCode }));
        }

        return createdApiId;
    };

    const handleCreateProcessFlowFromHar = async () => {
        if (!analysis || !reviewedDraft) {
            return;
        }

        if (!reviewedDraft.projectId) {
            setError(t('messages.projectResolveFailed'));
            return;
        }

        if (flowCreationBlockers.length > 0) {
            setError(t('messages.resolveBlockersBeforeCreate'));
            return;
        }

        try {
            setSavingFlow(true);
            setError(null);
            setSuccess(null);

            const projectShortCodeForLookup = reviewedDraft.projectShortCode.trim() || analysis.projectShortCode;
            const resolvedApiIdsByStepOrder = new Map<number, number>();
            for (const step of includedSteps) {
                const apiInformationId = step.selectedApiInformationId
                    || await createApiInformationFromStepDraft(step, reviewedDraft.projectId, projectShortCodeForLookup);
                resolvedApiIdsByStepOrder.set(step.analysisStepOrder, apiInformationId);
            }

            const flowPayload: ProcessFlowDto = {
                processFlowId: null,
                shortCode: reviewedDraft.flowShortCode.trim(),
                isActive: reviewedDraft.isActive ? 'Aktif' : 'Pasif',
                systemShortCode: reviewedDraft.systemShortCode.trim(),
                systemShortCodeOAB: reviewedDraft.systemShortCode.trim(),
                projectId: reviewedDraft.projectId,
            };

            const flowResult = await processFlowService.save(flowPayload);
            if (flowResult && flowResult.success === false) {
                throw new Error(flowResult.message || t('messages.processFlowCreateFailed'));
            }

            const processFlowId = extractNumericId(flowResult);
            if (processFlowId === null) {
                throw new Error(t('messages.processFlowIdResolveFailed'));
            }

            for (const [index, step] of includedSteps.entries()) {
                const apiInformationId = resolvedApiIdsByStepOrder.get(step.analysisStepOrder);
                if (!apiInformationId) {
                    throw new Error(t('messages.includedStepsWithoutApiMapping', { steps: step.stepShortCode || `#${step.analysisStepOrder}` }));
                }

                const stepPayload: ProcessFlowStepDto = {
                    processFlowStepId: null,
                    gnlApiInformationId: apiInformationId,
                    processFlowId,
                    stepOrder: index + 1,
                    stepShortCode: step.stepShortCode.trim(),
                    plIn: step.plIn || step.requestPayloadTemplate || '',
                    preHeader: step.preHeader || '',
                    headerExtractor: step.headerExtractor || '',
                    parameterExtractor: step.parameterExtractor || '',
                    processFlowStepParmList: buildProcessFlowStepParameters(step),
                };

                const stepResult = await processFlowStepService.save(stepPayload);
                if (stepResult && stepResult.success === false) {
                    throw new Error(stepResult.message || t('messages.processFlowStepCreateFailed', { step: step.stepShortCode }));
                }
            }

            if (typeof window !== 'undefined') {
                sessionStorage.removeItem(HAR_REVIEW_SESSION_KEY);
            }
            setSuccess(t('messages.processFlowCreated', { id: processFlowId }));
            router.push(`/dashboard/process-flows/${processFlowId}/steps`);
        } catch (createError) {
            setError(getErrorMessage(createError, t('messages.processFlowCreateFailed')));
        } finally {
            setSavingFlow(false);
        }
    };

    const handleSendContinuation = async () => {
        if (!analysis || !reviewedDraft || !reviewedHarContext) {
            return;
        }

        const trimmedMessage = continuationMessage.trim();
        if (!trimmedMessage) {
            setError(t('messages.continuationMessageRequired'));
            return;
        }

        setError(null);
        setSuccess(null);

        const context: HarReviewedChatContext = {
            type: 'har-review',
            reviewedHar: reviewedHarContext,
        };

        await sendMessage({
            message: trimmedMessage,
            projectShortCode: reviewedDraft.projectShortCode.trim() || analysis.projectShortCode,
            systemShortCode: reviewedDraft.systemShortCode.trim() || analysis.systemShortCode,
            analysisReferenceId: analysis.analysisReferenceId,
            context,
        });

        setContinuationMessage('');
    };

    const handleContinuationMessageKeyDown: React.KeyboardEventHandler<HTMLDivElement> = (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();

            if (!isStreaming) {
                void handleSendContinuation();
            }
        }
    };

    return (
        <DashboardLayout>
            <Box>
                <Box sx={{ mb: 2 }}>
                    <Button
                        startIcon={<ArrowBackIcon />}
                        onClick={() => router.push('/dashboard/process-flows')}
                        sx={{ mb: 2, textTransform: 'none' }}
                    >
                        {t('actions.backToFlows')}
                    </Button>
                    <Breadcrumbs sx={{ mb: 3 }}>
                        <Link underline="hover" color="inherit" onClick={() => router.push('/dashboard/process-flows')} sx={{ cursor: 'pointer' }}>
                            {flowsT('title')}
                        </Link>
                        <Typography color="text.primary" sx={{ fontWeight: 500 }}>
                            {t('title')}
                        </Typography>
                    </Breadcrumbs>
                </Box>

                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, pb: 2, borderBottom: '2px solid', borderColor: 'divider', gap: 2, flexWrap: 'wrap' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Box sx={{ p: 1.5, borderRadius: 2, backgroundColor: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <UploadFileIcon sx={{ fontSize: 32, color: 'white' }} />
                        </Box>
                        <Box>
                            <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
                                {t('title')}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {t('subtitle')}
                            </Typography>
                        </Box>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
                        <Button variant="outlined" onClick={handleResetWorkflow} sx={{ textTransform: 'none' }}>
                            {t('actions.resetWorkflow')}
                        </Button>
                        <Button variant="contained" onClick={handleSaveDraftLocally} disabled={!analysis || !reviewedDraft} sx={{ textTransform: 'none' }}>
                            {t('actions.saveDraftToBrowser')}
                        </Button>
                    </Box>
                </Box>

                {restoredDraftNotice && (
                    <Alert severity="info" sx={{ mb: 2 }} onClose={() => setRestoredDraftNotice(null)}>
                        {restoredDraftNotice}
                    </Alert>
                )}

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

                <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, borderRadius: 3, mb: 3 }}>
                    <Stepper activeStep={activeWizardStep} alternativeLabel>
                        {wizardSteps.map((label) => (
                            <Step key={label}>
                                <StepLabel>{label}</StepLabel>
                            </Step>
                        ))}
                    </Stepper>
                </Paper>

                {activeWizardStep === 0 && (
                    <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, borderRadius: 3 }}>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                            <Tabs value={inputMode} onChange={(_, value: InputMode) => setInputMode(value)}>
                                <Tab value="file" label={t('tabs.harFileUpload')} />
                                <Tab value="text" label={t('tabs.rawHarContent')} />
                            </Tabs>

                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 2 }}>
                                <TextField
                                    label={t('fields.projectShortCode')}
                                    value={projectShortCode}
                                    onChange={(event) => setProjectShortCode(event.target.value)}
                                    fullWidth
                                    required
                                />
                                <TextField
                                    label={t('fields.systemShortCode')}
                                    value={systemShortCode}
                                    onChange={(event) => setSystemShortCode(event.target.value)}
                                    fullWidth
                                    required
                                    helperText={availableSystems.length > 0 ? t('helperTexts.systemShortCodeWithSuggestions') : t('helperTexts.systemShortCode')}
                                />
                            </Box>

                            {availableSystems.length > 0 && (
                                <Box>
                                    <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                                        {t('sections.knownSystems')}
                                    </Typography>
                                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                        {availableSystems.map((system) => (
                                            <Chip key={system.gnlWebSysId || system.shortCode} label={system.shortCode} onClick={() => setSystemShortCode(system.shortCode)} color={systemShortCode === system.shortCode ? 'primary' : 'default'} />
                                        ))}
                                    </Box>
                                </Box>
                            )}

                            {inputMode === 'file' ? (
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                                    <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} sx={{ alignSelf: 'flex-start', textTransform: 'none' }}>
                                        {t('actions.selectHarFile')}
                                        <input hidden type="file" accept=".har,.json,application/json" onChange={handleFileChange} />
                                    </Button>
                                    {harFile ? (
                                        <Alert severity={harFile.size > 10 * 1024 * 1024 ? 'warning' : 'info'}>
                                            {t('messages.selectedFile', { name: harFile.name, size: (harFile.size / (1024 * 1024)).toFixed(2) })}
                                        </Alert>
                                    ) : (
                                        <Typography variant="body2" color="text.secondary">
                                            {t('messages.uploadDescription')}
                                        </Typography>
                                    )}
                                </Box>
                            ) : (
                                <Box>
                                    <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                                        {t('fields.harContent')}
                                    </Typography>
                                    <CodeEditor value={harContent} onChange={setHarContent} language="json" height="320px" />
                                </Box>
                            )}

                            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
                                <Alert severity="info" sx={{ flex: 1, minWidth: 260 }}>
                                    {t('messages.backendRequirements')}
                                </Alert>
                                <Button variant="contained" onClick={handleAnalyze} disabled={analyzing} startIcon={analyzing ? <CircularProgress size={18} color="inherit" /> : <PlayArrowIcon />} sx={{ textTransform: 'none', minWidth: 180 }}>
                                    {analyzing ? t('actions.analyzing') : t('actions.analyzeHar')}
                                </Button>
                            </Box>
                        </Box>
                    </Paper>
                )}

                {activeWizardStep === 1 && analysis && reviewedDraft && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, borderRadius: 3 }}>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 2 }}>
                                <TextField label={t('fields.flowShortCode')} value={reviewedDraft.flowShortCode} onChange={(event) => setReviewedDraft((prev) => prev ? { ...prev, flowShortCode: event.target.value } : prev)} fullWidth />
                                <TextField label={t('fields.projectShortCode')} value={reviewedDraft.projectShortCode} disabled fullWidth />
                                <TextField label={t('fields.systemShortCode')} value={reviewedDraft.systemShortCode} onChange={(event) => setReviewedDraft((prev) => prev ? { ...prev, systemShortCode: event.target.value } : prev)} fullWidth />
                                <TextField
                                    label={t('fields.flowStatus')}
                                    value={reviewedDraft.isActive ? 'active' : 'passive'}
                                    onChange={(event) => setReviewedDraft((prev) => prev ? { ...prev, isActive: event.target.value === 'active' } : prev)}
                                    select
                                    fullWidth
                                >
                                    <MenuItem value="active">{flowsT('active')}</MenuItem>
                                    <MenuItem value="passive">{flowsT('passive')}</MenuItem>
                                </TextField>
                            </Box>
                        </Paper>

                        <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                            <Tabs value={reviewTab} onChange={(_, value) => setReviewTab(value)} variant="scrollable" scrollButtons="auto">
                                <Tab label={t('tabs.overview')} />
                                <Tab label={t('tabs.logicalSteps', { count: reviewedDraft.steps.length })} />
                                <Tab label={t('tabs.relationships', { count: analysis.relationships?.length || 0 })} />
                                <Tab label={t('tabs.ignoredRequests', { count: analysis.ignoredRequests?.length || 0 })} />
                                <Tab label={t('tabs.rawJson')} />
                            </Tabs>
                            <Divider />
                            <Box sx={{ p: { xs: 2, md: 3 } }}>
                                {reviewTab === 0 && (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                                        <Card variant="outlined">
                                            <CardContent>
                                                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                                                    {t('sections.summary')}
                                                </Typography>
                                                <Typography variant="body1">
                                                    {analysis.summary || t('messages.noSummary')}
                                                </Typography>
                                            </CardContent>
                                        </Card>

                                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: 'repeat(3, minmax(0, 1fr))' }, gap: 2 }}>
                                            {overviewStats.map((stat) => (
                                                <Card key={stat.label} variant="outlined">
                                                    <CardContent>
                                                        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                                                            {stat.label}
                                                        </Typography>
                                                        <Typography variant="h5" sx={{ fontWeight: 700 }}>
                                                            {stat.value}
                                                        </Typography>
                                                    </CardContent>
                                                </Card>
                                            ))}
                                        </Box>

                                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }, gap: 2 }}>
                                            <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                                <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>
                                                    {t('sections.warnings')}
                                                </Typography>
                                                {analysis.warnings?.length ? analysis.warnings.map((warning, index) => <Alert key={`${warning}-${index}`} severity="warning" sx={{ mb: 1 }}>{warning}</Alert>) : <Typography variant="body2" color="text.secondary">{t('messages.noWarnings')}</Typography>}
                                            </Paper>
                                            <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                                <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>
                                                    {t('sections.recommendations')}
                                                </Typography>
                                                {analysis.recommendations?.length ? analysis.recommendations.map((recommendation, index) => <Alert key={`${recommendation}-${index}`} severity="success" sx={{ mb: 1 }}>{recommendation}</Alert>) : <Typography variant="body2" color="text.secondary">{t('messages.noRecommendations')}</Typography>}
                                            </Paper>
                                        </Box>

                                        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>
                                                {t('sections.baseUrls')}
                                            </Typography>
                                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                                {analysis.baseUrls?.length ? analysis.baseUrls.map((baseUrl) => <Chip key={baseUrl} icon={<LinkIcon />} label={baseUrl} variant="outlined" />) : <Typography variant="body2" color="text.secondary">{t('messages.noBaseUrls')}</Typography>}
                                            </Box>
                                        </Paper>
                                    </Box>
                                )}

                                {reviewTab === 1 && (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        {loadingApis && <Alert severity="info">{t('messages.loadingApiOptions')}</Alert>}
                                        {reviewedDraft.steps.length === 0 ? (
                                            <Alert severity="warning">{t('messages.noLogicalSteps')}</Alert>
                                        ) : (
                                            reviewedDraft.steps.map((step) => (
                                                <HarLogicalStepReviewCard
                                                    key={step.analysisStepOrder}
                                                    step={step}
                                                    allSteps={reviewedDraft.steps}
                                                    apiOptions={availableApis}
                                                    dependencyIssue={dependencyIssues[step.analysisStepOrder] || null}
                                                    duplicateShortCode={duplicateStepCodes.has(step.stepShortCode.trim().toLowerCase())}
                                                    onStepChange={handleStepChange}
                                                    onOpenCreateApi={(targetStep) => setCreateApiTargetStep(targetStep)}
                                                />
                                            ))
                                        )}
                                    </Box>
                                )}

                                {reviewTab === 2 && (
                                    <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                                        <TableContainer>
                                            <Table>
                                                <TableHead>
                                                    <TableRow>
                                                        <TableCell>{t('tables.relationships.sourceStep')}</TableCell>
                                                        <TableCell>{t('tables.relationships.targetStep')}</TableCell>
                                                        <TableCell>{t('tables.relationships.sourceType')}</TableCell>
                                                        <TableCell>{t('tables.relationships.sourceKeyPath')}</TableCell>
                                                        <TableCell>{t('tables.relationships.targetType')}</TableCell>
                                                        <TableCell>{t('tables.relationships.targetKeyPath')}</TableCell>
                                                        <TableCell>{t('tables.relationships.confidence')}</TableCell>
                                                    </TableRow>
                                                </TableHead>
                                                <TableBody>
                                                    {analysis.relationships?.length ? analysis.relationships.map((relationship, index) => (
                                                        <TableRow key={`${relationship.sourceStepOrder || relationship.fromStepOrder}-${relationship.targetStepOrder || relationship.toStepOrder}-${index}`}>
                                                            <TableCell>{relationship.sourceStepOrder || relationship.fromStepOrder ? `#${relationship.sourceStepOrder || relationship.fromStepOrder}` : '-'}</TableCell>
                                                            <TableCell>{relationship.targetStepOrder || relationship.toStepOrder ? `#${relationship.targetStepOrder || relationship.toStepOrder}` : '-'}</TableCell>
                                                            <TableCell>{relationship.sourceType || '-'}</TableCell>
                                                            <TableCell>{relationship.sourceKey || relationship.sourcePath || '-'}</TableCell>
                                                            <TableCell>{relationship.targetType || '-'}</TableCell>
                                                            <TableCell>{relationship.targetKey || relationship.targetPath || '-'}</TableCell>
                                                            <TableCell>{relationship.confidence ?? '-'}</TableCell>
                                                        </TableRow>
                                                    )) : (
                                                        <TableRow>
                                                            <TableCell colSpan={7} align="center">{t('messages.noRelationships')}</TableCell>
                                                        </TableRow>
                                                    )}
                                                </TableBody>
                                            </Table>
                                        </TableContainer>
                                    </Paper>
                                )}

                                {reviewTab === 3 && (
                                    <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                                        <TableContainer>
                                            <Table>
                                                <TableHead>
                                                    <TableRow>
                                                        <TableCell>{t('tables.ignoredRequests.requestOrder')}</TableCell>
                                                        <TableCell>{t('tables.ignoredRequests.method')}</TableCell>
                                                        <TableCell>{t('tables.ignoredRequests.pathUrl')}</TableCell>
                                                        <TableCell>{t('tables.ignoredRequests.reason')}</TableCell>
                                                    </TableRow>
                                                </TableHead>
                                                <TableBody>
                                                    {analysis.ignoredRequests?.length ? analysis.ignoredRequests.map((request, index) => (
                                                        <TableRow key={`${request.requestOrder || index}-${request.reason || 'ignored'}`}>
                                                            <TableCell>{request.requestOrder ?? '-'}</TableCell>
                                                            <TableCell>{request.method || '-'}</TableCell>
                                                            <TableCell sx={{ wordBreak: 'break-all' }}>{request.path || request.url || '-'}</TableCell>
                                                            <TableCell>{request.reason || '-'}</TableCell>
                                                        </TableRow>
                                                    )) : (
                                                        <TableRow>
                                                            <TableCell colSpan={4} align="center">{t('messages.noIgnoredRequests')}</TableCell>
                                                        </TableRow>
                                                    )}
                                                </TableBody>
                                            </Table>
                                        </TableContainer>
                                    </Paper>
                                )}

                                {reviewTab === 4 && (
                                    <Box>
                                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                                            {t('messages.rawJsonDescription')}
                                        </Typography>
                                        <CodeEditor value={JSON.stringify(analysis, null, 2)} onChange={() => undefined} language="json" height="520px" readOnly />
                                    </Box>
                                )}
                            </Box>
                        </Paper>

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
                            <Button variant="outlined" onClick={() => setActiveWizardStep(0)} sx={{ textTransform: 'none' }}>
                                {t('actions.backToAnalysisInputs')}
                            </Button>
                            <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
                                <Button
                                    variant="contained"
                                    color="success"
                                    onClick={handleCreateProcessFlowFromHar}
                                    disabled={savingFlow || flowCreationBlockers.length > 0}
                                    startIcon={savingFlow ? <CircularProgress size={18} color="inherit" /> : <PlayArrowIcon />}
                                    sx={{ textTransform: 'none' }}
                                >
                                    {savingFlow ? t('actions.creatingProcessFlow') : t('actions.createProcessFlow')}
                                </Button>
                                <Button variant="contained" onClick={() => setActiveWizardStep(2)} disabled={!analysis || !reviewedDraft || savingFlow} sx={{ textTransform: 'none' }}>
                                    {t('actions.continueToChat')}
                                </Button>
                            </Box>
                        </Box>
                    </Box>
                )}

                {activeWizardStep === 2 && analysis && reviewedDraft && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, borderRadius: 3 }}>
                            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                {t('sections.continueWithChat')}
                            </Typography>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' }, gap: 2 }}>
                                <TextField label={t('fields.flowShortCode')} value={reviewedDraft.flowShortCode} onChange={(event) => setReviewedDraft((prev) => prev ? { ...prev, flowShortCode: event.target.value } : prev)} fullWidth />
                                <TextField label={t('fields.systemShortCode')} value={reviewedDraft.systemShortCode} onChange={(event) => setReviewedDraft((prev) => prev ? { ...prev, systemShortCode: event.target.value } : prev)} fullWidth />
                                <TextField label={t('fields.projectShortCode')} value={reviewedDraft.projectShortCode} disabled fullWidth />
                                <TextField
                                    label={t('fields.flowStatus')}
                                    value={reviewedDraft.isActive ? 'active' : 'passive'}
                                    onChange={(event) => setReviewedDraft((prev) => prev ? { ...prev, isActive: event.target.value === 'active' } : prev)}
                                    select
                                    fullWidth
                                >
                                    <MenuItem value="active">{flowsT('active')}</MenuItem>
                                    <MenuItem value="passive">{flowsT('passive')}</MenuItem>
                                </TextField>
                                <TextField label={t('fields.includedStepCount')} value={includedSteps.length} disabled fullWidth />
                                <TextField label={t('fields.unresolvedStepCount')} value={unresolvedIncludedSteps.length} disabled fullWidth />
                            </Box>
                        </Paper>

                        <Alert severity="info">
                            {t('messages.continueUsesChatContext')}
                        </Alert>

                        {continuationBlockers.length > 0 ? (
                            <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>
                                    {t('sections.blockers')}
                                </Typography>
                                {includedSteps.length === 0 && (
                                    <Alert severity="warning" sx={{ mb: 1.5 }}>
                                        {t('messages.includeAtLeastOneStep')}
                                    </Alert>
                                )}
                                {hasBlankFlowShortCode && (
                                    <Alert severity="warning" sx={{ mb: 1.5 }}>
                                        {t('messages.flowShortCodeRequired')}
                                    </Alert>
                                )}
                                {hasBlankSystemShortCode && (
                                    <Alert severity="warning" sx={{ mb: 1.5 }}>
                                        {t('messages.systemShortCodeRequired')}
                                    </Alert>
                                )}
                                {unresolvedIncludedSteps.length > 0 && (
                                    <Alert severity="warning" sx={{ mb: 1.5 }} icon={<WarningAmberIcon />}>
                                        {t('messages.includedStepsWithoutApiMapping', { steps: unresolvedIncludedSteps.map((step) => step.stepShortCode || `#${step.analysisStepOrder}`).join(', ') })}
                                    </Alert>
                                )}
                                {blankShortCodeSteps.length > 0 && (
                                    <Alert severity="warning" sx={{ mb: 1.5 }}>
                                        {t('messages.blankShortCodeSteps', { steps: blankShortCodeSteps.map((step) => `#${step.analysisStepOrder}`).join(', ') })}
                                    </Alert>
                                )}
                                {dependencyBlockedSteps.length > 0 && (
                                    <Alert severity="error" sx={{ mb: 1.5 }}>
                                        {t('messages.excludedDependencySteps', { steps: dependencyBlockedSteps.map((step) => step.stepShortCode || `#${step.analysisStepOrder}`).join(', ') })}
                                    </Alert>
                                )}
                                {duplicateShortCodeSteps.length > 0 && (
                                    <Alert severity="error">
                                        {t('messages.duplicateIncludedStepCodes', { steps: Array.from(new Set(duplicateShortCodeSteps.map((step) => step.stepShortCode.trim()).filter(Boolean))).join(', ') })}
                                    </Alert>
                                )}
                            </Paper>
                        ) : (
                            <Alert severity="success">
                                {t('messages.noContinuationBlockers')}
                            </Alert>
                        )}

                        <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                            <Box sx={{ px: 3, py: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
                                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                    {t('sections.reviewedContextSummary')}
                                </Typography>
                            </Box>
                            <TableContainer>
                                <Table>
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>{t('tables.finalReview.finalOrder')}</TableCell>
                                            <TableCell>{t('tables.finalReview.step')}</TableCell>
                                            <TableCell>{t('tables.finalReview.apiInformationId')}</TableCell>
                                            <TableCell>{t('tables.finalReview.method')}</TableCell>
                                            <TableCell>{t('tables.finalReview.path')}</TableCell>
                                            <TableCell>{t('tables.finalReview.status')}</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {includedSteps.length > 0 ? includedSteps.map((step, index) => (
                                            <TableRow key={step.analysisStepOrder}>
                                                <TableCell>{index + 1}</TableCell>
                                                <TableCell>{step.stepShortCode}</TableCell>
                                                <TableCell>{step.selectedApiInformationId || '-'}</TableCell>
                                                <TableCell>{step.method || '-'}</TableCell>
                                                <TableCell sx={{ wordBreak: 'break-all' }}>{step.path || '-'}</TableCell>
                                                <TableCell>{step.selectedApiInformationId ? t('status.mapped') : t('status.needsReview')}</TableCell>
                                            </TableRow>
                                        )) : (
                                            <TableRow>
                                                <TableCell colSpan={6} align="center">{t('messages.noIncludedSteps')}</TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Paper>

                        <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, borderRadius: 3 }}>
                            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>
                                {t('sections.chatConversation')}
                            </Typography>

                            {chatError && (
                                <Alert severity="error" sx={{ mb: 2 }}>
                                    {chatError}
                                </Alert>
                            )}

                            <Box
                                sx={{
                                    p: 2,
                                    mb: 2,
                                    minHeight: 220,
                                    maxHeight: 420,
                                    overflow: 'auto',
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: 2,
                                    backgroundColor: 'grey.50',
                                }}
                            >
                                {displayedChatMessages.length === 0 && !showStreamingAssistantBubble ? (
                                    <Typography variant="body2" color="text.secondary">
                                        {t('messages.chatConversationEmpty')}
                                    </Typography>
                                ) : (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        {displayedChatMessages.map((message, index) => (
                                            <Box
                                                key={`${message.role}-${index}-${message.content.slice(0, 20)}`}
                                                sx={{
                                                    display: 'flex',
                                                    alignItems: 'flex-start',
                                                    gap: 1.5,
                                                    justifyContent: message.role === 'user' ? 'flex-end' : 'flex-start',
                                                }}
                                            >
                                                {message.role === 'assistant' && (
                                                    <SmartToyIcon sx={{ mt: 0.5, color: 'primary.main' }} />
                                                )}
                                                <Paper
                                                    variant="outlined"
                                                    sx={{
                                                        p: 1.5,
                                                        maxWidth: '80%',
                                                        backgroundColor: message.role === 'user' ? 'primary.main' : 'background.paper',
                                                        color: message.role === 'user' ? 'primary.contrastText' : 'text.primary',
                                                    }}
                                                >
                                                    <Box sx={{ whiteSpace: 'pre-wrap' }}>
                                                        {message.role === 'assistant' && renderReasoningBlock(message.reasoning)}
                                                        {message.content && (
                                                            <Typography variant="body2">{message.content}</Typography>
                                                        )}
                                                        {message.role === 'assistant' && renderUsageFooter({
                                                            promptTokens: message.promptTokens,
                                                            completionTokens: message.completionTokens,
                                                            totalTokens: message.totalTokens,
                                                        })}
                                                    </Box>
                                                </Paper>
                                                {message.role === 'user' && (
                                                    <PersonIcon sx={{ mt: 0.5, color: 'secondary.main' }} />
                                                )}
                                            </Box>
                                        ))}

                                        {showStreamingAssistantBubble && (
                                            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
                                                <SmartToyIcon sx={{ mt: 0.5, color: 'primary.main' }} />
                                                <Paper variant="outlined" sx={{ p: 1.5, maxWidth: '80%' }}>
                                                    <Box sx={{ whiteSpace: 'pre-wrap' }}>
                                                        {renderReasoningBlock(currentAiReasoning, streamingThinkingPlaceholder)}
                                                        {currentAiMessage && (
                                                            <Typography variant="body2">{currentAiMessage}</Typography>
                                                        )}
                                                        {renderUsageFooter(currentAiUsage)}
                                                    </Box>
                                                </Paper>
                                            </Box>
                                        )}
                                        <div ref={chatMessagesEndRef} />
                                    </Box>
                                )}
                            </Box>

                            <TextField
                                label={t('fields.continuationMessage')}
                                placeholder={t('messages.chatPromptHelper')}
                                value={continuationMessage}
                                onChange={(event) => setContinuationMessage(event.target.value)}
                                onKeyDown={handleContinuationMessageKeyDown}
                                fullWidth
                                multiline
                                minRows={3}
                                disabled={isStreaming}
                            />

                            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap', mt: 2 }}>
                                <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
                                    <Button variant="outlined" onClick={() => setActiveWizardStep(1)} sx={{ textTransform: 'none' }}>
                                        {t('actions.backToReview')}
                                    </Button>
                                    <Button variant="outlined" onClick={handleSaveDraftLocally} sx={{ textTransform: 'none' }}>
                                        {t('actions.saveReviewDraft')}
                                    </Button>
                                </Box>

                                <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
                                    {isStreaming && (
                                        <Button variant="outlined" color="error" onClick={cancelStream} startIcon={<StopIcon />} sx={{ textTransform: 'none' }}>
                                            {t('actions.stopResponse')}
                                        </Button>
                                    )}
                                    <Button
                                        variant="contained"
                                        color="success"
                                        onClick={handleCreateProcessFlowFromHar}
                                        disabled={savingFlow || flowCreationBlockers.length > 0 || isStreaming}
                                        startIcon={savingFlow ? <CircularProgress size={18} color="inherit" /> : <PlayArrowIcon />}
                                        sx={{ textTransform: 'none', minWidth: 210 }}
                                    >
                                        {savingFlow ? t('actions.creatingProcessFlow') : t('actions.createProcessFlow')}
                                    </Button>
                                    <Button
                                        variant="contained"
                                        onClick={handleSendContinuation}
                                        disabled={!reviewedHarContext || !continuationMessage.trim() || isStreaming || savingFlow}
                                        startIcon={isStreaming ? <CircularProgress size={18} color="inherit" /> : <SendIcon />}
                                        sx={{ textTransform: 'none', minWidth: 210 }}
                                    >
                                        {isStreaming ? t('actions.sendingToChat') : t('actions.sendToChat')}
                                    </Button>
                                </Box>
                            </Box>
                        </Paper>
                    </Box>
                )}

                <HarCreateApiDialog
                    open={!!createApiTargetStep}
                    draft={createApiTargetStep?.apiInformationDraft || null}
                    projectId={reviewedDraft?.projectId || analysis?.projectId || selectedProject?.projectId || null}
                    projectShortCode={reviewedDraft?.projectShortCode || analysis?.projectShortCode || projectShortCode}
                    defaultMethod={createApiTargetStep?.method}
                    defaultPath={createApiTargetStep?.path}
                    stepShortCode={createApiTargetStep?.stepShortCode || ''}
                    onClose={() => setCreateApiTargetStep(null)}
                    onCreated={handleCreateApiCreated}
                />
            </Box>
        </DashboardLayout>
    );
}
