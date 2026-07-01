# Performance Scenario Engine Design

## Goal

Extend the performance test module from a single-flow runner into a scenario-model runner.

The first scenario version focuses on realistic multi-flow behavior, dataset-aware execution, same-virtual-user correlation, retry policy, and scenario-level reporting. Load profile types such as spike, stress, soak, step-load, and target RPS are intentionally deferred.

## Scope

Included:

- Optional scenario request model.
- Multiple process flows in one performance run.
- Weighted scenario items.
- Item-level loop, think time, timeout, and continue-on-error settings.
- Dataset reference and dataset distribution strategy.
- Same-virtual-user correlation context.
- Retry policy.
- Scenario, retry, dataset, and correlation summaries.
- Stop and force-stop support through the existing run registry.
- Frontend scenario builder and distribution preview.

Excluded:

- Distributed load generators.
- Target RPS scheduler.
- Spike/stress/soak/step-load profiles.
- Full dataset management UI.
- Cron or scheduled performance runs.
- Complex branching workflow designer.
- K6/JMeter DSL import/export.
- Cross-user or global correlation.

## Current Context

The current performance runner accepts a `PerformanceRequest` with one `processFlowId`, thread count, ramp-up, duration, loop count, think time, timeout, environment, and threshold fields.

The current execution path:

- `PerformanceService.executePerformanceTest`
- `executeProcessFlowPerformance`
- `ApiCallServiceImpl.executeFlowPerformanceTest`
- `PerformanceRunRegistry` for stop and force-stop
- `PerformanceMetricsCalculator` for run and step summaries
- `PerformanceAnalysisBuilder` and threshold evaluation after completion

The scenario engine must preserve the existing simple test behavior.

## Request Model

Extend `PerformanceRequest` with an optional field:

- `PerformanceScenarioRequest scenario`

Behavior:

- If `scenario` is null, the current single-flow execution path runs.
- If `scenario` is present, the new scenario execution path runs.

### PerformanceScenarioRequest

Fields:

- `name: String`
- `description: String`
- `totalThreads: Integer`
- `globalLoopCount: Integer`
- `globalThinkTimeMs: Integer`
- `globalTimeoutMs: Integer`
- `datasetStrategy: PerformanceDatasetStrategy`
- `items: List<PerformanceScenarioItemRequest>`

### PerformanceScenarioItemRequest

Fields:

- `processFlowId: Long`
- `weightPercent: Double`
- `enabled: Boolean`
- `loopCount: Integer`
- `thinkTimeMs: Integer`
- `timeoutMs: Integer`
- `continueOnError: Boolean`
- `datasetId: Long`
- `datasetStrategy: PerformanceDatasetStrategy`
- `retryPolicy: PerformanceRetryPolicy`

### PerformanceRetryPolicy

Fields:

- `enabled: Boolean`
- `maxRetries: Integer`
- `retryDelayMs: Integer`
- `retryOnTimeout: Boolean`
- `retryOn5xx: Boolean`
- `retryOnAssertionFailure: Boolean`

### PerformanceDatasetStrategy

Values:

- `NONE`
- `SEQUENTIAL`
- `RANDOM`
- `UNIQUE_PER_THREAD`

The first implementation accepts `datasetId` and strategy in the scenario request model. Full dataset management UI is outside this spec, but the backend contract is explicit: when a dataset is referenced and cannot be resolved by the available dataset source, the run fails validation before execution.

## Persistence

Add nullable JSONB fields to `perf_rslt`:

- `scenario_config`
- `scenario_summary`
- `retry_summary`
- `dataset_usage_summary`
- `correlation_summary`

Entity fields:

- `PerformanceScenarioConfig scenarioConfig`
- `PerformanceScenarioSummary scenarioSummary`
- `PerformanceRetrySummary retrySummary`
- `PerformanceDatasetUsageSummary datasetUsageSummary`
- `PerformanceCorrelationSummary correlationSummary`

The original run summary, step summary, threshold result, analysis summary, error analysis, environment metrics, validation checklist, and export payload remain available.

## Execution Architecture

### PerformanceScenarioPlanner

Responsibilities:

- Validate scenario request.
- Remove disabled items.
- Validate flow IDs.
- Normalize or reject weights.
- Calculate virtual-user allocation per scenario item.
- Build a scenario execution plan.
- Estimate total sample count.
- Produce warnings for risky configurations.

Planning rules:

- At least one enabled item is required.
- `totalThreads` must be positive.
- Weight total must be 100 or normalizable.
- Each enabled item must have a valid `processFlowId`.
- Item-level loop, think time, timeout override global values.
- Retry limits are capped.
- Dataset strategy cannot be non-`NONE` without a dataset reference.

### PerformanceScenarioExecutor

Responsibilities:

- Execute the scenario plan.
- Create virtual-user tasks.
- Attach tasks to `PerformanceRunRegistry`.
- Honor stop and force-stop.
- Use existing process-flow execution logic where possible.

Execution model:

- Each virtual user has a scenario context.
- Each virtual user is assigned one or more scenario items based on the plan.
- Item loop count controls how many times that item runs for the virtual user.
- Global loop count applies when item loop count is missing.
- Think time is applied between steps or items according to existing behavior.

### PerformanceScenarioContextManager

Responsibilities:

- Manage scenario context values.
- Manage virtual-user context values.
- Manage iteration context values.
- Attach dataset row values to the active context.
- Store extracted correlation values from responses.

Context boundaries:

- `scenario`: shared immutable scenario metadata.
- `virtualUser`: values available to all flows for one virtual user.
- `iteration`: values available during one loop/iteration.

First version correlation:

- Same virtual user only.
- Same run only.
- Flow-to-flow correlation is allowed only when flows run in the same virtual-user sequence.
- Global cross-user correlation is not supported.

### PerformanceRetryExecutor

Responsibilities:

- Execute a step with retry policy.
- Decide retry eligibility by error type.
- Track retry attempts.
- Expose retry metadata to result summaries.

Retry behavior:

- Retry is disabled by default.
- `maxRetries` is capped by backend validation.
- Retry delay is capped by backend validation.
- Retry on timeout / 5xx / assertion failure is configurable.
- Retry attempts are counted separately from first attempts.
- A request that succeeds after retry should be visible as recovered, not silently hidden.

### PerformanceScenarioResultBuilder

Responsibilities:

- Build scenario item summary.
- Build flow summary.
- Build retry summary.
- Build dataset usage summary.
- Build correlation summary.
- Keep existing run and step summary compatible.

## Result Models

### PerformanceScenarioSummary

Fields:

- `scenarioName`
- `totalThreads`
- `enabledItemCount`
- `plannedSampleCount`
- `actualSampleCount`
- `items: List<PerformanceScenarioItemSummary>`
- `warnings: List<String>`

### PerformanceScenarioItemSummary

Fields:

- `processFlowId`
- `flowShortCode`
- `weightPercent`
- `allocatedThreads`
- `loopCount`
- `sampleCount`
- `successCount`
- `failureCount`
- `errorRate`
- `averageMs`
- `p95Ms`
- `p99Ms`
- `throughputPerSecond`

### PerformanceRetrySummary

Fields:

- `enabled`
- `totalRetryAttempts`
- `recoveredRequestCount`
- `failedAfterRetryCount`
- `retryByStep`
- `retryByReason`

### PerformanceDatasetUsageSummary

Fields:

- `datasetsUsed`
- `strategy`
- `rowsConsumed`
- `duplicateRowCount`
- `missingDatasetWarnings`

### PerformanceCorrelationSummary

Fields:

- `extractedValueCount`
- `missingValueCount`
- `failedExtractorCount`
- `warnings`

## Backend Integration

`PerformanceService.executePerformanceTest`:

- If `request.scenario == null`, use current path.
- If `request.scenario != null`, call `executeScenarioPerformance`.

`executeScenarioPerformance`:

1. Create running result.
2. Persist `scenario_config`.
3. Load required process flows.
4. Build scenario plan.
5. Create running result items for planned virtual users.
6. Start `PerformanceScenarioExecutor` asynchronously.
7. Return `PerformanceResultDto`.

On completion:

1. Persist thread detail.
2. Build run and step summaries.
3. Build scenario summaries.
4. Evaluate thresholds.
5. Build analysis and error analysis.
6. Apply baseline comparison.
7. Build validation checklist.
8. Allow AI insight/report layer to read scenario summaries when present.

## Frontend UX

Add a test type selector to the performance dashboard:

- `Basit Test`
- `Scenario Test`

Simple test keeps the existing form and behavior.

Scenario test shows a builder.

### Scenario General Settings

Fields:

- Scenario name.
- Total threads.
- Global loop count.
- Global think time.
- Global timeout.

### Scenario Items

Each item supports:

- Flow select.
- Weight percent.
- Enabled toggle.
- Item loop count.
- Think time override.
- Timeout override.
- Continue on error.
- Dataset select.
- Dataset strategy.
- Retry policy.

### Distribution Preview

Shows:

- Total threads.
- Threads per flow.
- Estimated iterations.
- Estimated sample count.
- Weight total.
- Risk warnings.

### Validation Panel

Shows:

- Invalid weight total.
- Missing flow.
- Dataset strategy without dataset.
- Retry limit warnings.
- High thread count confirmation.
- Ramp-up or think-time warnings when relevant.

### Scenario Result Detail

Add result sections:

- Scenario summary.
- Scenario item summary.
- Retry summary.
- Dataset usage summary.
- Correlation warnings.

The history table can initially show existing run-level fields. Scenario-specific detail appears in the detail modal.

## Compatibility

Backward compatibility rules:

- Existing single-flow requests continue working.
- Existing history and detail views tolerate missing scenario fields.
- Existing exports continue working.
- Existing test stop and force-stop endpoints continue to apply.
- Existing performance analytics remain valid for simple and scenario runs.

## Error Handling

Validation errors are returned before starting the run when:

- No enabled scenario item exists.
- A flow ID cannot be resolved.
- Total threads is invalid.
- Retry policy exceeds backend limits.
- Dataset strategy requires a missing dataset.
- Cross-user correlation is requested.

Runtime warnings are recorded when:

- Correlation value is missing.
- Extractor fails.
- Dataset rows are reused.
- Retry recovers requests.
- Continue-on-error skips a failed flow item.

Force stop:

- Cancels active tasks through `PerformanceRunRegistry`.
- Marks running steps as stopped.
- Still persists partial scenario summary when possible.

## Testing

Backend unit tests:

- Planner distributes weights correctly.
- Planner rejects no enabled items.
- Planner validates missing flow.
- Planner validates dataset strategy.
- Retry policy validation caps unsafe values.
- Context manager resolves same-virtual-user correlation.
- Retry executor counts recovered and failed-after-retry requests.
- Scenario result builder creates item summaries.

Backend integration tests:

- Simple request still uses old path.
- Scenario request persists `scenario_config`.
- Scenario run produces scenario summary.
- Stop and force-stop work for scenario runs.
- `/performance/analysis` includes scenario summaries when available.

Frontend verification:

- Switching between simple and scenario test does not lose current simple form behavior.
- Scenario item add/remove works.
- Weight preview updates.
- Validation panel catches invalid scenario.
- Scenario detail sections render legacy and new payloads safely.

Acceptance criteria:

- User can configure one performance run with multiple flows.
- Backend executes scenario requests through a scenario path.
- Scenario config is persisted on the result.
- Scenario summaries are persisted after completion.
- Retry behavior is configurable and visible in summaries.
- Dataset and correlation issues produce clear validation errors or warnings.
- Stop and force-stop work for scenario tests.
- Existing simple performance tests remain compatible.
