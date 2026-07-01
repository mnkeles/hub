# Performance AI Report Enhancements - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Add stronger AI report validation, report metadata, AI regeneration, richer export, print/PDF support, and a risk matrix to the existing performance `Rapor` experience.

**Architecture:** Existing JSONB report snapshots remain the persistence boundary; no new database columns or AI history table are added. Backend deterministic decisions stay in `PerformanceInsightBuilder`, AI quality gates move into a dedicated validator, and regeneration overwrites only `aiManagementReport`. Frontend extends the existing report panel with risk matrix, regenerate, observability, and browser print/PDF actions.

**Tech / dependencies:** Spring Boot 3.5, Java 21 records/services, Jackson, existing Spring AI OpenAI integration, JUnit 5, Next.js 16, React 19, MUI 7, TypeScript, next-intl. No new backend PDF dependency or Maven wrapper work is included.

**File map:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightReport.java` - Adds insight schema/version metadata.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAiManagementReport.java` - Adds AI schema/version and observability metadata.
- `src/main/java/etiya/omniAutomation/service/PerformanceReportVersions.java` - Central constants for report schema and generator versions.
- `src/main/java/etiya/omniAutomation/service/PerformanceInsightBuilder.java` - Populates insight report version metadata.
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` - Builds prompt hashes, generation duration, response metadata, and delegates validation.
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportValidator.java` - Validates AI narratives/actions against deterministic report data.
- `src/main/java/etiya/omniAutomation/service/PerformanceAiValidationResult.java` - Small validation result record.
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportRegenerationService.java` - Rebuilds and persists overwritten AI reports for an existing result.
- `src/main/java/etiya/omniAutomation/controller/PerformanceController.java` - Adds AI report regeneration endpoint.
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` - Exposes regeneration through service layer.
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` - Adds insight, AI action, root-cause, and observability sections to CSV.
- `src/test/java/etiya/omniAutomation/service/PerformanceInsightBuilderTest.java` - Updates constructors and asserts version metadata.
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` - Covers metadata, validation fallback, and prompt safety.
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportValidatorTest.java` - Unit tests strict validator rules.
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` - Covers enriched CSV and legacy null metadata.
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportRegenerationServiceTest.java` - Covers overwrite and fallback persistence behavior.
- `../../apihub-fe/apihub-fe/types/performance.ts` - Adds frontend metadata fields.
- `../../apihub-fe/apihub-fe/messages/tr.json` - Adds Turkish labels for regenerate, print/PDF, risk matrix, and observability.
- `../../apihub-fe/apihub-fe/messages/en.json` - Adds English labels for regenerate, print/PDF, risk matrix, and observability.
- `../../apihub-fe/apihub-fe/services/performanceService.ts` - Adds regenerate API call.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceRiskMatrixPanel.tsx` - Builds and renders risk rows from deterministic report data.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiObservabilityPanel.tsx` - Shows AI generation metadata.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiRegenerateButton.tsx` - Handles regenerate request, loading, and callback.
- `../../apihub-fe/apihub-fe/components/performance/PerformancePrintActions.tsx` - Provides browser print/PDF action.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` - Composes risk matrix, observability, regenerate, and print actions.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx` - Keeps AI narrative display compatible with metadata and fallback states.
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` - Holds updated AI report state after regeneration.
- `../../apihub-fe/apihub-fe/app/globals.css` - Adds print-only CSS for report PDF output.

---

### TASK-01: Backend Report Metadata Contract

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightReport.java` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceAiManagementReport.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceReportVersions.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceInsightBuilder.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceInsightBuilderTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Add this constants class:
  ```java
  package etiya.omniAutomation.service;

  public final class PerformanceReportVersions {
      public static final int INSIGHT_SCHEMA_VERSION = 2;
      public static final String INSIGHT_GENERATOR_VERSION = "performance-insight-v2";
      public static final int AI_REPORT_SCHEMA_VERSION = 2;
      public static final String AI_REPORT_GENERATOR_VERSION = "performance-ai-report-v2";

      private PerformanceReportVersions() {
      }
  }
  ```
- Extend `PerformanceInsightReport` by appending these record components after `stepInsights`:
  ```java
  Integer schemaVersion,
  String generatedByVersion
  ```
- Extend `PerformanceAiManagementReport` by appending these record components after `errorMessage`:
  ```java
  Integer schemaVersion,
  String generatedByVersion,
  Long durationMs,
  Integer attemptCount,
  String failureReason,
  List<String> validationErrors,
  String promptHash,
  String inputSummaryHash,
  Integer responseSize,
  Integer promptTokens,
  Integer completionTokens,
  Integer totalTokens
  ```
- Update `PerformanceAiManagementReport.notGenerated(String errorMessage)` to return schema/generator version values, `attemptCount=1`, empty `validationErrors`, null hashes, null response/token values, and `failureReason=errorMessage`.
- Add a second static factory:
  ```java
  public static PerformanceAiManagementReport notGenerated(
          String errorMessage,
          String failureReason,
          List<String> validationErrors,
          Long durationMs,
          String promptHash,
          String inputSummaryHash,
          Integer responseSize
  )
  ```
  This factory must set `generated=false`, schema/generator versions, `attemptCount=1`, and null token values.
- In `PerformanceInsightBuilder.build(...)`, pass `PerformanceReportVersions.INSIGHT_SCHEMA_VERSION` and `PerformanceReportVersions.INSIGHT_GENERATOR_VERSION` to the `PerformanceInsightReport` constructor.
- In `PerformanceAiReportService`, every generated `PerformanceAiManagementReport` constructor call must pass `PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION`, `PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION`, and null observability values for now. Detailed observability is added in TASK-02.
- Update all test helper constructors for `PerformanceInsightReport` and `PerformanceAiManagementReport` to include the new fields or use the updated `notGenerated(...)`.

**Done When:**
- Backend report DTOs expose optional schema and generator metadata.
- Deterministic insight reports produced by the builder include non-null schema/version values.
- AI generated and fallback reports include non-null schema/version values.
- Existing tests compile after constructor updates.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceInsightBuilderTest,PerformanceAiReportServiceTest,PerformanceExportServiceTest test`. Expected: targeted tests pass.
- If `mvn` is unavailable, run `git diff --check -- src/main/java/etiya/omniAutomation/business/dto/PerformanceInsightReport.java src/main/java/etiya/omniAutomation/business/dto/PerformanceAiManagementReport.java src/main/java/etiya/omniAutomation/service/PerformanceReportVersions.java src/main/java/etiya/omniAutomation/service/PerformanceInsightBuilder.java src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java src/test/java/etiya/omniAutomation/service/PerformanceInsightBuilderTest.java src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java`. Expected: no whitespace errors.

---

### TASK-02: AI Validation And Observability

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceAiValidationResult.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportValidator.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportValidatorTest.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` (modify)

**Model Tier:** T4

**Implementation Notes:**
- Create validation result:
  ```java
  package etiya.omniAutomation.service;

  import java.util.List;

  public record PerformanceAiValidationResult(boolean valid, List<String> errors) {
      public static PerformanceAiValidationResult valid() {
          return new PerformanceAiValidationResult(true, List.of());
      }

      public static PerformanceAiValidationResult invalid(List<String> errors) {
          return new PerformanceAiValidationResult(false, errors == null ? List.of() : errors);
      }
  }
  ```
- Create `@Service class PerformanceAiReportValidator` with:
  ```java
  public PerformanceAiValidationResult validate(
          PerformanceAiManagementReport aiReport,
          PerformanceManagementReport managementReport,
          PerformanceInsightReport insightReport,
          PerformanceRunSummary runSummary,
          PerformanceThresholdResult thresholdResult
  )
  ```
- Validator rules:
  - `executiveNarrative`, `technicalNarrative`, and `releaseReadinessNarrative` must be non-blank.
  - Every `recommendedActionPlan` item must have priority `P0`, `P1`, or `P2`; invalid priority adds error `"Invalid action priority: <priority>"`.
  - Every action item must reference either an allowed problem step or an allowed metric. Allowed steps come from `managementReport.stepAssessments` where status is `NEEDS_IMPROVEMENT` or `WATCH`, `managementReport.problemAreas.stepName`, and `insightReport.stepInsights.stepName`. Allowed metrics come from `insightReport.metricInsights.metric`, `managementReport.problemAreas.metric`, and failed `managementReport.slaSummary.metric`. If both `relatedStepName` and `relatedMetric` are blank or unrelated, add `"Action item is not tied to a deterministic problem: <title>"`.
  - If `insightReport.releaseReadiness() == BLOCKED`, reject combined AI text containing any normalized phrase: `"yayina uygundur"`, `"release ready"`, `"performans iyi"`, `"sorun yok"`, `"test basarili"`.
  - If `thresholdResult != null && !thresholdResult.passed()`, reject combined AI text containing normalized healthy phrases: `"performans iyi"`, `"sorun yok"`, `"test basarili"`, `"all good"`, `"healthy performance"`.
  - If a metric insight exists for `P95`, `P99`, `Hata orani`, `Error Rate`, `Throughput`, or `Ortalama`, the combined AI text must mention that metric name or its normalized equivalent. Missing mention adds `"AI narrative ignores failed metric: <metric>"`.
- In `PerformanceAiReportService`:
  - Inject `PerformanceAiReportValidator`.
  - Measure generation duration with `System.nanoTime()` around `performanceAiClient.complete(...)`.
  - Build `userPrompt` as today, then compute:
    - `promptHash = sha256(systemPrompt + "\n" + userPrompt)`
    - `inputSummaryHash = sha256(userPrompt)`
    - `responseSize = aiResponse == null ? 0 : aiResponse.getBytes(StandardCharsets.UTF_8).length`
  - Add private helper:
    ```java
    private String sha256(String value)
    ```
    using `MessageDigest.getInstance("SHA-256")` and lowercase hex.
  - Parse JSON into the existing private response DTO, create a generated `PerformanceAiManagementReport` with metadata populated: schema/version constants, `durationMs`, `attemptCount=1`, `failureReason=null`, `validationErrors=List.of()`, hashes, response size, null token fields.
  - Call validator. If invalid, return `PerformanceAiManagementReport.notGenerated("AI output failed validation.", "VALIDATION_FAILED", validation.errors(), durationMs, promptHash, inputSummaryHash, responseSize)`.
  - If client throws, return `notGenerated("AI report generation failed: " + conciseMessage, "CLIENT_ERROR", List.of(), durationMs, promptHash, inputSummaryHash, null)`.
  - If JSON parsing fails, return `notGenerated("AI response could not be parsed as JSON.", "PARSE_ERROR", List.of(), durationMs, promptHash, inputSummaryHash, responseSize)`.
  - Remove the old partial behavior that dropped invalid action priorities; invalid priorities now make the whole AI report fallback.
- Tests:
  - `PerformanceAiReportValidatorTest.rejectsBlockedReadinessContradiction` asserts BLOCKED + `"release ready"` is invalid.
  - `PerformanceAiReportValidatorTest.rejectsHealthyLanguageWhenThresholdFailed` asserts failed threshold + `"performans iyi"` is invalid.
  - `PerformanceAiReportValidatorTest.rejectsUnrelatedActionItem` asserts action with no matching step/metric is invalid.
  - `PerformanceAiReportValidatorTest.acceptsActionTiedToProblemMetric` asserts valid P1 action tied to `P95` passes.
  - Update `PerformanceAiReportServiceTest.dropsInvalidActionPriorities` into `returnsFallbackForInvalidActionPriorities`, expecting `generated=false`, `failureReason="VALIDATION_FAILED"`.
  - Add `recordsPromptObservabilityMetadata` to assert generated reports have non-null `durationMs`, `promptHash`, `inputSummaryHash`, `responseSize`, and `attemptCount == 1`.

**Done When:**
- AI validation is strict and returns full fallback for invalid AI output.
- Generated and fallback AI reports include prompt hashes, duration, attempt count, response size, failure reason, and validation errors.
- Raw prompt and raw response text are not persisted in DTO fields.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceAiReportValidatorTest,PerformanceAiReportServiceTest test`. Expected: tests pass.
- If `mvn` is unavailable, run `git diff --check -- src/main/java/etiya/omniAutomation/service/PerformanceAiValidationResult.java src/main/java/etiya/omniAutomation/service/PerformanceAiReportValidator.java src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java src/test/java/etiya/omniAutomation/service/PerformanceAiReportValidatorTest.java src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java`. Expected: no whitespace errors.

---

### TASK-03: AI Report Regeneration Endpoint

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceAiReportRegenerationService.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `src/main/java/etiya/omniAutomation/controller/PerformanceController.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportRegenerationServiceTest.java` (create)

**Model Tier:** T4

**Implementation Notes:**
- Create `@Service @RequiredArgsConstructor class PerformanceAiReportRegenerationService`.
- Inject:
  - `PerformanceResultRepository`
  - `PerformanceManagementReportBuilder`
  - `PerformanceInsightBuilder`
  - `PerformanceAiReportService`
- Add method:
  ```java
  @Transactional
  public PerformanceAiManagementReport regenerate(
          Long performanceResultId,
          PerformanceThreadGroup threadDetail
  )
  ```
- Method behavior:
  - If `performanceResultId == null`, throw `new ResponseStatusException(HttpStatus.BAD_REQUEST, "performanceResultId is required.")`.
  - Load `PerfRsltEntity` with `performanceResultRepository.findById(...)`; if missing, throw `new ResponseStatusException(HttpStatus.NOT_FOUND, "Performance result not found: " + performanceResultId)`.
  - Build `PerformanceManagementReport` using existing `PerformanceManagementReportBuilder.build(...)` with entity fields.
  - Use `result.getInsightReport()` if non-null; otherwise compute it with `PerformanceInsightBuilder.build(...)` and set it back on the entity.
  - Generate AI report through `PerformanceAiReportService.generate(...)`.
  - Set `result.setAiManagementReport(aiReport)`.
  - Save the entity.
  - Return the saved AI report.
  - Do not modify `result.getPerfStatus()`.
- In `PerformanceService`, inject `PerformanceAiReportRegenerationService` and add:
  ```java
  public PerformanceAiManagementReport regenerateAiReport(Long performanceResultId) {
      return performanceAiReportRegenerationService.regenerate(performanceResultId, loadThreadGroup(performanceResultId));
  }
  ```
- In `PerformanceController`, import `PerformanceAiManagementReport` and add:
  ```java
  @PostMapping("/{performanceResultId}/ai-report/regenerate")
  public ResponseEntity<PerformanceAiManagementReport> regenerateAiReport(@PathVariable Long performanceResultId) {
      return ResponseEntity.ok(this.performanceService.regenerateAiReport(performanceResultId));
  }
  ```
- Tests:
  - `regeneratesAndOverwritesAiReport` uses a fake `PerformanceAiReportService` or Mockito mock to return a generated report, asserts repository save receives entity with the new `aiManagementReport`.
  - `persistsFallbackWithoutChangingRunStatus` returns a `generated=false` report, asserts original `perfStatus` is unchanged.
  - `throwsNotFoundForMissingResult` asserts `ResponseStatusException` status is `NOT_FOUND`.

**Done When:**
- `POST /performance/{performanceResultId}/ai-report/regenerate` returns the updated `PerformanceAiManagementReport`.
- Regeneration overwrites only the current `aiManagementReport`.
- Missing insight report is recomputed deterministically and persisted.
- AI fallback reports are persisted without changing run status.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceAiReportRegenerationServiceTest test`. Expected: tests pass.
- Manual: From backend running locally, call `POST /performance/{id}/ai-report/regenerate` for an existing completed result. Expected: HTTP 200 and body contains `generated`, `generatedAt`, `schemaVersion`, and `generatedByVersion`.

---

### TASK-04: Enriched CSV Export

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Keep existing CSV sections and append new sections after `Error Summary`.
- Add private methods:
  ```java
  private void appendDecisionSummary(StringBuilder csv, PerformanceExportPayload payload)
  private void appendMetricInsights(StringBuilder csv, PerformanceExportPayload payload)
  private void appendRootCauseHints(StringBuilder csv, PerformanceExportPayload payload)
  private void appendAiActionPlan(StringBuilder csv, PerformanceExportPayload payload)
  private void appendAiObservability(StringBuilder csv, PerformanceExportPayload payload)
  ```
- `appendDecisionSummary` output:
  ```text
  Decision Summary
  Metric,Value
  Release Readiness,<payload.insightReport.releaseReadiness>
  Anomaly Score,<payload.insightReport.anomalyScore>
  Anomaly Level,<payload.insightReport.anomalyLevel>
  Apdex,<payload.insightReport.apdexScore>
  SLO Compliance,<payload.insightReport.sloCompliancePercent>
  Regression Score,<payload.insightReport.regressionScore>
  Bottleneck Type,<payload.insightReport.bottleneckType>
  AI Generated,<payload.aiManagementReport.generated>
  ```
  Use empty cells when `insightReport` or `aiManagementReport` is null.
- `appendMetricInsights` headers: `Metric,Severity,Actual,Expected,Explanation`.
- `appendRootCauseHints` headers: `Severity,Category,Signal,Explanation,Recommendation`.
- `appendAiActionPlan` headers: `Priority,Title,Description,Related Step,Related Metric`.
- `appendAiObservability` headers: `Metric,Value`, rows for model, durationMs, attemptCount, failureReason, validationErrors joined with `"; "`, promptHash, inputSummaryHash, responseSize, promptTokens, completionTokens, totalTokens.
- Reuse the existing `row(...)` helper for escaping.
- Tests:
  - Extend `buildsCsvWithRunThresholdStepAndErrorSections` to assert CSV contains `Decision Summary`, `Metric Insights`, `Root Cause Hints`, `AI Action Plan`, and `AI Observability`.
  - Add `buildsCsvWhenInsightAndAiMetadataAreMissing` by setting `entity.setInsightReport(null)` and `entity.setAiManagementReport(null)`, then assert CSV still contains `Decision Summary` and does not throw.
  - Add fixture data with one metric insight, one root-cause hint, and one AI action so section rows are asserted.

**Done When:**
- CSV export includes deterministic insight, AI action plan, and observability sections.
- CSV export remains safe for legacy results with null insight/AI reports.
- JSON export behavior is unchanged.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceExportServiceTest test`. Expected: tests pass.
- Manual: Export CSV from the UI for a result with AI data. Expected: downloaded CSV contains the five new sections.

---

### TASK-05: Frontend Contract, Labels, And API Client

**Targets:**
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify)
- `../../apihub-fe/apihub-fe/messages/tr.json` (modify)
- `../../apihub-fe/apihub-fe/messages/en.json` (modify)
- `../../apihub-fe/apihub-fe/services/performanceService.ts` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Extend frontend `PerformanceInsightReport` with optional:
  ```ts
  schemaVersion?: number | null;
  generatedByVersion?: string | null;
  ```
- Extend frontend `PerformanceAiManagementReport` with optional:
  ```ts
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
  ```
- In `performanceService.ts`, import `PerformanceAiManagementReport` and add:
  ```ts
  async regenerateAiReport(performanceResultId: number): Promise<PerformanceAiManagementReport> {
      const response = await api.post(`/performance/${performanceResultId}/ai-report/regenerate`);
      return response.data;
  }
  ```
- Add Turkish keys under `performance`:
  - `regenerateAiReport`: `AI Raporunu Yeniden Oluştur`
  - `regeneratingAiReport`: `AI raporu oluşturuluyor...`
  - `aiReportRegenerated`: `AI raporu yeniden oluşturuldu.`
  - `aiReportRegenerationFailed`: `AI raporu yeniden oluşturulamadı.`
  - `printPdf`: `PDF / Yazdır`
  - `riskMatrix`: `Risk Matrisi`
  - `sourceType`: `Kaynak Tipi`
  - `riskSignal`: `Risk Sinyali`
  - `aiObservability`: `AI Gözlemlenebilirlik`
  - `schemaVersion`: `Şema Versiyonu`
  - `generatedByVersion`: `Üreten Versiyon`
  - `durationMsLabel`: `Süre (ms)`
  - `attemptCount`: `Deneme Sayısı`
  - `failureReason`: `Hata Nedeni`
  - `validationErrors`: `Validasyon Hataları`
  - `promptHash`: `Prompt Hash`
  - `inputSummaryHash`: `Girdi Özeti Hash`
  - `responseSize`: `Yanıt Boyutu`
  - `tokenUsage`: `Token Kullanımı`
  - `noRiskSignals`: `Risk sinyali bulunamadı.`
- Add English keys with matching names:
  - `Regenerate AI Report`, `Regenerating AI report...`, `AI report regenerated.`, `AI report could not be regenerated.`, `PDF / Print`, `Risk Matrix`, `Source Type`, `Risk Signal`, `AI Observability`, `Schema Version`, `Generated By Version`, `Duration (ms)`, `Attempt Count`, `Failure Reason`, `Validation Errors`, `Prompt Hash`, `Input Summary Hash`, `Response Size`, `Token Usage`, `No risk signals found.`

**Done When:**
- Frontend type contract includes AI metadata fields.
- Frontend service exposes `regenerateAiReport(...)`.
- TR/EN translation files contain labels used by upcoming components.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `node -e "JSON.parse(require('fs').readFileSync('messages/tr.json','utf8')); JSON.parse(require('fs').readFileSync('messages/en.json','utf8')); console.log('json ok')"`. Expected: `json ok`.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint exits 0.

---

### TASK-06: Risk Matrix And Observability Panels

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceRiskMatrixPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiObservabilityPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (modify)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create `PerformanceRiskMatrixPanel` with props:
  ```ts
  interface Props {
      managementReport?: PerformanceManagementReport | null;
      insightReport?: PerformanceInsightReport | null;
      baselineComparison?: PerformanceComparisonResult | null;
  }
  ```
- Build local risk rows:
  ```ts
  interface RiskRow {
      sourceType: 'Step' | 'Regression' | 'Root Cause' | 'Metric';
      name: string;
      level: 'Critical / High' | 'Watch' | 'Good / Info';
      explanation: string;
      score: number;
  }
  ```
- Row derivation:
  - Step rows from `managementReport.stepAssessments`; `NEEDS_IMPROVEMENT` score 100, `WATCH` score 60, `GOOD` score 10.
  - Metric rows from `insightReport.metricInsights`; `CRITICAL` score 100, `HIGH` score 90, `WARNING` score 60, `INFO` score 20.
  - Root-cause rows from `insightReport.rootCauseHints` using same severity score mapping.
  - Regression rows from `baselineComparison.metrics` where `improvement === false`, score 85.
  - Sort descending by score and render top five.
  - Empty state uses `t('noRiskSignals')`.
- Use MUI `Paper variant="outlined" sx={{ p: 2 }}` and compact typography. On mobile, rows render as stacked boxes; on desktop, use a simple table-like grid.
- Create `PerformanceAiObservabilityPanel` with props:
  ```ts
  interface Props {
      aiReport?: PerformanceAiManagementReport | null;
      insightReport?: PerformanceInsightReport | null;
  }
  ```
- Observability panel shows schema version, generatedByVersion, model, durationMs, attemptCount, failureReason, validationErrors, promptHash, inputSummaryHash, responseSize, and token usage. Use `-` for missing values. Do not show raw prompt or raw response.
- In `PerformanceManagementReportPanel`, render `<PerformanceRiskMatrixPanel />` immediately after `<PerformanceReportDecisionHeader />`, and render `<PerformanceAiObservabilityPanel />` after `<PerformanceAiNarrativePanel />`.
- In `PerformanceAiNarrativePanel`, keep fallback behavior and display `failureReason` and `validationErrors` when present in addition to `errorMessage`.

**Done When:**
- Rapor tab includes a risk matrix directly below decision summary.
- AI observability metadata is visible without exposing raw prompt/response.
- Legacy payloads with null metadata render without runtime errors.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint exits 0.
- Manual: Open a performance result report with partial data. Expected: risk matrix and observability panels render with `-` or empty states, not exceptions.

---

### TASK-07: Regenerate And Print/PDF UI

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceAiRegenerateButton.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformancePrintActions.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (modify)
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` (modify)
- `../../apihub-fe/apihub-fe/app/globals.css` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create `PerformanceAiRegenerateButton` with props:
  ```ts
  interface Props {
      performanceResultId?: number;
      onRegenerated: (report: PerformanceAiManagementReport) => void;
      onSuccess?: (message: string) => void;
      onError?: (message: string) => void;
  }
  ```
- Component behavior:
  - Disabled when `performanceResultId` is missing.
  - On click, set local loading state and call `performanceService.regenerateAiReport(performanceResultId)`.
  - On success, call `onRegenerated(report)` and `onSuccess?.(t('aiReportRegenerated'))`.
  - On failure, call `onError?.(t('aiReportRegenerationFailed'))`.
  - Button label uses `regeneratingAiReport` while loading, otherwise `regenerateAiReport`.
- Create `PerformancePrintActions` with:
  ```ts
  export default function PerformancePrintActions() {
      const t = useTranslations('performance');
      return <Button variant="outlined" size="small" onClick={() => window.print()}>{t('printPdf')}</Button>;
  }
  ```
- Update `PerformanceManagementReportPanel` props:
  ```ts
  interface PerformanceManagementReportPanelProps {
      report?: PerformanceManagementReport | null;
      insightReport?: PerformanceInsightReport | null;
      aiReport?: PerformanceAiManagementReport | null;
      baselineComparison?: PerformanceComparisonResult | null;
      performanceResultId?: number;
      onAiReportUpdated?: (report: PerformanceAiManagementReport) => void;
      onSuccess?: (message: string) => void;
      onError?: (message: string) => void;
  }
  ```
- In `PerformanceManagementReportPanel`, wrap report content in:
  ```tsx
  <Box className="performance-report-print-root" ...>
  ```
  Add a top action row with `PerformancePrintActions` and `PerformanceAiRegenerateButton`. Keep the action row out of print via CSS class `performance-report-print-hidden`.
- In `app/dashboard/performance/page.tsx`, pass:
  ```tsx
  performanceResultId={detailTitleId}
  onAiReportUpdated={(report) => {
      setAnalysisData((prev) => prev ? { ...prev, aiManagementReport: report } : prev);
      setSelectedHistoryItem((prev) => prev ? { ...prev, aiManagementReport: report } : prev);
      setSelectedResult((prev) => prev ? { ...prev, aiManagementReport: report } : prev);
  }}
  onSuccess={setSuccess}
  onError={setError}
  ```
- Add print CSS to `app/globals.css`:
  ```css
  @media print {
    body * {
      visibility: hidden;
    }

    .performance-report-print-root,
    .performance-report-print-root * {
      visibility: visible;
    }

    .performance-report-print-root {
      position: absolute;
      left: 0;
      top: 0;
      width: 100%;
      background: #ffffff;
      color: #000000;
      padding: 16px;
    }

    .performance-report-print-hidden {
      display: none !important;
    }
  }
  ```

**Done When:**
- Rapor tab has a working AI regenerate button.
- Regeneration updates displayed AI report state without refetching the whole detail dialog.
- Rapor tab has a PDF/print button using browser print.
- Print mode hides dashboard chrome and prints only the report content.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint exits 0.
- Manual: Open a report and click `PDF / Print`. Expected: browser print dialog opens and report content is the visible printable area.

---

### TASK-08: Final Compatibility And Verification Tests

**Targets:**
- `src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Add `PerformanceAiReportServiceTest.legacyFallbackCarriesMetadata`:
  - Call `PerformanceAiManagementReport.notGenerated("legacy unavailable")`.
  - Assert `generated=false`, `schemaVersion` equals `PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION`, `generatedByVersion` equals `PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION`, `attemptCount == 1`, and `validationErrors` is empty.
- Add `PerformanceExportServiceTest.csvIncludesAiObservabilityWhenFallback`:
  - Use entity with `aiManagementReport=PerformanceAiManagementReport.notGenerated("not generated")`.
  - Assert CSV contains `AI Observability`, `not generated`, and `performance-ai-report-v2`.
- In frontend `types/performance.ts`, make sure `validationErrors?: string[];` and token fields remain optional rather than required. This task should only adjust types if previous tasks accidentally made metadata required.

**Done When:**
- Backend tests cover generated/fallback metadata compatibility.
- CSV export test covers observability section for fallback reports.
- Frontend AI metadata fields remain optional for legacy API responses.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceAiReportServiceTest,PerformanceExportServiceTest,PerformanceAiReportValidatorTest,PerformanceAiReportRegenerationServiceTest test`. Expected: tests pass.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint exits 0.
- If backend `mvn` is unavailable, report that Maven is missing and include the frontend lint result plus `git diff --check` results for all touched files.
