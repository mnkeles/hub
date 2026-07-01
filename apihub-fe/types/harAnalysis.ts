import { ApiInformationDto, ProcessFlowStepParmDto } from '@/types/api';

export interface HarApiInformationMatch extends Partial<ApiInformationDto> {
    score?: number;
    matchScore?: number;
    confidence?: number;
    reasons?: string[];
    explanation?: string;
}

export interface HarApiInformationDraft {
    name?: string;
    shortCode?: string;
    apiShortCode?: string;
    srvcName?: string;
    httpMethod?: string;
    headerParameters?: string;
    statusCode?: number;
    plIn?: string;
    mediaType?: string;
    headerVal?: string;
    description?: string;
    method?: string;
    url?: string;
    servicePathTemplate?: string;
}

export interface HarLogicalStep {
    stepOrder: number;
    stepShortCode?: string;
    stepType?: string;
    method?: string;
    url?: string;
    path?: string;
    normalizedPath?: string;
    servicePathTemplate?: string;
    sourceRequestOrders?: number[];
    plIn?: string;
    preHeader?: string;
    headerExtractor?: string;
    parameterExtractor?: string;
    dependsOnStepOrders?: number[];
    statusCode?: number;
    responseTime?: number;
    requestPayloadTemplate?: string;
    requestSample?: HarPayloadSample;
    responseSample?: HarPayloadSample;
    processFlowStepParmList?: ProcessFlowStepParmDto[];
    matchedApiInformation?: HarApiInformationMatch | null;
    candidateApiInformation?: HarApiInformationMatch[];
    apiInformationDraft?: HarApiInformationDraft | null;
    [key: string]: unknown;
}

export interface HarIgnoredRequest {
    requestOrder?: number;
    method?: string;
    path?: string;
    url?: string;
    reason?: string;
    [key: string]: unknown;
}

export interface HarRelationship {
    sourceStepOrder?: number;
    targetStepOrder?: number;
    fromStepOrder?: number;
    toStepOrder?: number;
    sourceType?: string;
    sourceKey?: string;
    sourcePath?: string;
    targetType?: string;
    targetKey?: string;
    targetPath?: string;
    confidence?: number;
    transitionCount?: number;
    reason?: string;
    [key: string]: unknown;
}

export interface HarProcessFlowDraftStep {
    stepOrder: number;
    stepShortCode?: string;
    stepType?: string;
    gnlApiInformationId?: number | null;
    matchedApiShortCode?: string;
    plIn?: string;
    preHeader?: string;
    headerExtractor?: string;
    parameterExtractor?: string;
    method?: string;
    path?: string;
    servicePathTemplate?: string;
    dependsOnStepOrders?: number[];
    requestPayloadTemplate?: string;
    requestSample?: HarPayloadSample;
    responseSample?: HarPayloadSample;
    processFlowStepParmList?: ProcessFlowStepParmDto[];
    apiInformationDraft?: HarApiInformationDraft | null;
}

export interface HarPayloadSample {
    headers?: Record<string, string>;
    queryParameters?: Record<string, string>;
    body?: string | null;
    bodyTruncated?: boolean;
    sizeBytes?: number | null;
}

export interface HarProcessFlowDraft {
    shortCode?: string;
    projectId?: number | null;
    projectShortCode?: string;
    systemShortCode?: string;
    isActive?: boolean;
    steps?: HarProcessFlowDraftStep[];
}

export interface HarAnalysisStatistics {
    totalRequests?: number;
    ignoredRequests?: number;
    logicalSteps?: number;
    matchedSteps?: number;
    unmatchedSteps?: number;
    [key: string]: string | number | boolean | null | undefined;
}

export interface HarAnalysisResponse {
    analysisReferenceId?: string;
    projectShortCode: string;
    systemShortCode: string;
    projectId?: number | null;
    projectName?: string;
    flowShortCodeSuggestion?: string;
    summary?: string;
    endpoints?: Array<Record<string, unknown>>;
    baseUrls?: string[];
    recommendations?: string[];
    warnings?: string[];
    statistics?: HarAnalysisStatistics;
    ignoredRequests?: HarIgnoredRequest[];
    logicalSteps?: HarLogicalStep[];
    relationships?: HarRelationship[];
    processFlowDraft?: HarProcessFlowDraft;
    [key: string]: unknown;
}

export interface HarReviewedDraftStep {
    analysisStepOrder: number;
    included: boolean;
    selectedApiInformationId: number | null;
    stepShortCode: string;
    plIn: string;
    preHeader: string;
    headerExtractor: string;
    parameterExtractor: string;
    stepType?: string;
    method?: string;
    path?: string;
    servicePathTemplate?: string;
    statusCode?: number;
    responseTime?: number;
    dependsOnStepOrders: number[];
    sourceRequestOrders: number[];
    requestPayloadTemplate?: string;
    requestSample?: HarPayloadSample;
    responseSample?: HarPayloadSample;
    processFlowStepParmList?: ProcessFlowStepParmDto[];
    matchedApiShortCode?: string;
    candidateApiInformation: HarApiInformationMatch[];
    matchedApiInformation?: HarApiInformationMatch | null;
    apiInformationDraft?: HarApiInformationDraft | null;
    originalAnalysisReference: HarLogicalStep;
}

export interface HarReviewedDraft {
    flowShortCode: string;
    projectId: number | null;
    projectShortCode: string;
    systemShortCode: string;
    isActive: boolean;
    steps: HarReviewedDraftStep[];
}

export interface HarReviewedContinuationStep {
    analysisStepOrder: number;
    included: boolean;
    stepShortCode: string;
    stepType?: string;
    method?: string;
    path?: string;
    servicePathTemplate?: string;
    statusCode?: number;
    responseTime?: number;
    dependsOnStepOrders: number[];
    sourceRequestOrders: number[];
    selectedApiInformationId: number | null;
    selectedApiShortCode?: string;
    selectedApiName?: string | null;
    unresolved: boolean;
}

export interface HarReviewedContinuationAnalysisSummary {
    projectId?: number | null;
    projectName?: string;
    flowShortCodeSuggestion?: string;
    summary?: string;
    baseUrls?: string[];
    recommendations?: string[];
    warnings?: string[];
    statistics?: HarAnalysisStatistics;
    endpointCount: number;
    logicalStepCount: number;
    relationshipCount: number;
    ignoredRequestCount: number;
}

export interface HarReviewedContinuationBlocker {
    analysisStepOrder?: number;
    stepShortCode?: string;
    reason: string;
}

export interface HarReviewedContinuationContext {
    projectShortCode: string;
    systemShortCode: string;
    analysisSummary: HarReviewedContinuationAnalysisSummary;
    reviewedDraft: {
        flowShortCode: string;
        projectId: number | null;
        isActive: boolean;
    };
    includedSteps: HarReviewedContinuationStep[];
    excludedSteps: HarReviewedContinuationStep[];
    selectedApiMappings: Array<{
        analysisStepOrder: number;
        stepShortCode: string;
        selectedApiInformationId: number;
        selectedApiShortCode?: string;
        selectedApiName?: string | null;
        method?: string;
        path?: string;
    }>;
    unresolvedSteps: HarReviewedContinuationStep[];
    blockers: HarReviewedContinuationBlocker[];
}

export interface HarReviewedChatContext {
    type: 'har-review';
    reviewedHar: HarReviewedContinuationContext;
}
