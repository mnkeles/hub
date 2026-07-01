# Performance AI Insight Report - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Add deterministic insight scoring and persisted AI narratives to the existing performance `Rapor` tab.

**Architecture:** Backend decisions remain deterministic through a new `PerformanceInsightBuilder`; AI only generates Turkish narrative and action text through a separate `PerformanceAiReportService`. Persisted `insight_report` and `ai_management_report` JSONB snapshots are returned by `/performance/analysis`, then the frontend renders a decision-oriented report UI with safe fallback for legacy or failed-AI results.

**Tech / dependencies:** Spring Boot 3.5, Java 21 records/services, Hibernate JSONB fields, Liquibase, Spring AI OpenAI via existing `OPENAI_API_KEY`, JUnit 5, Next.js 16, React 19, MUI 7, TypeScript, next-intl. No new runtime dependency is required.

**File map:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAnomalyLevel.java` - Insight anomaly enum.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceBottleneckType.java` - Insight bottleneck enum.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceReleaseReadiness.java` - Release readiness enum.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightSeverity.java` - Shared severity enum for insight rows and hints.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceMetricInsight.java` - Deterministic metric insight row.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceRootCauseHint.java` - Deterministic root-cause hint row.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceStepInsight.java` - Deterministic per-step insight row.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightReport.java` - Deterministic insight report DTO.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAiActionItem.java` - AI action plan item DTO.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAiManagementReport.java` - Persisted AI narrative DTO.
- `src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` - Adds persisted insight and AI JSONB fields.
- `src/main/resources/db/changelog/changes/liquibase-migration-file.xml` - Adds `insight_report` and `ai_management_report` columns.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` - Adds insight and AI reports to analysis/export payload.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` - Adds insight and AI reports to mapped result DTO.
- `src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` - Adds insight and AI reports to history summary payload.
- `src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` - Maps new entity fields to DTO.
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` - Returns persisted reports, with legacy fallback.
- `src/main/java/etiya/omniAutomation/service/PerformanceInsightBuilder.java` - Computes deterministic scoring and insight fields.
- `src/test/java/etiya/omniAutomation/service/PerformanceInsightBuilderTest.java` - Unit tests for scoring, bottleneck, and readiness rules.
- `src/main/java/etiya/omniAutomation/service/PerformanceAiClient.java` - Interface for AI completion calls.
- `src/main/java/etiya/omniAutomation/service/SpringPerformanceAiClient.java` - Spring AI implementation of the AI client.
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` - Builds prompt, parses AI JSON, validates fallback.
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` - Unit tests for AI JSON parsing, failure fallback, and contradiction rejection.
- `src/main/java/etiya/omniAutomation/service/PerformanceReportSnapshotService.java` - Builds and persists insight and AI snapshots after a run completes.
- `src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` - Calls snapshot service after baseline and validation data are saved.
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` - Updates export tests for new payload fields.
- `../../apihub-fe/apihub-fe/types/performance.ts` - Frontend TypeScript contract for insight and AI reports.
- `../../apihub-fe/apihub-fe/messages/tr.json` - Turkish labels for new report sections.
- `../../apihub-fe/apihub-fe/messages/en.json` - English labels for new report sections.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceReportDecisionHeader.tsx` - Top decision summary.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceExecutiveSummaryPanel.tsx` - Executive summary with AI fallback.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceTechnicalFindingsPanel.tsx` - Bottleneck, metric, threshold, and regression findings.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceStepRiskPanel.tsx` - Step risk grouping and compact step cards.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceRegressionTrendPanel.tsx` - Baseline trend and regression view.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceRootCauseHintsPanel.tsx` - Deterministic root-cause hints.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx` - AI narrative and limitations section.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceActionPlanPanel.tsx` - P0/P1/P2 action plan.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` - Composes the new report sections while preserving legacy fallback.

---

### TASK-01: Backend Report DTOs And Persistence Contract

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAnomalyLevel.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceBottleneckType.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceReleaseReadiness.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightSeverity.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceMetricInsight.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceRootCauseHint.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceStepInsight.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightReport.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAiActionItem.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAiManagementReport.java` (create)
- `src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` (modify)
- `src/main/resources/db/changelog/changes/liquibase-migration-file.xml` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` (modify)
- `src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` (modify)
- `src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create enums:
  ```java
  public enum PerformanceAnomalyLevel { NORMAL, WATCH, ANOMALOUS, CRITICAL }
  public enum PerformanceBottleneckType { NONE, ERROR, LATENCY, THROUGHPUT, INSTABILITY, ENVIRONMENT, MIXED }
  public enum PerformanceReleaseReadiness { READY, CONDITIONAL, BLOCKED, UNKNOWN }
  public enum PerformanceInsightSeverity { INFO, WARNING, HIGH, CRITICAL }
  ```
- Create records:
  ```java
  public record PerformanceMetricInsight(
          String metric,
          PerformanceInsightSeverity severity,
          String actual,
          String expected,
          String explanation
  ) {}

  public record PerformanceRootCauseHint(
          PerformanceInsightSeverity severity,
          String category,
          String signal,
          String explanation,
          String recommendation
  ) {}

  public record PerformanceStepInsight(
          String stepName,
          PerformanceInsightSeverity severity,
          PerformanceBottleneckType bottleneckType,
          String metric,
          String actual,
          String expected,
          String explanation
  ) {}

  public record PerformanceInsightReport(
          double anomalyScore,
          PerformanceAnomalyLevel anomalyLevel,
          Double regressionScore,
          boolean regressionAvailable,
          Double apdexScore,
          boolean apdexEstimated,
          double sloCompliancePercent,
          PerformanceBottleneckType bottleneckType,
          PerformanceReleaseReadiness releaseReadiness,
          List<PerformanceRootCauseHint> rootCauseHints,
          List<PerformanceMetricInsight> metricInsights,
          List<PerformanceStepInsight> stepInsights
  ) {}

  public record PerformanceAiActionItem(
          String priority,
          String title,
          String description,
          String relatedStepName,
          String relatedMetric
  ) {}

  public record PerformanceAiManagementReport(
          boolean generated,
          Date generatedAt,
          String model,
          String executiveNarrative,
          String technicalNarrative,
          String rootCauseNarrative,
          List<PerformanceAiActionItem> recommendedActionPlan,
          String releaseReadinessNarrative,
          List<String> limitations,
          String errorMessage
  ) {
      public static PerformanceAiManagementReport notGenerated(String errorMessage) {
          return new PerformanceAiManagementReport(false, new Date(), null, null, null, null, List.of(), null, List.of(), errorMessage);
      }
  }
  ```
- Add nullable JSONB columns to `liquibase-migration-file.xml` with unique changeSet IDs:
  - `2026-07-01-001-add-insight-report-to-perf-rslt`
  - `2026-07-01-002-add-ai-management-report-to-perf-rslt`
  Use `preConditions` with `columnExists` and `onFail="MARK_RAN"` like existing performance columns.
- Add JSONB fields to `PerfRsltEntity`:
  ```java
  @Type(JsonBinaryType.class)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "insight_report", columnDefinition = "jsonb")
  private PerformanceInsightReport insightReport;

  @Type(JsonBinaryType.class)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ai_management_report", columnDefinition = "jsonb")
  private PerformanceAiManagementReport aiManagementReport;
  ```
- Extend `PerformanceExportPayload` by adding fields directly after `managementReport`:
  ```java
  PerformanceInsightReport insightReport,
  PerformanceAiManagementReport aiManagementReport,
  ```
- Add matching fields to `PerformanceResultDto` and `PerformanceSummaryResult`.
- Update `PerformanceResultMapper` to map `insightReport` and `aiManagementReport`.
- Update `PerformanceService.toSummaryResult(...)` and any `PerformanceSummaryResult` constructor call to pass the new fields.
- Update `PerformanceExportService.buildPayload(...)` to pass `result.getInsightReport()` and `result.getAiManagementReport()` into `PerformanceExportPayload`. Do not compute insight or AI here in this task.
- Update any `PerformanceExportPayload` constructor usage in tests by passing `null` for both new fields.
- If `ApiCallServiceImpl` or `PerformanceService` uses `PerformanceSummaryResult` constructors, update those constructor calls only for the new fields. Do not add generation logic in this task.

**Done When:**
- New DTOs compile.
- Liquibase contains two idempotent JSONB column changeSets.
- Entity, result DTO, summary result, mapper, and export payload expose `insightReport` and `aiManagementReport`.
- Existing export payload construction compiles with the two new fields set from the entity or `null`.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceExportServiceTest test`. Expected: Maven compiles and the export service tests pass.

---

### TASK-02: Deterministic Insight Builder

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceInsightBuilder.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceInsightBuilderTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- Create `@Service public class PerformanceInsightBuilder`.
- Add this public method:
  ```java
  public PerformanceInsightReport build(
          PerformanceRunSummary runSummary,
          PerformanceThresholdResult thresholdResult,
          PerformanceAnalysisSummary analysisSummary,
          PerformanceErrorAnalysis errorAnalysis,
          PerformanceEnvironmentMetrics environmentMetrics,
          PerformanceComparisonResult baselineComparison,
          List<PerformanceSummary> stepSummaries,
          PerformanceThreadGroup threadDetail
  )
  ```
- Use `PerformanceThresholdConfig.defaults()` when `thresholdResult == null` or `thresholdResult.thresholds() == null`.
- Apdex:
  - Use `threadDetail` samples when present by flattening `groups().steps()` and using `elapsedTime > 0`.
  - `T = thresholds.maxAverageMs()`.
  - satisfied: `elapsed <= T`.
  - tolerating: `elapsed > T && elapsed <= 4 * T`.
  - formula: `(satisfied + tolerating / 2.0) / total`.
  - Set `apdexEstimated=false` when raw samples were used.
  - If no raw sample is available, estimate from `PerformanceSummary.responseTimeBuckets()` and set `apdexEstimated=true`. For `T >= 1000`, count `under500ms` and `from500msTo1s` as satisfied, `from1sTo3s` as tolerating, `over3s` as frustrated.
  - If no sample or bucket data exists, set `apdexScore=null` and `apdexEstimated=false`.
- SLO compliance:
  - Evaluate error rate, average, P95, P99, throughput from `runSummary`.
  - Return `passedCount / 5.0 * 100.0`.
  - If `runSummary == null`, return `0.0`.
- Regression score:
  - If `baselineComparison == null` or metrics are empty, set `regressionAvailable=false` and `regressionScore=null`.
  - Start at `100`.
  - For metrics where `improvement == false`, subtract:
    - `p99`: 25
    - `p95`: 20
    - `averageResponseTime`: 15
    - `errorRate`: 25
    - `throughput`: 15
  - Clamp to `0`.
- Anomaly score:
  - Add `30` when run P99 is greater than `2 * maxP99Ms`.
  - Add `20` when run P95 exceeds `maxP95Ms`.
  - Add `25` when run error rate exceeds `maxErrorRatePercent`.
  - Add `15` when throughput is below `minThroughputPerSecond`.
  - Add `10` when any step has `standardDeviation >= averageElapsedTime * 0.50` and sample count is at least 2.
  - Add `20` when P95, P99, average, error rate, or throughput has regression.
  - Clamp to `100`.
  - Map `0-24 NORMAL`, `25-49 WATCH`, `50-74 ANOMALOUS`, `75-100 CRITICAL`.
- Bottleneck:
  - Build strong signal booleans for error, latency, throughput, instability, and environment.
  - Environment signal is true when `environmentMetrics.warnings()` is non-empty and latency signal is true.
  - If more than one strong signal is true, return `MIXED`, except environment + latency returns `ENVIRONMENT`.
  - Otherwise return the single matching type, or `NONE`.
- Release readiness:
  - `UNKNOWN` when `runSummary == null`, status is `RUNNING`, `STOPPING`, `STOPPED`, or `ERROR`.
  - `BLOCKED` when threshold failed, anomaly level is `CRITICAL`, or regression score is below `70`.
  - `CONDITIONAL` when anomaly level is `WATCH` or `ANOMALOUS`, environment warnings exist, or regression score is below `90`.
  - `READY` when thresholds passed, anomaly is `NORMAL`, and there is no regression below `90`.
- Build `metricInsights` for each failed or near-risk threshold. Near-risk means `>= 80%` of max latency thresholds, `>= 70%` of max error threshold, or `<= 120%` of min throughput threshold.
- Build `stepInsights` from step summaries for steps exceeding thresholds or matching `analysisSummary.problemStepName()`, `slowestStepName()`, or `highestErrorStepName()`.
- Build `rootCauseHints`:
  - Environment metrics unavailable: `INFO`, category `OBSERVABILITY`.
  - DB active connection max close to pool size: `HIGH`, category `DATABASE`, when `dbConnectionPoolSize > 0` and active max is at least 90% of pool size.
  - Slow SQL count greater than 0: `WARNING`, category `DATABASE`.
  - CPU max at least 85: `WARNING`, category `INFRASTRUCTURE`.
  - JVM heap max at least 85 or GC time above 1000 ms: `WARNING`, category `JVM`.
  - HTTP 5xx count greater than 0: `HIGH`, category `APPLICATION`.
- Add tests named:
  - `calculatesApdexFromThreadDetail`
  - `estimatesApdexFromBucketsWhenSamplesAreMissing`
  - `calculatesSloCompliance`
  - `calculatesRegressionScore`
  - `marksRegressionUnavailableWithoutBaseline`
  - `mapsAnomalyLevelBoundaries`
  - `selectsBottleneckType`
  - `calculatesReleaseReadiness`
  - `createsRootCauseHintsFromEnvironmentMetrics`

**Done When:**
- `PerformanceInsightBuilder.build(...)` returns a non-null report for null-safe inputs.
- Builder computes Apdex, SLO compliance, regression score, anomaly score, bottleneck type, release readiness, metric insights, step insights, and root-cause hints.
- Unit tests assert the named scoring and classification behaviors.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceInsightBuilderTest test`. Expected: tests pass.

---

### TASK-03: AI Report Service And Fallback Validation

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceAiClient.java` (create)
- `src/main/java/etiya/omniAutomation/service/SpringPerformanceAiClient.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- Create interface:
  ```java
  public interface PerformanceAiClient {
      String complete(String systemPrompt, String userPrompt);
      String modelName();
  }
  ```
- Create `@Service class SpringPerformanceAiClient implements PerformanceAiClient`.
  - Inject `ChatClient.Builder`.
  - `complete(...)` calls:
    ```java
    return chatClientBuilder.build()
            .prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();
    ```
  - `modelName()` returns `"spring-ai-openai"`.
- Create `@Service class PerformanceAiReportService`.
  - Inject `PerformanceAiClient` and `ObjectMapper`.
  - Add public method:
    ```java
    public PerformanceAiManagementReport generate(
            PerformanceManagementReport managementReport,
            PerformanceInsightReport insightReport,
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison,
            List<PerformanceSummary> stepSummaries
    )
    ```
  - If `insightReport == null`, return `PerformanceAiManagementReport.notGenerated("Insight report is required before AI narrative generation.")`.
  - System prompt requirements:
    - Turkish output.
    - Strict JSON object only.
    - Do not invent metrics.
    - Do not override deterministic `releaseReadiness`, `riskLevel`, or pass/fail.
    - Use cautious root-cause language.
    - Do not mention raw request/response data.
  - User prompt must include compact JSON generated with `ObjectMapper` containing only these fields:
    - `managementReport.overallStatus`
    - `managementReport.riskLevel`
    - `managementReport.stepAssessmentSummary`
    - `managementReport.slaSummary`
    - `insightReport`
    - `runSummary`
    - `thresholdResult.reasons`
    - summarized `analysisSummary`
    - summarized `errorAnalysis`
    - summarized `environmentMetrics`
    - `baselineComparison.metrics`
    - top 10 `stepSummaries` sorted by P99 desc
  - Parse AI JSON into a private DTO with fields:
    ```java
    String executiveNarrative;
    String technicalNarrative;
    String rootCauseNarrative;
    List<PerformanceAiActionItem> recommendedActionPlan;
    String releaseReadinessNarrative;
    List<String> limitations;
    ```
  - Convert parsed DTO to `PerformanceAiManagementReport` with `generated=true`, `generatedAt=new Date()`, and `model=performanceAiClient.modelName()`.
  - Validation:
    - `executiveNarrative`, `technicalNarrative`, and `releaseReadinessNarrative` must be non-blank.
    - `recommendedActionPlan` must contain only priorities `P0`, `P1`, or `P2`; invalid priorities are dropped.
    - If all actions are dropped, use an empty list and keep the report generated.
    - If the deterministic `insightReport.releaseReadiness()` is `BLOCKED`, reject AI text containing `"yayına uygundur"` or `"release ready"` case-insensitively by returning `notGenerated("AI output contradicted deterministic release readiness.")`.
    - If JSON parsing fails, return `notGenerated("AI response could not be parsed as JSON.")`.
    - If the client throws, return `notGenerated("AI report generation failed: " + concise message)`, capped to 300 characters.
- Add tests with a fake `PerformanceAiClient`:
  - `parsesValidAiJson`
  - `returnsFallbackWhenAiThrows`
  - `returnsFallbackWhenJsonCannotBeParsed`
  - `dropsInvalidActionPriorities`
  - `rejectsReadinessContradiction`

**Done When:**
- AI report service returns generated reports for valid JSON.
- AI failures and invalid output produce `generated=false` reports without throwing.
- The service does not mutate deterministic report or insight inputs.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceAiReportServiceTest test`. Expected: tests pass.

---

### TASK-04: Persist Insight And AI Snapshots After Run Completion

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceReportSnapshotService.java` (create)
- `src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T4

**Implementation Notes:**
- Create `@Service class PerformanceReportSnapshotService`.
- Inject:
  - `PerformanceInsightBuilder`
  - `PerformanceManagementReportBuilder`
  - `PerformanceAiReportService`
- Add method:
  ```java
  public PerformanceReportSnapshot build(
          PerfRsltEntity result,
          PerformanceThreadGroup threadDetail
  )
  ```
  where `PerformanceReportSnapshot` is an inner record:
  ```java
  public record PerformanceReportSnapshot(
          PerformanceManagementReport managementReport,
          PerformanceInsightReport insightReport,
          PerformanceAiManagementReport aiManagementReport
  ) {}
  ```
- The method builds:
  - `managementReport` with the existing `PerformanceManagementReportBuilder`.
  - `insightReport` with `PerformanceInsightBuilder.build(...)`.
  - `aiManagementReport` with `PerformanceAiReportService.generate(...)`.
- In `ApiCallServiceImpl.persistPerformanceResult(...)`, after:
  ```java
  compared.setValidationChecklist(performanceValidationChecklistBuilder.build(compared, runningItem));
  ```
  and before the final `performanceResultRepository.save(compared);`, call the snapshot service and set:
  ```java
  PerformanceReportSnapshotService.PerformanceReportSnapshot snapshot = performanceReportSnapshotService.build(compared, runningItem);
  compared.setInsightReport(snapshot.insightReport());
  compared.setAiManagementReport(snapshot.aiManagementReport());
  ```
  Keep `managementReport` dynamic; do not persist it in this task.
- Inject `PerformanceReportSnapshotService` into `ApiCallServiceImpl` through constructor injection.
- Update `PerformanceExportService.buildPayload(...)`:
  - Use existing builder for `managementReport` as it does today.
  - For `insightReport`, return `result.getInsightReport()` when present. If missing, compute it using `PerformanceInsightBuilder` injected into `PerformanceExportService`.
  - For `aiManagementReport`, return `result.getAiManagementReport()` when present. If missing, return `PerformanceAiManagementReport.notGenerated("AI report is not available for this performance result.")`.
- Update `PerformanceExportService` constructor to inject `PerformanceInsightBuilder`.
- Update `PerformanceExportServiceTest` constructor calls and assertions:
  - Instantiate `PerformanceExportService(new PerformanceManagementReportBuilder(), new PerformanceInsightBuilder())`.
  - Assert JSON payload has non-null `managementReport`.
  - Assert JSON payload has non-null `insightReport` when source entity has enough run data.
  - Assert JSON payload has non-null `aiManagementReport` fallback for legacy result.

**Done When:**
- Completed runs persist `insightReport` and `aiManagementReport`.
- AI generation failure persists `generated=false` instead of failing the run.
- `/performance/analysis` returns insight and AI fields for new and legacy results.
- Export service tests cover legacy fallback behavior.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceExportServiceTest,PerformanceInsightBuilderTest,PerformanceAiReportServiceTest test`. Expected: tests pass.

---

### TASK-05: Frontend Types And Translations

**Targets:**
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify)
- `../../apihub-fe/apihub-fe/messages/tr.json` (modify)
- `../../apihub-fe/apihub-fe/messages/en.json` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Add frontend union types:
  ```ts
  export type PerformanceAnomalyLevel = 'NORMAL' | 'WATCH' | 'ANOMALOUS' | 'CRITICAL';
  export type PerformanceBottleneckType = 'NONE' | 'ERROR' | 'LATENCY' | 'THROUGHPUT' | 'INSTABILITY' | 'ENVIRONMENT' | 'MIXED';
  export type PerformanceReleaseReadiness = 'READY' | 'CONDITIONAL' | 'BLOCKED' | 'UNKNOWN';
  export type PerformanceInsightSeverity = 'INFO' | 'WARNING' | 'HIGH' | 'CRITICAL';
  ```
- Add interfaces matching backend DTOs:
  ```ts
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
  }
  ```
- Add `insightReport?: PerformanceInsightReport | null;` and `aiManagementReport?: PerformanceAiManagementReport | null;` to `PerformanceExportPayload`, `PerformanceResultDto`, and `PerformanceHistoryItem`.
- Add translation keys under `performance` in both `tr.json` and `en.json`.
  - Turkish labels:
    - `decisionSummary`: `Karar Özeti`
    - `releaseReadiness`: `Yayın Hazırlığı`
    - `apdexScore`: `Apdex Skoru`
    - `sloCompliance`: `SLO Uyumu`
    - `regressionScore`: `Regresyon Skoru`
    - `anomalyScore`: `Anomali Skoru`
    - `aiGenerated`: `AI Yorumu Üretildi`
    - `aiFallback`: `AI Yorumu Kullanılamıyor`
    - `technicalFindings`: `Teknik Bulgular`
    - `bottleneckType`: `Darboğaz Tipi`
    - `rootCauseHints`: `Root Cause İpuçları`
    - `aiNarrative`: `AI Destekli Yorum`
    - `actionPlan`: `Aksiyon Planı`
    - `limitations`: `Sınırlamalar`
    - `noInsightReport`: `Insight raporu bulunamadı.`
    - `noAiReport`: `AI raporu bulunamadı.`
    - `noRootCauseHints`: `Root cause ipucu bulunamadı.`
    - `regressionUnavailable`: `Baseline karşılaştırması bulunmuyor.`
    - `ready`: `Yayına Uygun`
    - `conditional`: `Koşullu Uygun`
    - `blocked`: `Uygun Değil`
    - `unknown`: `Bilinmiyor`
    - `showGoodSteps`: `İyi Durumdaki Adımları Göster`
    - `hideGoodSteps`: `İyi Durumdaki Adımları Gizle`
  - English labels with matching keys:
    - `Decision Summary`, `Release Readiness`, `Apdex Score`, `SLO Compliance`, `Regression Score`, `Anomaly Score`, `AI Narrative Generated`, `AI Narrative Unavailable`, `Technical Findings`, `Bottleneck Type`, `Root Cause Hints`, `AI Narrative`, `Action Plan`, `Limitations`, `Insight report is not available.`, `AI report is not available.`, `No root-cause hints are available.`, `Baseline comparison is not available.`, `Ready`, `Conditional`, `Blocked`, `Unknown`, `Show Good Steps`, `Hide Good Steps`.

**Done When:**
- Frontend types expose insight and AI report payloads.
- Translation files include all labels used by upcoming components.
- Existing performance types remain exported.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint completes without type or translation errors.

---

### TASK-06: Report UI Section Components

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceReportDecisionHeader.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceExecutiveSummaryPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceTechnicalFindingsPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceRegressionTrendPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceRootCauseHintsPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceActionPlanPanel.tsx` (create)

**Model Tier:** T3

**Implementation Notes:**
- Each component must be a client component only if it uses React state. Components in this task can be stateless and do not need `'use client'`.
- Use MUI `Paper variant="outlined" sx={{ p: 2 }}` for each section.
- Use compact dashboard typography; do not use hero-size headings.
- `PerformanceReportDecisionHeader` props:
  ```ts
  interface Props {
      managementReport?: PerformanceManagementReport | null;
      insightReport?: PerformanceInsightReport | null;
      aiReport?: PerformanceAiManagementReport | null;
  }
  ```
  Render status, risk, release readiness, Apdex, SLO, regression, anomaly, and AI generated/fallback chip. Format percentages with two decimals, Apdex with three decimals.
- `PerformanceExecutiveSummaryPanel` props:
  ```ts
  interface Props {
      managementReport?: PerformanceManagementReport | null;
      aiReport?: PerformanceAiManagementReport | null;
  }
  ```
  Use `aiReport.executiveNarrative` when `aiReport.generated === true`; otherwise use `managementReport.executiveSummary`; otherwise show `t('reportUnavailable')`.
- `PerformanceTechnicalFindingsPanel` props:
  ```ts
  interface Props {
      insightReport?: PerformanceInsightReport | null;
      managementReport?: PerformanceManagementReport | null;
  }
  ```
  Render bottleneck type, metric insights, and problem areas. If both are missing, show `t('noInsightReport')`.
- `PerformanceRegressionTrendPanel` props:
  ```ts
  interface Props {
      insightReport?: PerformanceInsightReport | null;
      baselineComparison?: PerformanceComparisonResult | null;
      trendSummary?: string | null;
  }
  ```
  Render regression score when available; group baseline metrics into improved, regressed, and informational using `improvement === true`, `false`, and `null`.
- `PerformanceRootCauseHintsPanel` props:
  ```ts
  interface Props {
      insightReport?: PerformanceInsightReport | null;
  }
  ```
  Render `rootCauseHints`, including severity, category, signal, explanation, and recommendation. Empty state uses `t('noRootCauseHints')`.
- `PerformanceAiNarrativePanel` props:
  ```ts
  interface Props {
      aiReport?: PerformanceAiManagementReport | null;
  }
  ```
  Render generated/fallback status, technical narrative, root-cause narrative, release readiness narrative, limitations, and error message.
- `PerformanceActionPlanPanel` props:
  ```ts
  interface Props {
      aiReport?: PerformanceAiManagementReport | null;
      managementReport?: PerformanceManagementReport | null;
  }
  ```
  If `aiReport.generated === true` and `recommendedActionPlan` has items, render P0/P1/P2 action cards. Otherwise render `managementReport.recommendedActions` as list items.
- Use simple local helpers in components for color mapping:
  - `READY` success, `CONDITIONAL` warning, `BLOCKED` error, `UNKNOWN` default.
  - `INFO` info, `WARNING` warning, `HIGH` error, `CRITICAL` error.

**Done When:**
- Seven focused report section components compile.
- Each component renders a meaningful empty state.
- Components depend only on their props and `useTranslations('performance')`.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-07: Step Risk Panel And Report Composition

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceStepRiskPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create `PerformanceStepRiskPanel` with props:
  ```ts
  interface Props {
      managementReport?: PerformanceManagementReport | null;
      insightReport?: PerformanceInsightReport | null;
  }
  ```
- It renders `managementReport.stepAssessments ?? []`.
- Group by `NEEDS_IMPROVEMENT`, `WATCH`, and `GOOD`.
- `NEEDS_IMPROVEMENT` and `WATCH` are shown by default.
- `GOOD` is hidden behind a MUI `Button` toggle with labels `showGoodSteps` and `hideGoodSteps`.
- Each card shows:
  - step name
  - status chip
  - priority chip
  - main reason
  - evidence
  - impact
  - recommendation
  - compact metrics: average, P95, P99, error rate, throughput
- If matching `PerformanceStepInsight` exists by `stepName`, show an additional caption row with `stepInsight.explanation`.
- Modify `PerformanceManagementReportPanel` to compose:
  - `PerformanceReportDecisionHeader`
  - `PerformanceExecutiveSummaryPanel`
  - `PerformanceTechnicalFindingsPanel`
  - `PerformanceStepRiskPanel`
  - `PerformanceRegressionTrendPanel`
  - `PerformanceRootCauseHintsPanel`
  - `PerformanceAiNarrativePanel`
  - `PerformanceActionPlanPanel`
- Update `PerformanceManagementReportPanel` props:
  ```ts
  interface PerformanceManagementReportPanelProps {
      report?: PerformanceManagementReport | null;
      insightReport?: PerformanceInsightReport | null;
      aiReport?: PerformanceAiManagementReport | null;
      baselineComparison?: PerformanceComparisonResult | null;
  }
  ```
- Preserve legacy fallback: if all three report props are absent, render `<Alert severity="info">{t('reportUnavailable')}</Alert>`.

**Done When:**
- Existing `Rapor` tab still renders when only `managementReport` is present.
- New insight and AI sections render when provided.
- Step risk groups remain accessible and do not overflow on narrow screens.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-08: Wire Analysis Payload Into Report Tab

**Targets:**
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` (modify)

**Model Tier:** T2

**Implementation Notes:**
- After existing detail derivations, add:
  ```ts
  const detailInsightReport = analysisData?.insightReport
      ?? selectedHistoryItem?.insightReport
      ?? selectedResult?.insightReport
      ?? null;
  const detailAiManagementReport = analysisData?.aiManagementReport
      ?? selectedHistoryItem?.aiManagementReport
      ?? selectedResult?.aiManagementReport
      ?? null;
  ```
- Update the `PerformanceManagementReportPanel` call:
  ```tsx
  <PerformanceManagementReportPanel
      report={detailManagementReport}
      insightReport={detailInsightReport}
      aiReport={detailAiManagementReport}
      baselineComparison={detailBaselineComparison}
  />
  ```
- When mapping `runningItems` from `results`, include `insightReport` and `aiManagementReport` fields.
- Do not change tab order, history loading, comparison behavior, live monitor, or test launch behavior.

**Done When:**
- `Rapor` tab receives management, insight, AI, and baseline data.
- Legacy API responses still render without runtime errors.
- Running result mapping preserves new fields when present.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-09: End-To-End Verification And Build Checks

**Targets:**
- `src/test/java/etiya/omniAutomation/service/PerformanceInsightBuilderTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Add one export-level assertion in `PerformanceExportServiceTest`:
  - For a legacy result with no persisted AI report, `payload.aiManagementReport().generated()` is `false`.
  - For a result with persisted `insightReport`, the same object is returned in `payload.insightReport()`.
- Add one builder-level test in `PerformanceInsightBuilderTest`:
  - `buildsNullSafeReportForLegacyResult` passes all nullable inputs and asserts:
    - report is non-null
    - anomaly level is `NORMAL`
    - release readiness is `UNKNOWN`
    - SLO compliance is `0.0`
- Add one AI service test in `PerformanceAiReportServiceTest`:
  - `doesNotSendRawThreadDetail` verifies the fake AI client receives a prompt that does not contain sample raw error text longer than the summarized input used by the service. Use a deliberately long fake error body and assert the prompt does not contain the full string.

**Done When:**
- Backend tests cover null-safe legacy payloads, persisted report passthrough, AI fallback, and prompt safety.
- No frontend file changes are made in this task.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceInsightBuilderTest,PerformanceAiReportServiceTest,PerformanceExportServiceTest test`. Expected: all targeted tests pass.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes after previous frontend tasks.
