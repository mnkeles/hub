export type PerformanceStatus =
    | 'RUNNING'
    | 'COMPLETED'
    | 'FAILED'
    | 'STOPPING'
    | 'COMPLETED_PASSED'
    | 'COMPLETED_FAILED'
    | 'STOPPED'
    | 'ERROR';

export type PerformanceThresholdPreset = 'SMOKE' | 'NORMAL' | 'STRESS' | 'STRICT_SLA' | 'CUSTOM';

export type PerformanceValidationStatus = 'PASSED' | 'FAILED' | 'WARNING' | 'NOT_APPLICABLE';

export type PerformanceAnomalyLevel = 'NORMAL' | 'WATCH' | 'ANOMALOUS' | 'CRITICAL';

export type PerformanceBottleneckType = 'NONE' | 'ERROR' | 'LATENCY' | 'THROUGHPUT' | 'INSTABILITY' | 'ENVIRONMENT' | 'MIXED';

export type PerformanceReleaseReadiness = 'READY' | 'CONDITIONAL' | 'BLOCKED' | 'UNKNOWN';

export type PerformanceInsightSeverity = 'INFO' | 'WARNING' | 'HIGH' | 'CRITICAL';

export interface PerformanceRequest {
    environment: string;
    processFlowId: number;
    projectId: number;
    rampUpPeriod: number;
    threadCount: number;
    durationSeconds?: number | null;
    loopCount?: number | null;
    thinkTimeMs?: number | null;
    timeoutMs?: number | null;
    testDataId?: number | null;
    environmentBaseUrl?: string | null;
    thresholdPreset?: PerformanceThresholdPreset | null;
    maxErrorRatePercent?: number | null;
    maxAverageMs?: number | null;
    maxP95Ms?: number | null;
    maxP99Ms?: number | null;
    minThroughputPerSecond?: number | null;
}

export interface PerformanceStepResult {
    performanceItemStatus: PerformanceStatus;
    elapsedTime: number;
    errorMessage: string | null;
    stepName?: string | null;
    threadNumber?: number;
    startedAt?: string | null;
    finishedAt?: string | null;
}

export interface ThreadGroup {
    threadNumber: number;
    steps: PerformanceStepResult[];
}

export interface PerformanceThreadGroup {
    groups: ThreadGroup[];
}

export interface PerformanceDetailResponse {
    groups: ThreadGroup[];
}

export interface PerformanceResponseTimeBuckets {
    under500ms?: number;
    from500msTo1s?: number;
    from1sTo3s?: number;
    over3s?: number;
}

export interface PerformanceRunSummary {
    status?: PerformanceStatus;
    startedAt?: string | null;
    completedAt?: string | null;
    totalDurationMs?: number;
    threadCount?: number;
    rampUpPeriod?: number;
    totalSamples?: number;
    successfulSamples?: number;
    failedSamples?: number;
    errorRate?: number;
    throughputPerSecond?: number;
    averageElapsedTime?: number;
    minElapsedTime?: number;
    maxElapsedTime?: number;
    p50ElapsedTime?: number;
    p90ElapsedTime?: number;
    p95ElapsedTime?: number;
    p99ElapsedTime?: number;
    slowestStepName?: string | null;
}

export interface PerformanceStepSummary {
    stepName: string;
    maxElapsedTime: number;
    minElapsedTime: number;
    averageElapsedTime: number;
    sampleCount?: number;
    successCount?: number;
    failureCount?: number;
    errorRate?: number;
    throughputPerSecond?: number;
    medianElapsedTime?: number;
    p90ElapsedTime?: number;
    p95ElapsedTime?: number;
    p99ElapsedTime?: number;
    standardDeviation?: number;
    lastError?: string | null;
    responseTimeBuckets?: PerformanceResponseTimeBuckets | null;
}

export interface PerformanceThresholdConfig {
    maxErrorRatePercent?: number;
    maxAverageMs?: number;
    maxP95Ms?: number;
    maxP99Ms?: number;
    minThroughputPerSecond?: number;
}

export interface PerformanceThresholdResult {
    passed?: boolean;
    statusLabel?: string | null;
    reasons?: string[];
    thresholds?: PerformanceThresholdConfig | null;
}

export interface PerformanceAnalysisSummary {
    status?: PerformanceStatus;
    thresholdResult?: PerformanceThresholdResult | null;
    problemStepName?: string | null;
    slowestStepName?: string | null;
    highestP95StepName?: string | null;
    highestP99StepName?: string | null;
    highestErrorStepName?: string | null;
    highestStdDeviationStepName?: string | null;
    summaryText?: string | null;
    warnings?: string[];
}

export type PerformanceManagementRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type PerformanceManagementStepStatus = 'NEEDS_IMPROVEMENT' | 'WATCH' | 'GOOD';

export interface PerformanceManagementSlaItem {
    metric: string;
    passed: boolean;
    expected?: string | null;
    actual?: string | null;
    explanation?: string | null;
}

export interface PerformanceManagementProblemArea {
    title: string;
    stepName?: string | null;
    metric?: string | null;
    value?: string | null;
    impact?: string | null;
}

export interface PerformanceManagementStepAssessment {
    stepName?: string | null;
    status?: PerformanceManagementStepStatus | null;
    priority?: PerformanceManagementRiskLevel | null;
    mainReason?: string | null;
    evidence?: string | null;
    impact?: string | null;
    recommendation?: string | null;
    sampleCount?: number;
    successCount?: number;
    failureCount?: number;
    errorRate?: number;
    averageMs?: number;
    p90Ms?: number;
    p95Ms?: number;
    p99Ms?: number;
    throughputPerSecond?: number;
    lastError?: string | null;
}

export interface PerformanceManagementReport {
    overallStatus?: string | null;
    riskLevel?: PerformanceManagementRiskLevel | null;
    stepAssessmentSummary?: string | null;
    stepAssessments?: PerformanceManagementStepAssessment[];
    executiveSummary?: string | null;
    slaSummary?: PerformanceManagementSlaItem[];
    problemAreas?: PerformanceManagementProblemArea[];
    trendSummary?: string | null;
    recommendedActions?: string[];
    detailExplanation?: string | null;
}

export interface PerformanceMetricInsight {
    metric?: string | null;
    severity?: PerformanceInsightSeverity | null;
    actual?: string | null;
    expected?: string | null;
    explanation?: string | null;
}

export interface PerformanceRootCauseHint {
    severity?: PerformanceInsightSeverity | null;
    category?: string | null;
    signal?: string | null;
    explanation?: string | null;
    recommendation?: string | null;
}

export interface PerformanceStepInsight {
    stepName?: string | null;
    severity?: PerformanceInsightSeverity | null;
    bottleneckType?: PerformanceBottleneckType | null;
    metric?: string | null;
    actual?: string | null;
    expected?: string | null;
    explanation?: string | null;
}

export interface PerformanceInsightReport {
    anomalyScore?: number | null;
    anomalyLevel?: PerformanceAnomalyLevel | null;
    regressionScore?: number | null;
    regressionAvailable?: boolean | null;
    apdexScore?: number | null;
    apdexEstimated?: boolean | null;
    sloCompliancePercent?: number | null;
    bottleneckType?: PerformanceBottleneckType | null;
    releaseReadiness?: PerformanceReleaseReadiness | null;
    rootCauseHints?: PerformanceRootCauseHint[];
    metricInsights?: PerformanceMetricInsight[];
    stepInsights?: PerformanceStepInsight[];
    schemaVersion?: number | null;
    generatedByVersion?: string | null;
}

export interface PerformanceAiActionItem {
    priority?: 'P0' | 'P1' | 'P2' | string | null;
    title?: string | null;
    description?: string | null;
    relatedStepName?: string | null;
    relatedMetric?: string | null;
}

export interface PerformanceAiManagementReport {
    generated?: boolean | null;
    generatedAt?: string | null;
    model?: string | null;
    executiveNarrative?: string | null;
    technicalNarrative?: string | null;
    rootCauseNarrative?: string | null;
    recommendedActionPlan?: PerformanceAiActionItem[];
    releaseReadinessNarrative?: string | null;
    limitations?: string[];
    errorMessage?: string | null;
    schemaVersion?: number | null;
    generatedByVersion?: string | null;
    durationMs?: number | null;
    attemptCount?: number | null;
    failureReason?: string | null;
    validationErrors?: string[];
    promptHash?: string | null;
    inputSummaryHash?: string | null;
    responseSize?: number | null;
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
}

export interface PerformanceErrorTypeCount {
    errorType: string;
    count: number;
}

export interface PerformanceStepErrorCount {
    stepName: string;
    count: number;
}

export interface PerformanceFailedRequest {
    threadNumber?: number | null;
    stepName?: string | null;
    elapsedTime?: number | null;
    errorType?: string | null;
    errorMessage?: string | null;
}

export interface PerformanceErrorAnalysis {
    totalErrorCount?: number;
    errorRate?: number;
    errorsByType?: PerformanceErrorTypeCount[];
    errorsByStep?: PerformanceStepErrorCount[];
    lastError?: string | null;
    failedRequests?: PerformanceFailedRequest[];
}

export interface PerformanceEnvironmentMetrics {
    metricsAvailable?: boolean;
    message?: string | null;
    cpuAvgPercent?: number | null;
    cpuMaxPercent?: number | null;
    memoryAvgPercent?: number | null;
    memoryMaxPercent?: number | null;
    jvmHeapMaxPercent?: number | null;
    gcTimeMs?: number | null;
    dbActiveConnectionMax?: number | null;
    dbConnectionPoolSize?: number | null;
    slowSqlCount?: number | null;
    http5xxCount?: number | null;
    podRestartCount?: number | null;
    warnings?: string[];
}

export interface PerformanceLiveSnapshot {
    performanceResultId?: number;
    status?: PerformanceStatus;
    activeThreadCount?: number;
    totalThreadCount?: number;
    completedSamples?: number;
    successfulSamples?: number;
    failedSamples?: number;
    errorRate?: number;
    throughputPerSecond?: number;
    averageElapsedTime?: number | null;
    p90ElapsedTime?: number | null;
    p95ElapsedTime?: number | null;
    elapsedTimeMs?: number;
    estimatedRemainingMs?: number | null;
    lastCompletedStep?: string | null;
    lastError?: string | null;
    warnings?: string[];
    metricsAvailable?: boolean;
    message?: string | null;
}

export interface PerformanceComparisonMetric {
    metricName: string;
    baseValue?: string | number | boolean | null;
    targetValue?: string | number | boolean | null;
    delta?: string | number | boolean | null;
    direction?: string | null;
    improvement?: boolean | null;
}

export interface PerformanceComparisonResult {
    baseResultId?: number | null;
    targetResultId?: number | null;
    metrics: PerformanceComparisonMetric[];
}

export interface PerformanceValidationChecklistItem {
    key: string;
    label: string;
    status: PerformanceValidationStatus;
    message?: string | null;
}

export interface PerformanceValidationChecklist {
    items?: PerformanceValidationChecklistItem[];
    manualNote?: string | null;
    manualNoteUpdatedAt?: string | null;
}

export interface PerformanceExportPayload {
    resultSchemaVersion?: number | null;
    thresholdPreset?: PerformanceThresholdPreset | null;
    thresholdConfig?: PerformanceThresholdConfig | null;
    baseline?: boolean | null;
    baselineResultId?: number | null;
    baselineComparison?: PerformanceComparisonResult | null;
    validationChecklist?: PerformanceValidationChecklist | null;
    runSummary?: PerformanceRunSummary | null;
    thresholdResult?: PerformanceThresholdResult | null;
    analysisSummary?: PerformanceAnalysisSummary | null;
    errorAnalysis?: PerformanceErrorAnalysis | null;
    environmentMetrics?: PerformanceEnvironmentMetrics | null;
    managementReport?: PerformanceManagementReport | null;
    insightReport?: PerformanceInsightReport | null;
    aiManagementReport?: PerformanceAiManagementReport | null;
    stepSummaries?: PerformanceStepSummary[] | null;
    threadDetail?: PerformanceThreadGroup | null;
}

export interface PerformanceResultDto {
    performanceResultId: number;
    performanceStatus: PerformanceStatus;
    projectId: number;
    processFlowId: number;
    projectDto: unknown | null;
    processFlowDto: unknown | null;
    threadCount: number;
    rampUpPeriod: number;
    threadGroup: PerformanceThreadGroup;
    createdAt?: string;
    runSummary?: PerformanceRunSummary | null;
    performanceSummaries?: PerformanceStepSummary[];
    thresholdResult?: PerformanceThresholdResult | null;
    analysisSummary?: PerformanceAnalysisSummary | null;
    errorAnalysis?: PerformanceErrorAnalysis | null;
    environmentMetrics?: PerformanceEnvironmentMetrics | null;
    resultSchemaVersion?: number | null;
    thresholdPreset?: PerformanceThresholdPreset | null;
    thresholdConfig?: PerformanceThresholdConfig | null;
    baseline?: boolean | null;
    baselineResultId?: number | null;
    baselineComparison?: PerformanceComparisonResult | null;
    validationChecklist?: PerformanceValidationChecklist | null;
    insightReport?: PerformanceInsightReport | null;
    aiManagementReport?: PerformanceAiManagementReport | null;
    durationSeconds?: number | null;
    loopCount?: number | null;
    thinkTimeMs?: number | null;
    timeoutMs?: number | null;
    environmentBaseUrl?: string | null;
}

export interface PerformanceHistoryItem {
    performanceResultId?: number;
    performanceStatus?: PerformanceStatus;
    threadCount?: number;
    rampUpPeriod?: number;
    durationSeconds?: number | null;
    loopCount?: number | null;
    thinkTimeMs?: number | null;
    timeoutMs?: number | null;
    environmentBaseUrl?: string | null;
    runSummary?: PerformanceRunSummary | null;
    thresholdResult?: PerformanceThresholdResult | null;
    analysisSummary?: PerformanceAnalysisSummary | null;
    errorAnalysis?: PerformanceErrorAnalysis | null;
    environmentMetrics?: PerformanceEnvironmentMetrics | null;
    resultSchemaVersion?: number | null;
    thresholdPreset?: PerformanceThresholdPreset | null;
    thresholdConfig?: PerformanceThresholdConfig | null;
    baseline?: boolean | null;
    baselineResultId?: number | null;
    baselineComparison?: PerformanceComparisonResult | null;
    validationChecklist?: PerformanceValidationChecklist | null;
    insightReport?: PerformanceInsightReport | null;
    aiManagementReport?: PerformanceAiManagementReport | null;
    performanceSummaries: PerformanceStepSummary[];
    createdAt: string;
}
