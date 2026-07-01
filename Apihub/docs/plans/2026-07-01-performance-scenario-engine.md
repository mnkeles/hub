# Performance Scenario Engine - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Add an optional scenario-model performance runner that can execute weighted multi-flow tests while preserving the existing simple single-flow runner.

**Architecture:** `PerformanceRequest.scenario` selects a new scenario execution path; simple requests continue through the existing path. Scenario planning, context, retry, execution, and result summaries are split into focused services so `ApiCallServiceImpl` only exposes a reusable single-virtual-user flow runner boundary.

**Tech / dependencies:** Spring Boot 3.5, Java 21 records/services, Hibernate JSONB, Liquibase, virtual-thread executor, JUnit 5, Next.js 16, React 19, MUI 7, TypeScript, next-intl. No new runtime dependency is required.

**File map:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetStrategy.java` - Dataset distribution enum.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceRetryPolicy.java` - Retry config DTO.
- `src/main/java/etiya/omniAutomation/request/PerformanceScenarioItemRequest.java` - Scenario item request DTO.
- `src/main/java/etiya/omniAutomation/request/PerformanceScenarioRequest.java` - Scenario request DTO.
- `src/main/java/etiya/omniAutomation/request/PerformanceRequest.java` - Adds optional `scenario`.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioConfig.java` - Persisted scenario config snapshot.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioItemSummary.java` - Scenario item result summary.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioSummary.java` - Scenario run summary.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceRetrySummary.java` - Retry result summary.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetUsageSummary.java` - Dataset usage summary.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceCorrelationSummary.java` - Correlation summary.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioExecutionPlan.java` - Planned virtual-user allocation.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioPlannedItem.java` - Planned scenario item allocation.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioVirtualUserPlan.java` - Per-virtual-user execution plan.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceResultItemDto.java` - Adds optional scenario and retry metadata to stored samples.
- `src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` - Adds scenario JSONB fields.
- `src/main/resources/db/changelog/changes/liquibase-migration-file.xml` - Adds scenario JSONB columns.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` - Adds scenario summaries to analysis/export payload.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` - Adds scenario summaries to result DTO.
- `src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` - Adds scenario summaries to history payload.
- `src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` - Maps scenario fields.
- `src/main/java/etiya/omniAutomation/service/PerformanceDatasetResolver.java` - Validates dataset references for the current build.
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioPlanner.java` - Validates and plans scenario execution.
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioPlannerTest.java` - Planner and validation tests.
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioContextManager.java` - Manages virtual-user, iteration, and correlation context.
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioContextManagerTest.java` - Context/correlation tests.
- `src/main/java/etiya/omniAutomation/service/PerformanceRetryExecutor.java` - Executes retry policy around step calls.
- `src/test/java/etiya/omniAutomation/service/PerformanceRetryExecutorTest.java` - Retry tests.
- `src/main/java/etiya/omniAutomation/service/PerformanceSingleVirtualUserFlowRunner.java` - Reusable single virtual-user flow execution extracted from `ApiCallServiceImpl`.
- `src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` - Delegates simple flow tasks to the reusable runner and keeps final persistence.
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioExecutor.java` - Executes planned scenario virtual users.
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioResultBuilder.java` - Builds scenario, retry, dataset, and correlation summaries.
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioResultBuilderTest.java` - Scenario summary tests.
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` - Chooses simple or scenario execution path.
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` - Adds scenario summaries to analysis/export payload.
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` - Export payload compatibility tests.
- `../../apihub-fe/apihub-fe/types/performance.ts` - Frontend scenario request and summary types.
- `../../apihub-fe/apihub-fe/messages/tr.json` - Turkish scenario UI labels.
- `../../apihub-fe/apihub-fe/messages/en.json` - English scenario UI labels.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioBuilder.tsx` - Scenario configuration form.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioDistributionPreview.tsx` - Weight/thread/sample preview.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioValidationPanel.tsx` - Scenario validation warnings/errors.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioSummaryPanel.tsx` - Scenario result detail panel.
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` - Adds simple/scenario mode, request wiring, and scenario detail tab.

---

### TASK-01: Scenario Request DTOs And Persistence Contract

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetStrategy.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceRetryPolicy.java` (create)
- `src/main/java/etiya/omniAutomation/request/PerformanceScenarioItemRequest.java` (create)
- `src/main/java/etiya/omniAutomation/request/PerformanceScenarioRequest.java` (create)
- `src/main/java/etiya/omniAutomation/request/PerformanceRequest.java` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioConfig.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioItemSummary.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioSummary.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceRetrySummary.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetUsageSummary.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceCorrelationSummary.java` (create)
- `src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` (modify)
- `src/main/resources/db/changelog/changes/liquibase-migration-file.xml` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` (modify)
- `src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` (modify)
- `src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create enum:
  ```java
  public enum PerformanceDatasetStrategy {
      NONE,
      SEQUENTIAL,
      RANDOM,
      UNIQUE_PER_THREAD
  }
  ```
- Create record:
  ```java
  public record PerformanceRetryPolicy(
          Boolean enabled,
          Integer maxRetries,
          Integer retryDelayMs,
          Boolean retryOnTimeout,
          Boolean retryOn5xx,
          Boolean retryOnAssertionFailure
  ) {
      public boolean isEnabled() {
          return Boolean.TRUE.equals(enabled);
      }
  }
  ```
- Create request classes with Lombok `@Getter @Setter` to match existing request style:
  ```java
  public class PerformanceScenarioItemRequest {
      private Long processFlowId;
      private Double weightPercent;
      private Boolean enabled = true;
      private Integer loopCount;
      private Integer thinkTimeMs;
      private Integer timeoutMs;
      private Boolean continueOnError = false;
      private Long datasetId;
      private PerformanceDatasetStrategy datasetStrategy = PerformanceDatasetStrategy.NONE;
      private PerformanceRetryPolicy retryPolicy;
  }

  public class PerformanceScenarioRequest {
      private String name;
      private String description;
      private Integer totalThreads;
      private Integer globalLoopCount;
      private Integer globalThinkTimeMs;
      private Integer globalTimeoutMs;
      private PerformanceDatasetStrategy datasetStrategy = PerformanceDatasetStrategy.NONE;
      private List<PerformanceScenarioItemRequest> items;
  }
  ```
- Add `private PerformanceScenarioRequest scenario;` to `PerformanceRequest`.
- Create persisted config/summary records:
  ```java
  public record PerformanceScenarioConfig(
          String name,
          String description,
          Integer totalThreads,
          Integer globalLoopCount,
          Integer globalThinkTimeMs,
          Integer globalTimeoutMs,
          PerformanceDatasetStrategy datasetStrategy,
          List<PerformanceScenarioItemRequest> items
  ) {}

  public record PerformanceScenarioItemSummary(
          Long processFlowId,
          String flowShortCode,
          double weightPercent,
          int allocatedThreads,
          int loopCount,
          long sampleCount,
          long successCount,
          long failureCount,
          double errorRate,
          double averageMs,
          double p95Ms,
          double p99Ms,
          double throughputPerSecond
  ) {}

  public record PerformanceScenarioSummary(
          String scenarioName,
          int totalThreads,
          int enabledItemCount,
          long plannedSampleCount,
          long actualSampleCount,
          List<PerformanceScenarioItemSummary> items,
          List<String> warnings
  ) {}

  public record PerformanceRetrySummary(
          boolean enabled,
          long totalRetryAttempts,
          long recoveredRequestCount,
          long failedAfterRetryCount,
          Map<String, Long> retryByStep,
          Map<String, Long> retryByReason
  ) {}

  public record PerformanceDatasetUsageSummary(
          long datasetsUsed,
          PerformanceDatasetStrategy strategy,
          long rowsConsumed,
          long duplicateRowCount,
          List<String> missingDatasetWarnings
  ) {}

  public record PerformanceCorrelationSummary(
          long extractedValueCount,
          long missingValueCount,
          long failedExtractorCount,
          List<String> warnings
  ) {}
  ```
- Add JSONB columns to Liquibase with preconditions:
  - `scenario_config`
  - `scenario_summary`
  - `retry_summary`
  - `dataset_usage_summary`
  - `correlation_summary`
- Add matching JSONB fields to `PerfRsltEntity`.
- Extend `PerformanceExportPayload`, `PerformanceResultDto`, and `PerformanceSummaryResult` with:
  ```java
  PerformanceScenarioConfig scenarioConfig,
  PerformanceScenarioSummary scenarioSummary,
  PerformanceRetrySummary retrySummary,
  PerformanceDatasetUsageSummary datasetUsageSummary,
  PerformanceCorrelationSummary correlationSummary
  ```
- Update `PerformanceResultMapper`, `PerformanceService.toSummaryResult(...)`, and `PerformanceExportService.buildPayload(...)` to pass through the new fields.
- Update tests and constructor calls with `null` for these fields.

**Done When:**
- Scenario request and summary DTOs compile.
- `PerformanceRequest` accepts optional scenario config.
- `perf_rslt` has idempotent migration entries for scenario JSONB fields.
- Existing simple export payload construction compiles and includes nullable scenario fields.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceExportServiceTest test`. Expected: tests pass.

---

### TASK-02: Scenario Planner And Dataset Validation

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioExecutionPlan.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioPlannedItem.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceScenarioVirtualUserPlan.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceDatasetResolver.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioPlanner.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioPlannerTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- Create plan records:
  ```java
  public record PerformanceScenarioPlannedItem(
          int itemIndex,
          Long processFlowId,
          String flowShortCode,
          double normalizedWeightPercent,
          int allocatedThreads,
          int loopCount,
          Integer thinkTimeMs,
          Integer timeoutMs,
          boolean continueOnError,
          Long datasetId,
          PerformanceDatasetStrategy datasetStrategy,
          PerformanceRetryPolicy retryPolicy
  ) {}

  public record PerformanceScenarioVirtualUserPlan(
          int virtualUserNumber,
          List<PerformanceScenarioPlannedItem> items
  ) {}

  public record PerformanceScenarioExecutionPlan(
          PerformanceScenarioConfig scenarioConfig,
          int totalThreads,
          long plannedSampleCount,
          List<PerformanceScenarioPlannedItem> plannedItems,
          List<PerformanceScenarioVirtualUserPlan> virtualUsers,
          List<String> warnings
  ) {}
  ```
- Create `@Service class PerformanceDatasetResolver`.
  - Add method:
    ```java
    public boolean canResolve(Long datasetId) {
        return datasetId == null;
    }
    ```
  - This intentionally rejects non-null dataset references until a dataset source is connected. The planner must fail validation for non-null `datasetId`.
- Create `@Service class PerformanceScenarioPlanner`.
- Add method:
  ```java
  public PerformanceScenarioExecutionPlan plan(
          PerformanceScenarioRequest scenario,
          Map<Long, ProcessFlowDto> flowsById
  )
  ```
- Validation:
  - `scenario` cannot be null.
  - `totalThreads` must be greater than 0.
  - At least one item with `enabled != false` is required.
  - Each enabled item requires `processFlowId`.
  - Each process flow ID must exist in `flowsById`.
  - Weight must be positive for enabled items.
  - If weight total is not 100, normalize each enabled item as `itemWeight / totalWeight * 100`.
  - If any enabled item has `datasetStrategy != NONE` and `datasetId == null`, throw `IllegalArgumentException("Dataset strategy requires datasetId.")`.
  - If `datasetId != null` and `PerformanceDatasetResolver.canResolve(datasetId)` is false, throw `IllegalArgumentException("Dataset source is not configured for datasetId: " + datasetId)`.
  - Retry caps:
    - `maxRetries` cannot exceed 3.
    - `retryDelayMs` cannot exceed 10000.
    - Violations throw `IllegalArgumentException`.
- Thread allocation:
  - Allocate `Math.floor(totalThreads * normalizedWeight / 100)` to each item.
  - Distribute remaining threads one by one to items with largest fractional remainder.
  - Every enabled item gets at least 1 thread when `totalThreads >= enabledItemCount`.
  - When enabled item count exceeds total threads, assign 1 thread to the highest-weight items until threads run out and add warning `"Some scenario items received zero threads because totalThreads is lower than enabled item count."`.
- Loop count:
  - Item loop count overrides global loop count.
  - Missing or invalid loop count becomes 1.
- Planned sample count:
  - For each item, `allocatedThreads * loopCount * flow.getProcessFlowStepRelations().size()`.
- Virtual user plan:
  - Create virtual users `0..totalThreads-1`.
  - Assign each virtual user one planned item according to allocated threads.
  - Cross-flow sequence is represented by multiple items on one virtual user only when a planned item explicitly appears more than once in a virtual user plan. The first version uses one item per allocated virtual user.
- Add tests:
  - `distributesWeightsAcrossThreads`
  - `normalizesWeightsWhenTotalIsNotOneHundred`
  - `rejectsScenarioWithoutEnabledItems`
  - `rejectsMissingFlow`
  - `rejectsDatasetStrategyWithoutDataset`
  - `rejectsUnresolvableDataset`
  - `rejectsUnsafeRetryPolicy`
  - `estimatesPlannedSampleCount`

**Done When:**
- Planner returns deterministic item allocations and virtual-user plans.
- Planner rejects invalid flow, dataset, and retry inputs before execution.
- Unit tests cover allocation and validation behavior.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceScenarioPlannerTest test`. Expected: tests pass.

---

### TASK-03: Context Manager And Retry Executor

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioContextManager.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceRetryExecutor.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioContextManagerTest.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceRetryExecutorTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- Create `PerformanceScenarioContextManager`.
- Add inner class:
  ```java
  public static final class ScenarioExecutionContext {
      private final int virtualUserNumber;
      private final Map<String, String> virtualUserContext = new ConcurrentHashMap<>();
      private final Map<String, String> iterationContext = new ConcurrentHashMap<>();
      private final List<String> warnings = new CopyOnWriteArrayList<>();
      private final AtomicLong extractedValueCount = new AtomicLong();
      private final AtomicLong missingValueCount = new AtomicLong();
      private final AtomicLong failedExtractorCount = new AtomicLong();
      // getters
  }
  ```
- Add methods:
  ```java
  public ScenarioExecutionContext createContext(int virtualUserNumber);
  public void beginIteration(ScenarioExecutionContext context);
  public void putVirtualUserValue(ScenarioExecutionContext context, String key, String value);
  public Optional<String> getValue(ScenarioExecutionContext context, String key);
  public void recordExtractedValue(ScenarioExecutionContext context, String key, String value);
  public void recordMissingValue(ScenarioExecutionContext context, String key);
  public void recordExtractorFailure(ScenarioExecutionContext context, String stepName, String message);
  public PerformanceCorrelationSummary buildSummary(Collection<ScenarioExecutionContext> contexts);
  ```
- `beginIteration` clears only iteration context.
- `getValue` checks iteration context first, then virtual user context.
- Create `PerformanceRetryExecutor`.
- Add records:
  ```java
  public record RetryExecutionResult<T>(
          T result,
          int retryAttempts,
          boolean recovered,
          boolean failedAfterRetry,
          String retryReason
  ) {}

  @FunctionalInterface
  public interface RetryableOperation<T> {
      T execute() throws Exception;
  }
  ```
- Add method:
  ```java
  public <T> RetryExecutionResult<T> execute(
          PerformanceRetryPolicy retryPolicy,
          RetryableOperation<T> operation,
          Predicate<T> successPredicate,
          Function<T, String> failureReasonExtractor
  )
  ```
- Retry rules:
  - If policy is null or not enabled, execute once.
  - Retry at most `maxRetries`.
  - Sleep `retryDelayMs` between attempts when delay is positive.
  - For thrown exceptions, use reason `"TIMEOUT"` when exception class name or message contains `"timeout"` case-insensitively, otherwise `"EXCEPTION"`.
  - For non-success results, use `failureReasonExtractor`.
  - Retry only if reason matches enabled policy:
    - `"TIMEOUT"` -> `retryOnTimeout`
    - reason starts with `"5"` or contains `"HTTP 5"` -> `retryOn5xx`
    - `"ASSERTION"` -> `retryOnAssertionFailure`
  - `recovered=true` when first attempt failed and a later attempt succeeded.
  - `failedAfterRetry=true` when attempts were retried and final result is still failed or final exception is thrown.
- Add tests:
  - `contextKeepsVirtualUserValuesAcrossIterations`
  - `contextClearsIterationValues`
  - `contextBuildsCorrelationSummary`
  - `retryExecutorReturnsFirstSuccessWithoutRetry`
  - `retryExecutorRecoversAfter5xx`
  - `retryExecutorDoesNotRetryDisabledReason`
  - `retryExecutorMarksFailedAfterRetry`

**Done When:**
- Context manager tracks same-virtual-user values and correlation counters.
- Retry executor returns retry metadata without depending on HTTP-specific classes.
- Unit tests cover context and retry rules.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceScenarioContextManagerTest,PerformanceRetryExecutorTest test`. Expected: tests pass.

---

### TASK-04: Extract Single Virtual User Flow Runner

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceSingleVirtualUserFlowRunner.java` (create)
- `src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceResultItemDto.java` (modify)
- `src/test/java/etiya/omniAutomation/service/impl/ApiCallServiceParallelCallTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/impl/ApiCallServiceImplTest.java` (modify)

**Model Tier:** T4

**Implementation Notes:**
- Add optional metadata fields to `PerformanceResultItemDto` with getters/setters:
  ```java
  private Integer scenarioItemIndex;
  private String scenarioName;
  private Long scenarioProcessFlowId;
  private String scenarioFlowShortCode;
  private Integer retryAttempt;
  private Boolean recoveredByRetry;
  private String retryReason;
  ```
- Create `@Service class PerformanceSingleVirtualUserFlowRunner`.
- Move the logic currently in `ApiCallServiceImpl.processPerformanceTask(...)` into this class, preserving behavior.
- Public method:
  ```java
  public void run(
          ProcessFlowDto processFlowDto,
          List<PerformanceResultItemDto> stepList,
          int threadNumber,
          int threadCount,
          int rampUpPeriodSeconds,
          PerformanceResultDto performanceResultDto,
          FlowRunOptions options
  )
  ```
- Add record:
  ```java
  public record FlowRunOptions(
          Integer loopCount,
          Integer durationSeconds,
          Integer thinkTimeMs,
          boolean continueOnError,
          Integer scenarioItemIndex,
          String scenarioName,
          Long scenarioProcessFlowId,
          String scenarioFlowShortCode
  ) {
      public static FlowRunOptions fromPerformanceResult(PerformanceResultDto dto) {
          return new FlowRunOptions(dto.getLoopCount(), dto.getDurationSeconds(), dto.getThinkTimeMs(), false, null, null, null, null);
      }
  }
  ```
- To avoid exposing unrelated methods, inject only the collaborators the runner needs:
  - `ObjectMapper`
  - `PerformanceRunRegistry`
  - `ApiCallServiceImpl` is not injected into the runner.
- Because the runner must call existing `processRequestBody(...)` and `sendApiInformationXML(...)`, make those methods package-private in `ApiCallServiceImpl` and inject `ApiCallServiceImpl` into the runner only for these two calls:
  ```java
  void processRequestBody(...)
  ResponseEntity<String> sendApiInformationXML(...)
  ```
  Keep method behavior unchanged.
- In `ApiCallServiceImpl`, replace the body of private `processPerformanceTask(...)` with:
  ```java
  singleVirtualUserFlowRunner.run(
          processFlowDto,
          stepList,
          threadNumber,
          threadCount,
          rampUpPeriodSeconds,
          performanceResultDto,
          PerformanceSingleVirtualUserFlowRunner.FlowRunOptions.fromPerformanceResult(performanceResultDto)
  );
  ```
- Preserve current cancellation, ramp-up, loop count, duration, think time, success/failure, header extraction, parameter extraction, and remaining-step failure behavior.
- When `options.continueOnError()` is true, do not mark remaining steps failed after a failed step; continue with the next step.
- Set scenario metadata fields on each sample when `options.scenarioItemIndex()` is non-null.
- Update impacted tests only for constructor injection changes.

**Done When:**
- Existing simple performance execution still delegates through `executeFlowPerformanceTest`.
- Single virtual-user execution logic is reusable by scenario executor.
- Scenario metadata fields can be stored on result items.
- Existing ApiCallService tests compile after constructor changes.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=ApiCallServiceParallelCallTest,ApiCallServiceImplTest test`. Expected: tests pass.

---

### TASK-05: Scenario Executor And Result Builder

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioExecutor.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceScenarioResultBuilder.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioResultBuilderTest.java` (create)

**Model Tier:** T4

**Implementation Notes:**
- Create `@Service class PerformanceScenarioExecutor`.
- Inject:
  - `ExecutorService virtualThreadExecutor`
  - `PerformanceRunRegistry`
  - `PerformanceSingleVirtualUserFlowRunner`
  - `PerformanceScenarioContextManager`
- Add method:
  ```java
  public void execute(
          PerformanceScenarioExecutionPlan plan,
          Map<Long, ProcessFlowDto> flowsById,
          PerformanceResultDto performanceResultDto
  )
  ```
- Execution:
  - Build a `ConcurrentHashMap<Integer, List<PerformanceResultItemDto>>` from `performanceResultDto.getThreadGroup()`.
  - For each `PerformanceScenarioVirtualUserPlan`, start one `CompletableFuture.runAsync(...)`.
  - Register futures with `PerformanceRunRegistry.register(performanceResultId, futures)`.
  - For each planned item in a virtual user:
    - Get a deep copy of the flow through the single virtual-user runner.
    - Use the virtual user's existing step list.
    - Call `singleVirtualUserFlowRunner.run(...)` with:
      - `threadNumber = virtualUserNumber`
      - `threadCount = plan.totalThreads()`
      - `rampUpPeriodSeconds = performanceResultDto.getRampUpPeriod()`
      - `FlowRunOptions` using item loop count, item think time, `continueOnError`, and scenario metadata.
  - On cancellation, let the runner mark active steps stopped.
  - Always unregister the run in `finally`.
- Create `@Service class PerformanceScenarioResultBuilder`.
- Add method:
  ```java
  public PerformanceScenarioSummary buildScenarioSummary(
          PerformanceScenarioExecutionPlan plan,
          PerformanceThreadGroup threadGroup,
          long totalDurationMs
  )
  ```
- Build item summaries by filtering flattened samples where `scenarioItemIndex` matches planned item index.
- Compute sample count, success count, failure count, error rate, average, P95, P99, and throughput using local helper methods matching `PerformanceMetricsCalculator` semantics.
- Add method:
  ```java
  public PerformanceRetrySummary buildRetrySummary(PerformanceThreadGroup threadGroup)
  ```
  Count samples where `retryAttempt > 0`, `recoveredByRetry == true`, failed samples with retry attempts, and group by `stepName` and `retryReason`.
- Add method:
  ```java
  public PerformanceDatasetUsageSummary buildDatasetUsageSummary(PerformanceScenarioExecutionPlan plan)
  ```
  First version:
  - `datasetsUsed` is count of distinct non-null dataset IDs in planned items.
  - `strategy` is the scenario config strategy.
  - `rowsConsumed=0`
  - `duplicateRowCount=0`
  - `missingDatasetWarnings` contains one message for each non-null dataset ID because dataset source is not configured by `PerformanceDatasetResolver`.
- Tests:
  - `buildsScenarioItemSummaries`
  - `buildsRetrySummary`
  - `buildsDatasetUsageSummary`

**Done When:**
- Scenario executor can run planned virtual users through the reusable flow runner.
- Scenario result builder produces scenario, retry, and dataset summaries from thread detail.
- Unit tests cover summary calculations.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceScenarioResultBuilderTest test`. Expected: tests pass.

---

### TASK-06: Wire Scenario Execution Into PerformanceService

**Targets:**
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/impl/ApiCallServiceTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T4

**Implementation Notes:**
- In `PerformanceService.executePerformanceTest(...)`, choose scenario first:
  ```java
  if (request.getScenario() != null) {
      return executeScenarioPerformance(request);
  }
  ```
- Add private `executeScenarioPerformance(PerformanceRequest request)`.
- Steps:
  1. Validate `projectId`, `environment`, and `scenario`.
  2. Create running result using existing `createRunningResult(request)`.
  3. Set `scenarioConfig` on the entity from request scenario.
  4. Load all enabled scenario flows with `processFlowService.findByIdWithRelations(processFlowId)`.
  5. For each flow, set project ID and system short code like simple flow path does.
  6. Call `PerformanceScenarioPlanner.plan(scenario, flowsById)`.
  7. Create running result items for scenario virtual users. Use one `PerformanceThread` per virtual user and pre-create samples for planned sample count per virtual user. If exact planned count is zero, create an empty step list.
  8. Map entity to `PerformanceResultDto`, set thread group and scenario fields.
  9. Start async scenario execution through `CompletableFuture.runAsync(() -> apiCallService.executeScenarioPerformanceTest(plan, flowsById, result))`.
  10. Return the running result.
- Add `ApiCallServiceImpl.executeScenarioPerformanceTest(...)`:
  ```java
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void executeScenarioPerformanceTest(
          PerformanceScenarioExecutionPlan plan,
          Map<Long, ProcessFlowDto> flowsById,
          PerformanceResultDto performanceResultDto
  )
  ```
- This method should mirror `executeFlowPerformanceTest(...)` final persistence:
  - Execute through `PerformanceScenarioExecutor`.
  - Build run summary and step summaries from flattened samples.
  - Build scenario, retry, dataset, and correlation summaries.
  - Evaluate thresholds and final status.
  - Build analysis, error analysis, environment metrics, baseline comparison, validation checklist.
  - Persist scenario summary fields on `PerfRsltEntity`.
  - Do not invoke AI insight/report snapshot generation in this scenario task. Scenario summaries must be persisted independently and remain readable by the current reporting path.
- Update `PerformanceExportService.buildPayload(...)` to include persisted scenario fields.
- Update tests for new constructor dependencies and payload assertions:
  - Existing simple export still has null scenario fields.
  - Scenario payload with persisted fields returns them unchanged.

**Done When:**
- Simple requests still follow the old path.
- Scenario requests create running results and start async scenario execution.
- Completed scenario runs persist scenario, retry, dataset, and correlation summaries.
- Stop/force-stop still use the same performance result ID and registry path.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=ApiCallServiceTest,PerformanceExportServiceTest,PerformanceScenarioPlannerTest,PerformanceScenarioResultBuilderTest test`. Expected: tests pass.

---

### TASK-07: Frontend Scenario Types And Labels

**Targets:**
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify)
- `../../apihub-fe/apihub-fe/messages/tr.json` (modify)
- `../../apihub-fe/apihub-fe/messages/en.json` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Add frontend types:
  ```ts
  export type PerformanceDatasetStrategy = 'NONE' | 'SEQUENTIAL' | 'RANDOM' | 'UNIQUE_PER_THREAD';

  export interface PerformanceRetryPolicy {
      enabled?: boolean | null;
      maxRetries?: number | null;
      retryDelayMs?: number | null;
      retryOnTimeout?: boolean | null;
      retryOn5xx?: boolean | null;
      retryOnAssertionFailure?: boolean | null;
  }

  export interface PerformanceScenarioItemRequest {
      processFlowId?: number | null;
      weightPercent?: number | null;
      enabled?: boolean | null;
      loopCount?: number | null;
      thinkTimeMs?: number | null;
      timeoutMs?: number | null;
      continueOnError?: boolean | null;
      datasetId?: number | null;
      datasetStrategy?: PerformanceDatasetStrategy | null;
      retryPolicy?: PerformanceRetryPolicy | null;
  }

  export interface PerformanceScenarioRequest {
      name?: string | null;
      description?: string | null;
      totalThreads?: number | null;
      globalLoopCount?: number | null;
      globalThinkTimeMs?: number | null;
      globalTimeoutMs?: number | null;
      datasetStrategy?: PerformanceDatasetStrategy | null;
      items?: PerformanceScenarioItemRequest[];
  }
  ```
- Add `scenario?: PerformanceScenarioRequest | null;` to `PerformanceRequest`.
- Add summary interfaces matching backend records:
  - `PerformanceScenarioConfig`
  - `PerformanceScenarioItemSummary`
  - `PerformanceScenarioSummary`
  - `PerformanceRetrySummary`
  - `PerformanceDatasetUsageSummary`
  - `PerformanceCorrelationSummary`
- Add nullable scenario summary fields to `PerformanceExportPayload`, `PerformanceResultDto`, and `PerformanceHistoryItem`.
- Add translation keys:
  - `testType`, `simpleTest`, `scenarioTest`, `scenarioSettings`, `scenarioName`, `scenarioDescription`, `totalThreads`, `globalLoopCount`, `globalThinkTime`, `globalTimeout`, `scenarioItems`, `addScenarioItem`, `removeScenarioItem`, `weightPercent`, `continueOnError`, `datasetStrategy`, `retryPolicy`, `maxRetries`, `retryDelayMs`, `retryOnTimeout`, `retryOn5xx`, `retryOnAssertionFailure`, `distributionPreview`, `allocatedThreads`, `estimatedSampleCount`, `validationIssues`, `scenarioSummary`, `retrySummary`, `datasetUsageSummary`, `correlationSummary`, `scenarioSummaryUnavailable`.
- Use Turkish and English values that match existing performance terminology.

**Done When:**
- Frontend request and response types include scenario fields.
- Translation files include all scenario builder/detail labels.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-08: Frontend Scenario Builder

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioDistributionPreview.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioValidationPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioBuilder.tsx` (create)

**Model Tier:** T3

**Implementation Notes:**
- Create `PerformanceScenarioDistributionPreview`.
- Props:
  ```ts
  interface Props {
      scenario: PerformanceScenarioRequest;
      processFlows: ProcessFlowDto[];
  }
  ```
- Compute enabled items, weight total, normalized weight, allocated threads, and estimated sample count in the component with pure helper functions.
- Estimated sample count per item:
  - `allocatedThreads * loopCount * stepCount`.
  - `loopCount` uses item loop count, then global loop count, then 1.
  - `stepCount` uses `flow.processFlowStepList?.length`, then `flow.processFlowStepRelations?.length`, then 1.
- Create `PerformanceScenarioValidationPanel`.
- Props:
  ```ts
  interface Props {
      scenario: PerformanceScenarioRequest;
      processFlows: ProcessFlowDto[];
  }
  ```
- Show validation issues:
  - no enabled item
  - missing flow
  - weight total <= 0
  - dataset strategy not `NONE` without dataset ID
  - retry max retries > 3
  - retry delay > 10000
  - total threads <= 0
- Create `PerformanceScenarioBuilder`.
- Props:
  ```ts
  interface Props {
      scenario: PerformanceScenarioRequest;
      processFlows: ProcessFlowDto[];
      onChange: (scenario: PerformanceScenarioRequest) => void;
  }
  ```
- Render:
  - scenario name
  - description
  - total threads
  - global loop count
  - global think time
  - global timeout
  - item list with flow select, weight percent, enabled switch, loop count, think time, timeout, continue-on-error switch, dataset ID number field, dataset strategy select, retry policy controls
  - add/remove item buttons
  - distribution preview
  - validation panel
- Use MUI controls. Keep cards at `borderRadius <= 1` or default MUI radius.
- Dataset select is represented by a numeric `datasetId` text field in this task because full dataset management UI is outside this scenario engine plan.

**Done When:**
- User can add/remove scenario items.
- User can configure flow, weight, overrides, dataset ID, dataset strategy, and retry policy per item.
- Preview and validation update from current form state.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-09: Wire Scenario Mode Into Performance Page

**Targets:**
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Add state:
  ```ts
  const [testType, setTestType] = useState<'simple' | 'scenario'>('simple');
  const [scenario, setScenario] = useState<PerformanceScenarioRequest>({
      name: '',
      description: '',
      totalThreads: 10,
      globalLoopCount: 1,
      globalThinkTimeMs: null,
      globalTimeoutMs: null,
      datasetStrategy: 'NONE',
      items: [],
  });
  ```
- Add a segmented control or MUI `Select` near the start form for `simple` vs `scenario`.
- When `testType === 'simple'`, render the existing simple fields exactly as before.
- When `testType === 'scenario'`, render `PerformanceScenarioBuilder`.
- In `handleRunTest`:
  - For simple mode, send the existing payload unchanged.
  - For scenario mode, send:
    ```ts
    {
        environment,
        processFlowId: null,
        projectId: selectedProject.projectId,
        rampUpPeriod,
        threadCount: scenario.totalThreads ?? threadCount,
        durationSeconds: optionalNumber(durationSeconds),
        loopCount: scenario.globalLoopCount ?? optionalNumber(loopCount),
        thinkTimeMs: scenario.globalThinkTimeMs ?? optionalNumber(thinkTimeMs),
        timeoutMs: scenario.globalTimeoutMs ?? optionalNumber(timeoutMs),
        environmentBaseUrl: environmentBaseUrl || null,
        thresholdPreset,
        maxErrorRatePercent: thresholdConfig.maxErrorRatePercent ?? null,
        maxAverageMs: thresholdConfig.maxAverageMs ?? null,
        maxP95Ms: thresholdConfig.maxP95Ms ?? null,
        maxP99Ms: thresholdConfig.maxP99Ms ?? null,
        minThroughputPerSecond: thresholdConfig.minThroughputPerSecond ?? null,
        scenario
    }
    ```
  - Before sending scenario mode, validate the same conditions shown in `PerformanceScenarioValidationPanel`; if invalid, set `error` and return.
- When mapping `runningItems` and detail data, include scenario summary fields.
- Do not remove existing history, compare, baseline, validation, live monitor, or export behavior.

**Done When:**
- User can switch between simple and scenario test modes.
- Simple test request remains unchanged.
- Scenario test request includes `scenario`.
- Invalid scenario is blocked before API call with a clear message.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-10: Scenario Result Detail Panel

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceScenarioSummaryPanel.tsx` (create)
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Create `PerformanceScenarioSummaryPanel`.
- Props:
  ```ts
  interface Props {
      scenarioSummary?: PerformanceScenarioSummary | null;
      retrySummary?: PerformanceRetrySummary | null;
      datasetUsageSummary?: PerformanceDatasetUsageSummary | null;
      correlationSummary?: PerformanceCorrelationSummary | null;
  }
  ```
- Render:
  - scenario name, total threads, planned sample count, actual sample count, warnings
  - scenario item summary table with flow, weight, allocated threads, loop count, sample count, success, failure, error rate, average, P95, P99, throughput
  - retry summary with total retry attempts, recovered count, failed-after-retry count, retry by step and reason
  - dataset usage summary with datasets used, strategy, rows consumed, duplicate row count, missing dataset warnings
  - correlation summary with extracted value count, missing value count, failed extractor count, warnings
- Empty state:
  - If all props are absent, show info alert `t('scenarioSummaryUnavailable')`.
- In performance detail modal:
  - Add a new tab after `Rapor` with label `t('scenarioSummary')`.
  - Derive:
    ```ts
    const detailScenarioSummary = analysisData?.scenarioSummary ?? selectedHistoryItem?.scenarioSummary ?? selectedResult?.scenarioSummary ?? null;
    const detailRetrySummary = analysisData?.retrySummary ?? selectedHistoryItem?.retrySummary ?? selectedResult?.retrySummary ?? null;
    const detailDatasetUsageSummary = analysisData?.datasetUsageSummary ?? selectedHistoryItem?.datasetUsageSummary ?? selectedResult?.datasetUsageSummary ?? null;
    const detailCorrelationSummary = analysisData?.correlationSummary ?? selectedHistoryItem?.correlationSummary ?? selectedResult?.correlationSummary ?? null;
    ```
  - Insert the scenario panel tab content and increment existing tab indexes after the new tab.

**Done When:**
- Scenario result details render when scenario summaries exist.
- Legacy/simple test details show a clear empty state in the scenario tab.
- Existing technical tabs still render after index shift.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes.

---

### TASK-11: End-To-End Scenario Compatibility Checks

**Targets:**
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioPlannerTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceScenarioResultBuilderTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)
- `src/test/java/etiya/omniAutomation/service/impl/ApiCallServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Add result builder test `buildsEmptySummariesForSimpleRun`:
  - Pass a thread group with no scenario metadata.
  - Assert scenario item summaries are empty and no exception is thrown.
- Add export test `exportsScenarioFieldsWhenPresent`:
  - Build a `PerfRsltEntity` with `scenarioConfig`, `scenarioSummary`, `retrySummary`, `datasetUsageSummary`, and `correlationSummary`.
  - Assert `PerformanceExportPayload` returns each field.
- Update an existing ApiCall service test to assert simple `executePerformanceTest` still works with `scenario == null`. The assertion must verify that the existing simple execution path does not require scenario fields and no validation error is thrown.
- Do not modify frontend files in this task.

**Done When:**
- Tests cover simple-mode compatibility and scenario payload passthrough.
- Scenario summary builders tolerate simple runs.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceScenarioPlannerTest,PerformanceScenarioResultBuilderTest,PerformanceExportServiceTest,ApiCallServiceTest test`. Expected: tests pass.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint passes after frontend scenario tasks.
