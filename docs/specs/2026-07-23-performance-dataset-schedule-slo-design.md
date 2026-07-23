# Performance Dataset, Schedule, and SLO Score Design

## Scope

This design adds three connected performance test capabilities:

- Dataset management for data-driven performance runs.
- Scheduled performance test execution.
- SLA/SLO scoring with a 0-100 score and A/B/C/D/F grade.

The features are delivered as one integrated performance enhancement, but each capability has its own backend service/model boundary and frontend panel.

## Existing Context

The current performance test flow already supports:

- Starting a performance run through `PerformanceService.executePerformanceTest`.
- Persisting run-level settings and generated analysis on `perf_rslt`.
- Threshold presets and custom threshold configuration.
- Baseline comparison.
- AI report output in the detail modal's first Report tab.
- JSON/CSV export.

`PerformanceRequest` already contains `testDataId`, but there is no complete dataset management flow behind it.

## Architecture

The implementation should keep three bounded modules:

- `PerformanceDataset`: owns dataset metadata, rows, upload parsing, preview, and mapping.
- `PerformanceSchedule`: owns saved recurring run definitions and schedule dispatching.
- `PerformanceSloScore`: owns score calculation, grade generation, and score presentation data.

These modules plug into the existing performance run lifecycle without replacing the current run/history/detail/export behavior.

## Dataset Management

### Backend Model

Create a dataset metadata table:

- `perf_dataset`
- `dataset_id`
- `project_id`
- `name`
- `description`
- `source_type`: `MANUAL`, `CSV`, or `JSON`
- `column_schema`: JSONB
- `default_mapping`: JSONB
- `row_count`
- `active`
- `created_at`
- `updated_at`

Create a dataset row table:

- `perf_dataset_row`
- `row_id`
- `dataset_id`
- `row_index`
- `data`: JSONB
- `active`

Add a `test_data_id` column to `perf_rslt` so executed results preserve which dataset was used.

### API

Add dataset endpoints under the performance API area:

- List active datasets by project.
- Create, update, and deactivate datasets.
- Add, update, and deactivate manual rows.
- Upload CSV or JSON dataset content.
- Preview dataset schema and the first 20 active rows.

Dataset operations use the existing performance permissions:

- Read/preview/list operations use performance history view permission.
- Create/update/upload/deactivate operations use performance test run permission.

### Data Mapping

Performance test requests can include:

- `testDataId`
- `datasetMapping`: process flow parameter name to dataset field name.

If `datasetMapping` is missing, the dataset's `default_mapping` is used.

At run time, selected dataset row values override the process flow parameter context for the current thread/loop. Missing fields, inactive datasets, empty datasets, and invalid mappings must fail before the run starts with clear validation errors.

### Row Selection

The first version uses deterministic round-robin row selection:

```text
rowIndex = (threadNumber + loopIndex) % rowCount
```

This gives repeatable runs and spreads rows across threads and loops without adding randomization complexity.

### Frontend

Add a Dataset panel to the performance screen:

- Dataset selector.
- Dataset preview.
- Mapping editor.
- File upload for CSV/JSON.
- Manual row creation/editing/deactivation.

The run request sends `testDataId` and the effective `datasetMapping`.

Out of scope for the first version: dataset versioning, masking, generated synthetic data, and expression-based transformations.

## Scheduled Performance Tests

### Backend Model

Create a schedule table:

- `perf_schedule`
- `schedule_id`
- `project_id`
- `process_flow_id`
- `name`
- `cron_expression`
- `timezone`
- `enabled`
- `request_snapshot`: JSONB serialized `PerformanceRequest`
- `last_run_at`
- `next_run_at`
- `last_result_id`
- `last_status`
- `created_at`
- `updated_at`

The `request_snapshot` stores the full run configuration, including dataset, threshold, timeout, duration, loop count, and environment settings.

### API

Add schedule endpoints under the performance API area:

- List schedules by project and process flow.
- Create schedule from the current performance form.
- Update schedule cron, enabled flag, name, and request snapshot.
- Deactivate schedule. Hard delete is not part of the first version.
- Run schedule now.

The run-now endpoint uses the schedule snapshot and returns the started performance result.

### Dispatcher

Add a small Spring dispatcher using `@Scheduled(fixedDelay = 60000)`.

The dispatcher:

- Finds enabled schedules where `next_run_at <= now`.
- Skips a schedule if its previous result is still `RUNNING` or `STOPPING`.
- Converts `request_snapshot` back into `PerformanceRequest`.
- Calls `PerformanceService.executePerformanceTest`.
- Updates `last_run_at`, `last_result_id`, `last_status`, and `next_run_at`.

If a schedule fails to start, the dispatcher records `last_status=FAILED_TO_START`, calculates the next cron time, and keeps the schedule enabled.

The first version assumes a single backend instance. Distributed locking is out of scope.

### Frontend

Add a Schedule panel to the performance screen:

- Save current form as schedule.
- Daily/hourly/weekly shortcuts plus advanced cron input.
- Timezone selection with the browser timezone as default; if unavailable, use `Europe/Istanbul`.
- Schedule list with enabled toggle.
- Last run, next run, status, run now, edit, and deactivate actions.

## SLA/SLO Score

### Model

Create a `PerformanceSloScore` DTO:

- `score`: 0-100
- `grade`: `A`, `B`, `C`, `D`, or `F`
- `status`: `EXCELLENT`, `GOOD`, `WARNING`, or `CRITICAL`
- `metricScores`: per-metric score details
- `strengths`
- `weaknesses`
- `recommendations`
- `calculatedAt`

Add a `slo_score` JSONB column to `perf_rslt`.

Include `sloScore` in:

- Result DTOs.
- History summaries.
- Analysis/export payload.
- CSV export.

### Scoring

Base weights:

- Error rate: 30 points.
- P95 response time: 25 points.
- P99 response time: 15 points.
- Average response time: 10 points.
- Throughput: 10 points.
- Baseline deviation: 10 points.

If baseline comparison is unavailable, distribute the baseline weight proportionally across the other metrics.

Grades:

- `A`: 90-100
- `B`: 75-89
- `C`: 60-74
- `D`: 40-59
- `F`: 0-39

Threshold failure does not prevent scoring. The score should show how severe the result is, not only whether it passed.

### Lifecycle

Calculate the SLO score after run summary, threshold result, and baseline comparison are available.

The score should be available to the AI report service. If AI generation fails or is disabled, fallback report text should still use the SLO score to produce a manager-readable summary.

## UI Integration

The performance main screen keeps the existing start form and adds:

- `PerformanceDatasetPanel`
- `PerformanceSchedulePanel`

The performance detail modal keeps the first Report tab and adds the SLO score card at the top of that tab.

History adds a score/grade column with a compact chip.

Analysis can show metric-level score details.

Export keeps the existing controls and includes SLO score in JSON/CSV outputs.

`PerformanceTestsContent.tsx` is already large. The implementation should avoid adding all new UI directly into that file. Prefer focused components:

- `PerformanceDatasetPanel`
- `PerformanceSchedulePanel`
- `PerformanceSloScorePanel`

## Validation and Error Handling

Dataset validation:

- Dataset must exist, be active, and belong to the selected project.
- Dataset must contain at least one active row.
- Mapping must point to existing fields.
- Uploaded CSV/JSON must parse successfully and produce object-like rows.

Schedule validation:

- Cron expression must be valid.
- Timezone must be valid.
- Request snapshot must pass the same validation as a manual performance run.
- A schedule cannot start a new run while its last run is still active.

Score validation:

- Missing run summary results in no score.
- Missing baseline does not block scoring.
- Zero or null thresholds must be guarded to avoid division-by-zero style score errors.

## Testing

Backend tests:

- Dataset upload parsing for CSV and JSON.
- Manual dataset row CRUD behavior.
- Dataset mapping and row selection.
- Performance run validation when dataset is inactive, empty, or mapping is invalid.
- Schedule cron validation and next-run calculation.
- Dispatcher starts due schedules.
- Dispatcher skips schedules whose previous result is still running.
- SLO score grade boundaries.
- SLO score behavior with and without baseline.
- Export includes `sloScore`.

Frontend verification:

- TypeScript and focused lint checks.
- Dataset panel can select, preview, map, upload, and edit rows.
- Schedule panel can save, toggle, run now, edit, and deactivate.
- Report tab shows SLO score above the AI report.
- History shows score/grade.

If full frontend build remains blocked by an unrelated generated `.next` validator issue, note it separately and still run targeted verification.

## Out of Scope

- Distributed schedule locking.
- Retry queue or notification/alarm engine.
- Dataset version history.
- Sensitive data masking.
- Synthetic data generation.
- Expression language for dataset mapping.
- Project-specific custom score weight profiles.
