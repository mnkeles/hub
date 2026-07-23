# Performance AI Report - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Add a saved AI-generated manager report to performance results and finish the remaining non-infrastructure performance screen gaps.

**Architecture:** Backend generates and persists `PerformanceAiReport` after a performance test finishes, using the existing Spring AI/OpenAI configuration and a deterministic fallback path. Frontend reads the saved report from existing performance payloads and shows it as the first detail-modal tab. Remaining UI gaps are completed with a shared performance screen component, an active-thread chart, and real timeout handling for performance request execution.

**Tech / dependencies:** Spring Boot, Spring AI `ChatClient.Builder`, Jackson `ObjectMapper`, Hibernate JSONB, Liquibase, JUnit 5, Mockito, Next.js, TypeScript, MUI, Recharts. No new runtime dependency is required.

**File map:**
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceAiReport.java` - persisted AI/fallback report DTO.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceAiReportSource.java` - report source enum, `AI` or `FALLBACK`.
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` - stores `aiReport` JSONB on `perf_rslt`.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` - exposes `aiReport` to result responses.
- `Apihub/src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` - exposes `aiReport` in history rows.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` - exposes `aiReport` in analysis/export payloads.
- `Apihub/src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` - maps `aiReport` between entity and DTO.
- `Apihub/src/main/resources/db/changelog/changes/liquibase-migration-file.xml` - adds `perf_rslt.ai_report`.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` - builds compact AI input, calls Spring AI, parses JSON, creates fallback report.
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` - wires AI report generation into final persistence and passes performance timeout to request execution.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceService.java` - includes `aiReport` in history and analysis payload construction.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` - includes `aiReport` in JSON payload and CSV report sections.
- `Apihub/src/main/java/etiya/omniAutomation/service/WebClientService.java` - adds timeout-capable exchange overload.
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallRequestOptions.java` - carries per-call timeout for performance execution.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` - covers AI parse success and fallback paths.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` - covers `aiReport` export output.
- `apihub-fe/types/performance.ts` - adds frontend `PerformanceAiReport` types and `aiReport` fields.
- `apihub-fe/components/performance/PerformanceAiReportPanel.tsx` - renders manager report tab content.
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` - shared upgraded performance screen used by both routes.
- `apihub-fe/app/dashboard/performance/page.tsx` - dashboard wrapper around shared performance screen.
- `apihub-fe/app/[projectShortCode]/performance/page.tsx` - project route wrapper around shared performance screen.
- `apihub-fe/components/performance/PerformanceChartsPanel.tsx` - adds Active Threads Over Time chart.
- `apihub-fe/messages/tr.json` - Turkish labels for report UI.
- `apihub-fe/messages/en.json` - English labels for report UI.

---

### TASK-01: Add AI Report Contract And Persistence

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceAiReport.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceAiReportSource.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` (modify)
- `Apihub/src/main/resources/db/changelog/changes/liquibase-migration-file.xml` (modify)

**Model Tier:** T3 - Power

**Implementation Notes:**
- Create `PerformanceAiReportSource` enum in package `etiya.omniAutomation.business.dto`:
  ```java
  public enum PerformanceAiReportSource {
      AI,
      FALLBACK
  }
  ```
- Create `PerformanceAiReport` record in package `etiya.omniAutomation.business.dto`:
  ```java
  public record PerformanceAiReport(
          String executiveSummary,
          String overallStatus,
          String businessImpact,
          List<String> goodPoints,
          List<String> badPoints,
          List<String> risks,
          List<String> recommendedActions,
          String technicalDetails,
          PerformanceAiReportSource source,
          Date generatedAt
  ) {
  }
  ```
  Import `java.util.Date` and `java.util.List`.
- In `PerfRsltEntity`, add:
  ```java
  @Type(JsonBinaryType.class)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ai_report", columnDefinition = "jsonb")
  private PerformanceAiReport aiReport;
  ```
  Add the `PerformanceAiReport` import.
- In `PerformanceResultDto`, add `private PerformanceAiReport aiReport;` with import. Lombok getters/setters already expose it.
- In `PerformanceSummaryResult`, add `PerformanceAiReport aiReport` before `List<PerformanceSummary> performanceSummaries`. Update both constructors so the compact constructor passes `null` for `aiReport`.
- In `PerformanceExportPayload`, add `PerformanceAiReport aiReport` before `List<PerformanceSummary> stepSummaries`.
- In `PerformanceResultMapper`, add both mappings:
  ```java
  @Mapping(target = "aiReport", source = "aiReport")
  ```
  for entity-to-DTO and DTO-to-entity methods.
- In `liquibase-migration-file.xml`, add a changeset near the existing `perf_rslt` JSONB additions:
  ```xml
  <changeSet id="2026-07-23-001-add-ai-report-to-perf-rslt" author="apihub">
      <preConditions onFail="MARK_RAN">
          <not>
              <columnExists tableName="perf_rslt" columnName="ai_report"/>
          </not>
      </preConditions>
      <addColumn tableName="perf_rslt">
          <column name="ai_report" type="jsonb"/>
      </addColumn>
  </changeSet>
  ```

**Done When:**
- `PerformanceAiReport` and `PerformanceAiReportSource` exist in backend DTO package.
- `perf_rslt.ai_report` has a Liquibase changeset.
- Entity, result DTO, history record, export payload, and mapper all include `aiReport`.

**Verification:**
- Manual: run `rg -n "aiReport|ai_report|PerformanceAiReport" Apihub/src/main/java Apihub/src/main/resources/db/changelog/changes/liquibase-migration-file.xml` and confirm every target surface is present.
- Automated: if Maven is available, run `mvn -DskipTests compile` from `Apihub`; expected result is successful compilation.

---

### TASK-02: Build AI Report Service With Fallback

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` (create)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceAiReportServiceTest.java` (create)

**Model Tier:** T3 - Power

**Implementation Notes:**
- Create `PerformanceAiReportService` in package `etiya.omniAutomation.service`.
- Inject `ChatClient.Builder` and `ObjectMapper` through constructor injection with Lombok `@RequiredArgsConstructor`.
- Public method signature:
  ```java
  public PerformanceAiReport generateReport(
          PerfRsltEntity result,
          PerformanceThreadGroup threadGroup
  )
  ```
  The method returns an AI report when parsing succeeds and a fallback report for any exception.
- Add helper method:
  ```java
  PerformanceAiReport fallbackReport(
          PerfRsltEntity result,
          PerformanceThreadGroup threadGroup,
          String fallbackReason
  )
  ```
  Package-private visibility is acceptable so the unit test can call it without reflection.
- The AI prompt must use only compact data from `PerfRsltEntity`:
  - `result.getRunSummary()`
  - `result.getThresholdResult()`
  - `result.getAnalysisSummary()`
  - `result.getErrorAnalysis()`
  - `result.getEnvironmentMetrics()`
  - `result.getBaselineComparison()`
  - worst step summaries from `result.getSummary()`, sorted by failed count, P99, P95, and average response time.
- Do not send full raw `threadGroup` samples to the AI prompt. `threadGroup` is only available to fallback logic if a count or step name is missing.
- Use this response contract in the prompt:
  ```json
  {
    "executiveSummary": "string",
    "overallStatus": "PASSED|FAILED|STOPPED|ERROR",
    "businessImpact": "string",
    "goodPoints": ["string"],
    "badPoints": ["string"],
    "risks": ["string"],
    "recommendedActions": ["string"],
    "technicalDetails": "string"
  }
  ```
  The service sets `source = AI` and `generatedAt = new Date()` after parsing. The model does not choose `source` or `generatedAt`.
- Use Spring AI via:
  ```java
  String raw = chatClientBuilder.build()
          .prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .call()
          .content();
  ```
  Keep the behavior to one system instruction, one user prompt, and plain string content.
- Parse JSON with `ObjectMapper.readValue(raw, AiReportResponse.class)` where `AiReportResponse` is a private record without `source` and `generatedAt`.
- Add a package-private helper for deterministic unit testing:
  ```java
  PerformanceAiReport parseAiReportResponse(String raw)
  ```
  This helper parses model JSON, sets `source = PerformanceAiReportSource.AI`, sets `generatedAt = new Date()`, and normalizes null lists to empty lists.
- Fallback rules:
  - `overallStatus` comes from `result.getPerfStatus()`: `COMPLETED_PASSED` maps to `PASSED`, `COMPLETED_FAILED` maps to `FAILED`, `STOPPED` maps to `STOPPED`, `ERROR` maps to `ERROR`; all other values map to their enum name or `UNKNOWN`.
  - `executiveSummary` says whether the run passed thresholds, failed thresholds, stopped, or errored.
  - `goodPoints` includes success rate and throughput when thresholds did not flag them.
  - `badPoints` includes each threshold failure reason and error count when present.
  - `risks` includes `"Ortam metrikleri bulunmadigi icin altyapi kaynakli kok neden analizi sinirlidir."` when environment metrics are missing or `metricsAvailable=false`.
  - `recommendedActions` includes at least one concrete action for the problem step or slowest step, one for P95/P99 threshold failures when present, and one for error-heavy steps when errors exist.
  - `technicalDetails` summarizes average, P95, P99, throughput, error rate, slowest step, and highest P95/P99 step when available.
- Create unit tests in `PerformanceAiReportServiceTest`:
  - `parseAiReportResponseReturnsAiSourceWhenJsonParses`: call package-private `parseAiReportResponse(...)` with valid JSON and assert `source=AI`, `generatedAt` is not null, and list fields are populated.
  - `generateReportReturnsFallbackWhenAiThrows`: instantiate the service with a mocked `ChatClient.Builder` whose `build()` throws `RuntimeException("AI unavailable")`; assert `source=FALLBACK`.
  - `fallbackReportIncludesThresholdFailuresAndActions`: build a `PerfRsltEntity` with failed threshold reasons and assert `source=FALLBACK`, non-empty `badPoints`, non-empty `recommendedActions`, and risks mention missing environment metrics.

**Done When:**
- `PerformanceAiReportService.generateReport(...)` returns `source=AI` for valid AI JSON.
- The same method returns a usable `source=FALLBACK` report if AI call or JSON parsing fails.
- Fallback report contains executive summary, status, good/bad points, risks, recommended actions, technical details, source, and generated timestamp.

**Verification:**
- Manual: run `rg -n "class PerformanceAiReportService|generateReport|fallbackReport|PerformanceAiReportSource.AI|PerformanceAiReportSource.FALLBACK" Apihub/src/main/java/etiya/omniAutomation/service Apihub/src/test/java/etiya/omniAutomation/service`.
- Automated: if Maven is available, run `mvn -Dtest=PerformanceAiReportServiceTest test` from `Apihub`; expected result is all tests passing.

---

### TASK-03: Persist AI Report And Include It In Analysis And Export

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3 - Power

**Implementation Notes:**
- In `ApiCallServiceImpl`, inject `PerformanceAiReportService performanceAiReportService`.
- In `persistPerformanceResult(...)`, after `PerformanceEnvironmentMetrics environmentMetrics = PerformanceAnalysisBuilder.unavailableEnvironmentMetrics();` and before final save, call:
  ```java
  entity.setAiReport(performanceAiReportService.generateReport(entity, runningItem));
  ```
  Set these fields on `entity` before generating the report:
  - `perfStatus`
  - `summary`
  - `runSummary`
  - `thresholdResult`
  - `analysisSummary`
  - `errorAnalysis`
  - `environmentMetrics`
  Then call report generation and save.
- Preserve baseline comparison behavior. If `performanceBaselineService.applyAutomaticBaselineComparison(saved)` updates baseline comparison after the first save, regenerate the report once after baseline comparison is applied so the saved report can mention baseline data:
  ```java
  PerfRsltEntity compared = performanceBaselineService.applyAutomaticBaselineComparison(saved);
  compared.setAiReport(performanceAiReportService.generateReport(compared, runningItem));
  compared.setValidationChecklist(performanceValidationChecklistBuilder.build(compared, runningItem));
  performanceResultRepository.save(compared);
  ```
  This means AI generation can run twice only when needed by the current save flow; both calls must still be protected by fallback behavior in the service.
- In `PerformanceService.getAnalysis(...)`, update the `new PerformanceExportPayload(...)` constructor call to include `result.getAiReport()` before step summaries.
- In `PerformanceService.toSummaryResult(...)`, update the `new PerformanceSummaryResult(...)` constructor call to include `item.getAiReport()` before `item.getSummary()`.
- In `PerformanceExportService.buildPayload(...)`, include `result.getAiReport()` before `result.getSummary()`.
- In `PerformanceExportService.buildCsv(...)`, add an `appendAiReport(csv, payload == null ? null : payload.aiReport())` section after report metadata and before validation checklist.
- CSV AI report section format:
  ```text
  AI Report
  Field,Value
  Overall Status,<value>
  Source,<value>
  Executive Summary,<value>
  Business Impact,<value>
  Good Points,<joined with " | ">
  Bad Points,<joined with " | ">
  Risks,<joined with " | ">
  Recommended Actions,<joined with " | ">
  Technical Details,<value>
  ```
  Use the existing `row(...)` and `escape(...)` helpers.
- Update `PerformanceExportServiceTest`:
  - In `entity()`, set `entity.setAiReport(new PerformanceAiReport(...))`.
  - Add assertion in `buildsJsonPayloadFromEntityAndThreadDetail` that `payload.aiReport()` equals `entity.getAiReport()`.
  - Add assertion in CSV test that output contains `"AI Report"` and the executive summary text.

**Done When:**
- Completed performance results save `aiReport`.
- `/performance/analysis` payload includes `aiReport`.
- `/performance/getHistory` rows include `aiReport`.
- JSON export includes `aiReport`.
- CSV export has an `AI Report` section.

**Verification:**
- Manual: run `rg -n "setAiReport|getAiReport|aiReport\\(\\)|AI Report" Apihub/src/main/java/etiya/omniAutomation/service Apihub/src/test/java/etiya/omniAutomation/service`.
- Automated: if Maven is available, run `mvn -Dtest=PerformanceExportServiceTest test` from `Apihub`; expected result is all tests passing.

---

### TASK-04: Wire Performance Timeout Into Request Execution

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallRequestOptions.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/WebClientService.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)

**Model Tier:** T3 - Power

**Implementation Notes:**
- Create `ApiCallRequestOptions` in package `etiya.omniAutomation.service`:
  ```java
  public record ApiCallRequestOptions(Integer timeoutMs) {
      public boolean hasTimeout() {
          return timeoutMs != null && timeoutMs > 0;
      }
  }
  ```
- In `WebClientService`, keep the current public `exchange(...)` method unchanged for existing callers.
- Add an overload:
  ```java
  public <T> ResponseEntity<T> exchange(
          String url,
          HttpEntity<?> httpEntity,
          HttpHeaders headers,
          HttpMethod httpMethod,
          ParameterizedTypeReference<T> typeReference,
          ApiCallRequestOptions options
  )
  ```
- Refactor the shared WebClient request construction into a private method or duplicate the small block; behavior must remain identical except the final blocking call:
  - without timeout: `.block()`
  - with timeout: `.block(Duration.ofMillis(options.timeoutMs()))`
- Catch Reactor timeout exceptions at `ApiCallServiceImpl.sendApiInformationXML(...)` level and return `ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Request timed out after " + timeoutMs + " ms")`.
- In `ApiCallServiceImpl`, keep current method signature:
  ```java
  public ResponseEntity<String> sendApiInformationXML(ApiInformationDto apiInformation, ProcessFlowStepDto processFlowStep, boolean fromTest, Long projectId, Map<String, String> parameterContext)
  ```
  Make it delegate to a new overload:
  ```java
  public ResponseEntity<String> sendApiInformationXML(
          ApiInformationDto apiInformation,
          ProcessFlowStepDto processFlowStep,
          boolean fromTest,
          Long projectId,
          Map<String, String> parameterContext,
          ApiCallRequestOptions options
  )
  ```
- Existing non-performance calls must keep using the no-options method.
- In `processPerformanceTask(...)`, call the new overload:
  ```java
  new ApiCallRequestOptions(performanceResultDto.getTimeoutMs())
  ```
- For gRPC steps inside the performance path, preserve existing behavior if `timeoutMs` is empty. If `timeoutMs` is positive, wrap `sendApiInformationGRPC(url, apiInformation)` with `CompletableFuture.supplyAsync(...)` and return HTTP 504 if `.get(timeoutMs, TimeUnit.MILLISECONDS)` times out. On timeout, cancel the future with `cancel(true)`.
- Do not apply `environmentBaseUrl` as a URL override in this task.

**Done When:**
- Performance HTTP calls use configured `timeoutMs`.
- Performance gRPC calls return timeout response when configured timeout elapses.
- Non-performance API calls still compile against the original `sendApiInformationXML(...)` and `WebClientService.exchange(...)` signatures.
- Timeout failures become failed performance samples through existing non-2xx handling.

**Verification:**
- Manual: run `rg -n "ApiCallRequestOptions|Duration.ofMillis|GATEWAY_TIMEOUT|sendApiInformationXML\\(" Apihub/src/main/java/etiya/omniAutomation/service`.
- Automated: if Maven is available, run `mvn -DskipTests compile` from `Apihub`; expected result is successful compilation.

---

### TASK-05: Add Frontend AI Report Types And Panel

**Targets:**
- `apihub-fe/types/performance.ts` (modify)
- `apihub-fe/components/performance/PerformanceAiReportPanel.tsx` (create)
- `apihub-fe/messages/tr.json` (modify)
- `apihub-fe/messages/en.json` (modify)

**Model Tier:** T2 - Balanced

**Implementation Notes:**
- In `types/performance.ts`, add:
  ```ts
  export type PerformanceAiReportSource = 'AI' | 'FALLBACK';

  export interface PerformanceAiReport {
      executiveSummary?: string | null;
      overallStatus?: string | null;
      businessImpact?: string | null;
      goodPoints?: string[] | null;
      badPoints?: string[] | null;
      risks?: string[] | null;
      recommendedActions?: string[] | null;
      technicalDetails?: string | null;
      source?: PerformanceAiReportSource | null;
      generatedAt?: string | null;
  }
  ```
- Add `aiReport?: PerformanceAiReport | null;` to:
  - `PerformanceExportPayload`
  - `PerformanceResultDto`
  - `PerformanceHistoryItem`
- Create `PerformanceAiReportPanel.tsx` with props:
  ```ts
  interface PerformanceAiReportPanelProps {
      report?: PerformanceAiReport | null;
  }
  ```
- The panel behavior:
  - If `!report`, render MUI `Alert` severity `info` with translation key `reportNotGenerated`.
  - Show a top row with `overallStatus`, `source`, and formatted `generatedAt`.
  - Show `executiveSummary` and `businessImpact` as short text sections.
  - Show four list sections: `goodPoints`, `badPoints`, `risks`, `recommendedActions`.
  - Show `technicalDetails` below the executive section.
  - Use existing `dash(...)` and `formatDateTime(...)` helpers from `PerformanceMetricFormatters`.
  - Use restrained MUI layout: `Box`, `Paper`, `Typography`, `Chip`, `Alert`, no nested cards.
- Add translation keys under `performance` in both message files:
  - `report`
  - `aiReport`
  - `executiveSummary`
  - `businessImpact`
  - `goodPoints`
  - `badPoints`
  - `risks`
  - `recommendedActions`
  - `technicalDetails`
  - `reportSource`
  - `generatedAt`
  - `reportNotGenerated`
  Use clear Turkish labels in `tr.json`, and English equivalents in `en.json`.

**Done When:**
- Frontend type definitions can represent backend `aiReport`.
- `PerformanceAiReportPanel` renders missing reports, AI reports, and fallback reports.
- Translation keys used by the panel exist in both locale files.

**Verification:**
- Manual: run `rg -n "PerformanceAiReport|aiReport|reportNotGenerated|executiveSummary" apihub-fe/types/performance.ts apihub-fe/components/performance/PerformanceAiReportPanel.tsx apihub-fe/messages`.
- Automated: run `npx eslint components/performance/PerformanceAiReportPanel.tsx types/performance.ts` from `apihub-fe`; expected result is no ESLint errors.

---

### TASK-06: Share Performance Screen Across Routes And Add Report Tab

**Targets:**
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` (create)
- `apihub-fe/app/dashboard/performance/page.tsx` (modify)
- `apihub-fe/app/[projectShortCode]/performance/page.tsx` (modify)
- `apihub-fe/messages/tr.json` (modify)
- `apihub-fe/messages/en.json` (modify)

**Model Tier:** T4 - Reasoning

**Implementation Notes:**
- Create `PerformanceTestsContent.tsx` as a client component.
- Move the current upgraded performance page logic from `app/dashboard/performance/page.tsx` into `PerformanceTestsContent`.
- `PerformanceTestsContent` props:
  ```ts
  interface PerformanceTestsContentProps {
      projectShortCode?: string;
      useDashboardProjectContext?: boolean;
  }
  ```
- Project selection behavior inside `PerformanceTestsContent`:
  - If `useDashboardProjectContext !== false`, use `useProject()` and the selected dashboard project exactly as the current dashboard page does.
  - If `useDashboardProjectContext === false`, load the project with `projectService.getByShortCode(projectShortCode)` and use that project as the selected project.
  - If project loading fails for the project route, show an MUI `Alert` with `performance.projectLoadFailed`.
- Keep the existing flow/environment/history/test-run behavior from the dashboard page.
- Add `aiReport` to `runningItems` mapping:
  ```ts
  aiReport: result.aiReport ?? null
  ```
- In detail data derivation, add:
  ```ts
  const detailAiReport = analysisData?.aiReport ?? selectedHistoryItem?.aiReport ?? selectedResult?.aiReport ?? null;
  ```
- Insert `Rapor` as the first tab:
  - tab index `0`: `PerformanceAiReportPanel report={detailAiReport}`
  - existing `Analiz` becomes index `1`
  - existing tab index checks shift by `+1`
- Import and use `PerformanceAiReportPanel`.
- `app/dashboard/performance/page.tsx` becomes a thin wrapper:
  - keep `DashboardLayout`, the page title, and `FloatingChat` in this wrapper.
  - `PerformanceTestsContent` renders only the performance content body between the title and floating chat.
  - pass `useDashboardProjectContext={true}`.
- `app/[projectShortCode]/performance/page.tsx` must stop using legacy `PerformanceRunner` and `PerformanceResultsGrid`.
  - Render `PerformanceTestsContent projectShortCode={params.projectShortCode} useDashboardProjectContext={false}`.
  - Keep this route's existing `Container`, page heading, floating chat button, and `AiChatDialog`.
  - Do not render `FloatingChat` inside `PerformanceTestsContent`.
- Add locale key `projectLoadFailed` in both message files.
- Do not delete legacy `PerformanceRunner.tsx` or `PerformanceResultGrid.tsx` in this task; they may still be referenced elsewhere.

**Done When:**
- Both `/dashboard/performance` and `/[projectShortCode]/performance` use the upgraded performance screen.
- Legacy empty result grid is not rendered by `[projectShortCode]/performance`.
- Detail modal shows `Rapor` as the first tab.
- The report tab renders saved `aiReport` when available and the missing-report message when unavailable.

**Verification:**
- Manual: run `rg -n "PerformanceTestsContent|PerformanceAiReportPanel|detailAiReport|<Tab label=\\{t\\('report'\\)" apihub-fe/app apihub-fe/components/performance`.
- Manual: run `rg -n "PerformanceRunner|PerformanceResultsGrid" apihub-fe/app/[projectShortCode]/performance/page.tsx`; expected result is no matches.
- Automated: run `npx eslint app/dashboard/performance/page.tsx app/[projectShortCode]/performance/page.tsx components/performance/PerformanceTestsContent.tsx` from `apihub-fe`; expected result is no ESLint errors.

---

### TASK-07: Add Active Threads Over Time Chart

**Targets:**
- `apihub-fe/components/performance/PerformanceChartsPanel.tsx` (modify)

**Model Tier:** T2 - Balanced

**Implementation Notes:**
- Keep existing `Response Time Over Time`, `Throughput / Error Rate Over Time`, and `Step P95 / P99` charts.
- Add active-thread time series derived from `PerformanceDetailResponse`.
- Define local interface:
  ```ts
  interface ActiveThreadRow {
      time: string;
      activeThreads: number;
  }
  ```
- Add helper:
  ```ts
  function buildActiveThreadSeries(detail: PerformanceDetailResponse | null): ActiveThreadRow[]
  ```
- Build algorithm:
  - For every step with both `startedAt` and `finishedAt`, create two events:
    - `{ timestamp: startedAt, delta: 1 }`
    - `{ timestamp: finishedAt, delta: -1 }`
  - Sort events by timestamp ascending. For equal timestamps, process `+1` before `-1` so zero-duration steps still show activity.
  - Walk events accumulating `activeThreads = Math.max(0, activeThreads + delta)`.
  - Return rows with `time: new Date(timestamp).toLocaleTimeString('tr-TR')`.
- Render a new line chart titled `Active Threads Over Time` when `activeThreadSeries.length > 0`.
- Use `ResponsiveContainer`, `LineChart`, `CartesianGrid`, `XAxis`, `YAxis`, `Tooltip`, `Legend`, and a single `Line` with `dataKey="activeThreads"`.
- If no active-thread series exists, do not add a second unavailable alert; the existing time-series unavailable alert is enough for empty detail data.

**Done When:**
- Performance charts include an active-thread chart when started/finished timestamps exist.
- Existing charts continue to render from their current data.
- Empty detail data still shows only the existing time-series unavailable message.

**Verification:**
- Manual: run `rg -n "ActiveThreadRow|buildActiveThreadSeries|Active Threads Over Time|activeThreads" apihub-fe/components/performance/PerformanceChartsPanel.tsx`.
- Automated: run `npx eslint components/performance/PerformanceChartsPanel.tsx` from `apihub-fe`; expected result is no ESLint errors.

---

### TASK-08: Run Feature Verification And Report Residual Build Issues

**Targets:**
- `docs/plans/2026-07-23-performance-ai-report.md` (modify only if verification notes need a correction)

**Model Tier:** T2 - Balanced

**Implementation Notes:**
- This task does not implement feature code. It verifies the integrated result after TASK-01 through TASK-07 have been completed.
- Run frontend targeted lint:
  ```powershell
  npx eslint app/dashboard/performance/page.tsx app/[projectShortCode]/performance/page.tsx components/performance/*.tsx services/performanceService.ts types/performance.ts
  ```
- Run frontend build:
  ```powershell
  npm run build
  ```
- If `npm run build` fails only because of `.next/dev/types/validator.ts:325`, report it as an existing generated Next cache issue, not as a performance feature source error.
- If Maven is available, run backend tests:
  ```powershell
  mvn -Dtest=PerformanceAiReportServiceTest,PerformanceExportServiceTest test
  ```
- If `mvn` is not available, report that backend tests could not be executed because Maven is missing from PATH.
- Use manual grep checks to confirm all surfaces:
  ```powershell
  rg -n "aiReport|PerformanceAiReport|AI Report" Apihub/src/main/java Apihub/src/test/java apihub-fe
  rg -n "ApiCallRequestOptions|GATEWAY_TIMEOUT|Duration.ofMillis" Apihub/src/main/java/etiya/omniAutomation/service
  rg -n "Active Threads Over Time|PerformanceTestsContent|PerformanceAiReportPanel" apihub-fe
  ```

**Done When:**
- Targeted frontend lint has been run and result is known.
- Frontend build has been run and result is known.
- Backend test status is known: passed, failed with details, or not runnable because Maven is unavailable.
- Manual grep checks confirm AI report, timeout, shared route, and active-thread chart surfaces.

**Verification:**
- Manual: report the exact command results to the user, including any failure line and whether it is feature code or pre-existing generated/cache state.
- Automated: no additional automated check belongs in this verification task.
