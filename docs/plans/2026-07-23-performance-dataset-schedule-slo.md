# Performance Dataset, Schedule, and SLO Score - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Add data-driven performance runs, recurring scheduled runs, and manager-readable SLA/SLO scoring to the existing performance test tool.

**Architecture:** Add three backend modules that plug into the current performance lifecycle: dataset management, schedule dispatching, and SLO scoring. Persist dataset/schedule records in dedicated tables and persist executed-run dataset and score metadata on `perf_rslt`. Add focused frontend panels instead of expanding all new UI directly inside `PerformanceTestsContent.tsx`.

**Tech / dependencies:** Java 21, Spring Boot 3.5.7, Spring Data JPA, Liquibase XML, PostgreSQL JSONB, Hypersistence JSON types, Spring `CronExpression`, Next.js 16, React 19, MUI 7, existing Axios API client. No new Maven or npm dependency is required by this plan.

**File map:**
- `Apihub/src/main/resources/db/changelog/changes/liquibase-migration-file.xml` - Adds `perf_dataset`, `perf_dataset_row`, `perf_schedule`, `perf_rslt.test_data_id`, and `perf_rslt.slo_score`.
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerformanceDatasetEntity.java` - Dataset metadata JPA entity.
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerformanceDatasetRowEntity.java` - Dataset row JPA entity with JSONB row data.
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerformanceScheduleEntity.java` - Schedule JPA entity with JSONB request snapshot.
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` - Adds executed dataset id and SLO score JSON fields.
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceDatasetRepository.java` - Dataset metadata repository.
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceDatasetRowRepository.java` - Dataset row repository.
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceScheduleRepository.java` - Schedule repository for CRUD and due-schedule lookup.
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceResultRepository.java` - Adds active-result lookup used by schedule skip logic.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetSourceType.java` - Dataset source enum.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetDto.java` - Dataset metadata API DTO.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetRowDto.java` - Dataset row API DTO.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetPreview.java` - Dataset preview DTO with schema and first 20 rows.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceScheduleDto.java` - Schedule API DTO.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceScheduleStatus.java` - Schedule status enum.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloMetricScore.java` - Per-metric score DTO.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloScore.java` - Run-level 0-100 score DTO.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloGrade.java` - SLO grade enum.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloStatus.java` - SLO status enum.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` - Adds `testDataId` and `sloScore`.
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` - Adds `testDataId` and `sloScore` to export payload.
- `Apihub/src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` - Adds `testDataId` and `sloScore` to history records.
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceRequest.java` - Adds dataset mapping to the run request.
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceDatasetRequest.java` - Dataset create/update request.
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceDatasetRowRequest.java` - Manual row create/update request.
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceScheduleRequest.java` - Schedule create/update request.
- `Apihub/src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` - Maps `testDataId` and `sloScore` between entity and DTO.
- `Apihub/src/main/java/etiya/omniAutomation/controller/PerformanceDatasetController.java` - Dataset HTTP API.
- `Apihub/src/main/java/etiya/omniAutomation/controller/PerformanceScheduleController.java` - Schedule HTTP API.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetParser.java` - Parses CSV and JSON dataset uploads.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetService.java` - Dataset CRUD, upload, preview, and validation.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetRuntimeContext.java` - Immutable dataset rows/mapping used during a run.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetRuntimeService.java` - Resolves and applies dataset rows to thread-local parameter contexts.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceScheduleService.java` - Schedule CRUD, cron validation, run-now, and next-run calculation.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceScheduleDispatcher.java` - Minute-based dispatcher that starts due schedules.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceSloScoreService.java` - Calculates SLO score and grade from run metrics.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceService.java` - Persists dataset id, validates dataset before run, exposes SLO data in summaries and analysis.
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` - Applies dataset values per thread/loop and calculates SLO after result metrics exist.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` - Includes SLO score and dataset id in JSON/CSV export.
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` - Uses SLO score in AI prompt and fallback report text.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceDatasetParserTest.java` - CSV/JSON parser tests.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceDatasetRuntimeServiceTest.java` - Dataset row selection and mapping tests.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceSloScoreServiceTest.java` - Score boundaries and missing-baseline tests.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceScheduleServiceTest.java` - Cron validation and next-run tests.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceScheduleDispatcherTest.java` - Due schedule start and running-result skip tests.
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` - Export includes SLO score.
- `apihub-fe/types/performance.ts` - Adds dataset, schedule, and SLO frontend types.
- `apihub-fe/services/performanceService.ts` - Sends dataset mapping in run request and keeps existing performance APIs.
- `apihub-fe/services/performanceDatasetService.ts` - Dataset API client.
- `apihub-fe/services/performanceScheduleService.ts` - Schedule API client.
- `apihub-fe/components/performance/PerformanceDatasetPanel.tsx` - Dataset select, preview, mapping, upload, and manual row UI.
- `apihub-fe/components/performance/PerformanceSchedulePanel.tsx` - Schedule create/list/toggle/run-now UI.
- `apihub-fe/components/performance/PerformanceSloScorePanel.tsx` - Score card and metric-level details.
- `apihub-fe/components/performance/PerformanceAiReportPanel.tsx` - Accepts optional score and renders manager report below score.
- `apihub-fe/components/performance/PerformanceAnalysisPanel.tsx` - Shows metric-level SLO score details.
- `apihub-fe/components/performance/PerformanceRunSummaryTable.tsx` - Adds history score/grade column.
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` - Wires dataset/schedule panels into the performance page and includes SLO score in detail data.
- `apihub-fe/messages/tr.json` - Turkish labels for dataset, schedule, and SLO UI.
- `apihub-fe/messages/en.json` - English labels for dataset, schedule, and SLO UI.

---

### TASK-01: Backend Contracts and Schema

**Targets:**
- `Apihub/src/main/resources/db/changelog/changes/liquibase-migration-file.xml` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerformanceDatasetEntity.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerformanceDatasetRowEntity.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerformanceScheduleEntity.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/entity/PerfRsltEntity.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceDatasetRepository.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceDatasetRowRepository.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceScheduleRepository.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetSourceType.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetDto.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetRowDto.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceDatasetPreview.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceScheduleDto.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceScheduleStatus.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloMetricScore.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloScore.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloGrade.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceSloStatus.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceResultDto.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/results/PerformanceSummaryResult.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceRequest.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceDatasetRequest.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceDatasetRowRequest.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/request/PerformanceScheduleRequest.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/mappers/PerformanceResultMapper.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Add Liquibase changesets with `preConditions onFail="MARK_RAN"`:
  - `2026-07-23-010-create-perf-dataset`
  - `2026-07-23-011-create-perf-dataset-row`
  - `2026-07-23-012-create-perf-schedule`
  - `2026-07-23-013-add-test-data-id-to-perf-rslt`
  - `2026-07-23-014-add-slo-score-to-perf-rslt`
- `perf_dataset` columns:
  - `dataset_id BIGINT autoIncrement primary key`
  - `project_id BIGINT not null`
  - `name VARCHAR(160) not null`
  - `description VARCHAR(1000)`
  - `source_type VARCHAR(32) not null`
  - `column_schema jsonb`
  - `default_mapping jsonb`
  - `row_count INT default 0 not null`
  - `active BOOLEAN default true not null`
  - `created_at TIMESTAMP default CURRENT_TIMESTAMP`
  - `updated_at TIMESTAMP default CURRENT_TIMESTAMP`
  - index `idx_perf_dataset_project_active` on `project_id, active`
- `perf_dataset_row` columns:
  - `row_id BIGINT autoIncrement primary key`
  - `dataset_id BIGINT not null`
  - `row_index INT not null`
  - `data jsonb`
  - `active BOOLEAN default true not null`
  - index `idx_perf_dataset_row_dataset_active` on `dataset_id, active`
  - unique constraint `uk_perf_dataset_row_dataset_index` on `dataset_id, row_index`
  - foreign key `fk_perf_dataset_row_dataset` from `dataset_id` to `perf_dataset.dataset_id` with cascade delete.
- `perf_schedule` columns:
  - `schedule_id BIGINT autoIncrement primary key`
  - `project_id BIGINT not null`
  - `process_flow_id BIGINT not null`
  - `name VARCHAR(160) not null`
  - `cron_expression VARCHAR(120) not null`
  - `timezone VARCHAR(80) not null`
  - `enabled BOOLEAN default true not null`
  - `request_snapshot jsonb`
  - `last_run_at TIMESTAMP`
  - `next_run_at TIMESTAMP`
  - `last_result_id BIGINT`
  - `last_status VARCHAR(40)`
  - `created_at TIMESTAMP default CURRENT_TIMESTAMP`
  - `updated_at TIMESTAMP default CURRENT_TIMESTAMP`
  - index `idx_perf_schedule_due` on `enabled, next_run_at`
  - index `idx_perf_schedule_project_flow` on `project_id, process_flow_id`
- Add nullable columns to `perf_rslt`:
  - `test_data_id BIGINT`
  - `slo_score jsonb`
- `PerformanceDatasetEntity` uses Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Entity`, `@Table(name = "perf_dataset")`.
  - JSON fields use `@Type(JsonBinaryType.class)`, `@JdbcTypeCode(SqlTypes.JSON)`, and `columnDefinition = "jsonb"`.
  - Field types: `Long datasetId`, `Long projectId`, `String name`, `String description`, `PerformanceDatasetSourceType sourceType`, `Map<String, Object> columnSchema`, `Map<String, String> defaultMapping`, `Integer rowCount`, `Boolean active`, `Date createdAt`, `Date updatedAt`.
- `PerformanceDatasetRowEntity` maps `perf_dataset_row`.
  - Field types: `Long rowId`, `Long datasetId`, `Integer rowIndex`, `Map<String, Object> data`, `Boolean active`.
- `PerformanceScheduleEntity` maps `perf_schedule`.
  - Field types: `Long scheduleId`, `Long projectId`, `Long processFlowId`, `String name`, `String cronExpression`, `String timezone`, `Boolean enabled`, `PerformanceRequest requestSnapshot`, `Date lastRunAt`, `Date nextRunAt`, `Long lastResultId`, `PerformanceScheduleStatus lastStatus`, `Date createdAt`, `Date updatedAt`.
- Create enums:
  - `PerformanceDatasetSourceType { MANUAL, CSV, JSON }`
  - `PerformanceScheduleStatus { NEVER_RUN, STARTED, FAILED_TO_START, SKIPPED_RUNNING, DISABLED }`
  - `PerformanceSloGrade { A, B, C, D, F }`
  - `PerformanceSloStatus { EXCELLENT, GOOD, WARNING, CRITICAL }`
- Create records:
  - `PerformanceDatasetDto(Long datasetId, Long projectId, String name, String description, PerformanceDatasetSourceType sourceType, Map<String, Object> columnSchema, Map<String, String> defaultMapping, Integer rowCount, Boolean active, Date createdAt, Date updatedAt)`
  - `PerformanceDatasetRowDto(Long rowId, Long datasetId, Integer rowIndex, Map<String, Object> data, Boolean active)`
  - `PerformanceDatasetPreview(PerformanceDatasetDto dataset, List<PerformanceDatasetRowDto> rows)`
  - `PerformanceScheduleDto(Long scheduleId, Long projectId, Long processFlowId, String name, String cronExpression, String timezone, Boolean enabled, PerformanceRequest requestSnapshot, Date lastRunAt, Date nextRunAt, Long lastResultId, PerformanceScheduleStatus lastStatus, Date createdAt, Date updatedAt)`
  - `PerformanceSloMetricScore(String metricName, double score, double maxScore, Double actualValue, Double targetValue, String direction, String message)`
  - `PerformanceSloScore(Integer score, PerformanceSloGrade grade, PerformanceSloStatus status, List<PerformanceSloMetricScore> metricScores, List<String> strengths, List<String> weaknesses, List<String> recommendations, Date calculatedAt)`
- Add fields:
  - `PerformanceRequest`: `private Map<String, String> datasetMapping = new LinkedHashMap<>();`
  - `PerformanceResultDto`: `private Long testDataId; private PerformanceSloScore sloScore;`
  - `PerformanceSummaryResult` record: add `Long testDataId` and `PerformanceSloScore sloScore` before `Date createdAt`.
  - `PerformanceExportPayload` record: add `Long testDataId` and `PerformanceSloScore sloScore` before `List<PerformanceSummary> stepSummaries`.
  - `PerfRsltEntity`: add `Long testDataId` mapped to `test_data_id`; add `PerformanceSloScore sloScore` mapped as JSONB to `slo_score`.
- Update `PerformanceResultMapper` to map `testDataId` and `sloScore` both directions.
- Update constructor calls that are affected by the new `PerformanceExportPayload` and `PerformanceSummaryResult` fields:
  - In `PerformanceExportService.buildPayload`, pass `result.getTestDataId()` and `result.getSloScore()` before `result.getSummary()`.
  - In `PerformanceService.getAnalysis`, pass `result.getTestDataId()` and `result.getSloScore()` before `result.getSummary()`.
  - In `PerformanceService.toSummaryResult`, pass `item.getTestDataId()` and `item.getSloScore()` before `item.getCreatedAt()`.
  - In the compact `PerformanceSummaryResult` constructor, pass `null` for `testDataId` and `sloScore`.
- Repository signatures:
  - `PerformanceDatasetRepository extends JpaRepository<PerformanceDatasetEntity, Long>` with `List<PerformanceDatasetEntity> findByProjectIdAndActiveTrueOrderByUpdatedAtDesc(Long projectId);`
  - `PerformanceDatasetRowRepository extends JpaRepository<PerformanceDatasetRowEntity, Long>` with `List<PerformanceDatasetRowEntity> findByDatasetIdAndActiveTrueOrderByRowIndexAsc(Long datasetId);`, `List<PerformanceDatasetRowEntity> findTop20ByDatasetIdAndActiveTrueOrderByRowIndexAsc(Long datasetId);`, and `long countByDatasetIdAndActiveTrue(Long datasetId);`
  - `PerformanceScheduleRepository extends JpaRepository<PerformanceScheduleEntity, Long>` with `List<PerformanceScheduleEntity> findByProjectIdAndProcessFlowIdOrderByCreatedAtDesc(Long projectId, Long processFlowId);` and `List<PerformanceScheduleEntity> findByEnabledTrueAndNextRunAtLessThanEqual(Date now);`

**Done When:**
- Liquibase contains all schema changes with guarded preconditions.
- Backend compiles against the new dataset, schedule, and SLO DTO/entity/repository contracts.
- `PerformanceRequest` can carry `testDataId` and `datasetMapping`.
- Performance result, history, and export DTOs expose `testDataId` and `sloScore`.

**Verification:**
- Manual: `cd Apihub; mvn -DskipTests compile` should complete without compilation errors. If `mvn` is not available on PATH, report that exact blocker.
- Manual: inspect `liquibase-migration-file.xml` and confirm the five new changeset ids are present exactly once.

### TASK-02: Dataset API and Upload Parsing

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/controller/PerformanceDatasetController.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetParser.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetService.java` (create)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceDatasetParserTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- Expose dataset endpoints from `PerformanceDatasetController` under `@RequestMapping("/performance/datasets")`.
- Use existing permissions:
  - Read endpoints use `@PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")`.
  - Mutating endpoints use `@PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")`.
- Controller methods:
  - `GET /performance/datasets?projectId={projectId}` returns `List<PerformanceDatasetDto>`.
  - `GET /performance/datasets/{datasetId}/preview` returns `PerformanceDatasetPreview` with first 20 active rows.
  - `POST /performance/datasets` accepts `PerformanceDatasetRequest` and returns `PerformanceDatasetDto`.
  - `PUT /performance/datasets/{datasetId}` accepts `PerformanceDatasetRequest` and returns `PerformanceDatasetDto`.
  - `DELETE /performance/datasets/{datasetId}` marks dataset inactive and returns HTTP 204.
  - `POST /performance/datasets/{datasetId}/rows` accepts `PerformanceDatasetRowRequest` and returns `PerformanceDatasetRowDto`.
  - `PUT /performance/datasets/{datasetId}/rows/{rowId}` accepts `PerformanceDatasetRowRequest` and returns `PerformanceDatasetRowDto`.
  - `DELETE /performance/datasets/{datasetId}/rows/{rowId}` marks row inactive and returns HTTP 204.
  - `POST /performance/datasets/upload` consumes `multipart/form-data` with request parts `projectId`, `name`, optional `description`, optional `defaultMapping` JSON string, and `file`.
- `PerformanceDatasetRequest` shape from TASK-01:
  - `Long projectId`
  - `String name`
  - `String description`
  - `Map<String, String> defaultMapping`
- `PerformanceDatasetRowRequest` shape from TASK-01:
  - `Map<String, Object> data`
- `PerformanceDatasetParser` public API:
  - `ParsedDataset parse(String fileName, byte[] content)`
  - `record ParsedDataset(PerformanceDatasetSourceType sourceType, Map<String, Object> columnSchema, List<Map<String, Object>> rows)`
- Parser behavior:
  - Treat `.csv` as CSV and `.json` as JSON based on lowercase file name.
  - CSV first line is header. Trim header names. Reject empty header, duplicate header, and files with no data rows.
  - CSV supports quoted values with doubled quote escaping. Keep values as strings.
  - JSON accepts either an array of objects or an object containing a `rows` array of objects. Reject scalar rows.
  - `columnSchema` should be a `LinkedHashMap` where each field maps to `"string"` for CSV and to simple type names (`"string"`, `"number"`, `"boolean"`, `"object"`, `"array"`, `"null"`) for JSON.
- `PerformanceDatasetService` behavior:
  - On create, set `sourceType=MANUAL`, `rowCount=0`, `active=true`, timestamps to now, and `columnSchema` from default mapping keys as empty schema if no rows exist.
  - On update, only update name, description, and default mapping. Do not mutate rows.
  - On deactivate, set dataset active false and updatedAt.
  - On row create/update, require dataset active, non-empty data map, recompute dataset `columnSchema`, recompute active row count, and update dataset timestamp.
  - On upload, create a dataset from parsed rows, persist rows with `rowIndex` starting at 0, set row count, schema, source type, and timestamps.
  - Throw `ResponseStatusException(HttpStatus.BAD_REQUEST, "...")` for empty names, invalid file type, parse failure, empty rows, empty row data, duplicate headers, and malformed default mapping JSON.
  - Throw `ResponseStatusException(HttpStatus.NOT_FOUND, "...")` for missing dataset or row ids.
- Tests in `PerformanceDatasetParserTest`:
  - `parseCsvWithQuotedValues()` asserts two rows, quoted comma preserved, schema keys in header order.
  - `rejectCsvDuplicateHeader()` asserts an exception for `id,id`.
  - `parseJsonArray()` asserts number/boolean/string schema types.
  - `parseJsonRowsObject()` asserts an object with `rows` is accepted.
  - `rejectJsonScalarRows()` asserts scalar JSON array entries are rejected.

**Done When:**
- Dataset CRUD, upload, row management, and preview endpoints exist.
- CSV and JSON uploads produce persisted dataset rows and schema.
- Dataset delete and row delete are soft deactivations.
- Parser tests cover the specified valid and invalid inputs.

**Verification:**
- Manual: `cd Apihub; mvn -Dtest=PerformanceDatasetParserTest test` should pass. If `mvn` is unavailable, report that blocker.
- Manual: `cd Apihub; mvn -DskipTests compile` should compile the new controller/service classes.

### TASK-03: Dataset Runtime Integration

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetRuntimeContext.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceDatasetRuntimeService.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceDatasetRuntimeServiceTest.java` (create)

**Model Tier:** T4

**Implementation Notes:**
- `PerformanceDatasetRuntimeContext` should be an immutable record:
  - `record PerformanceDatasetRuntimeContext(Long datasetId, Map<String, String> mapping, List<Map<String, Object>> rows)`
  - Include method `boolean enabled()` returning true when `datasetId != null` and rows is not empty.
- `PerformanceDatasetRuntimeService` constructor dependencies:
  - `PerformanceDatasetRepository`
  - `PerformanceDatasetRowRepository`
- Public methods:
  - `PerformanceDatasetRuntimeContext resolve(Long projectId, Long testDataId, Map<String, String> requestMapping)`
  - `void applyRow(PerformanceDatasetRuntimeContext context, Map<String, String> parameterContext, int threadNumber, int loopIndex)`
- `resolve` behavior:
  - If `testDataId` is null, return context with null dataset id, empty mapping, empty rows.
  - Find dataset. Missing dataset returns 404.
  - Dataset must be active and belong to `projectId`; otherwise throw 400.
  - Active rows must not be empty; otherwise throw 400.
  - Effective mapping is request mapping if non-empty; otherwise dataset `defaultMapping`.
  - Mapping must not be empty when dataset is selected.
  - Every mapping value must exist as a key in at least one active row. If any mapped field is absent from all rows, throw 400 with field name.
- `applyRow` behavior:
  - If context is not enabled, return without mutating parameter context.
  - Calculate `rowIndex = Math.floorMod(threadNumber + loopIndex, context.rows().size())`.
  - For each mapping entry, read row value using dataset field name and put `String.valueOf(value)` into parameter context under the process flow parameter name.
  - Null row values become empty string.
- Modify `PerformanceService.executeProcessFlowPerformance`:
  - Resolve dataset context once at the beginning:
    - `PerformanceDatasetRuntimeContext datasetRuntimeContext = performanceDatasetRuntimeService.resolve(request.getProjectId(), request.getTestDataId(), request.getDatasetMapping());`
  - Pass this context to `createRunningResult(request, datasetRuntimeContext)` and to `ApiCallServiceImpl`.
- Modify `PerformanceService.createRunningResult`:
  - Change signature to `private PerfRsltEntity createRunningResult(PerformanceRequest request, PerformanceDatasetRuntimeContext datasetRuntimeContext)`.
  - Set `resultEntity.setTestDataId(request.getTestDataId())`.
  - Do not resolve the dataset again inside `createRunningResult`; receiving the context proves validation already happened.
  - After `PerformanceResultDto result = PerformanceResultMapper.INSTANCE.toDto(resultEntity);`, set `result.setTestDataId(request.getTestDataId())`.
- Modify `ApiCallServiceImpl`:
  - Add overload `executeFlowPerformanceTest(ProcessFlowDto processFlowDto, PerformanceResultDto performanceResultDto, PerformanceDatasetRuntimeContext datasetRuntimeContext)`.
  - Keep existing `executeFlowPerformanceTest(ProcessFlowDto, PerformanceResultDto)` and delegate with an empty context so other callers remain compatible.
  - Extend `processPerformanceTask` signature to receive `PerformanceDatasetRuntimeContext datasetRuntimeContext`.
  - Inside the outer loop, immediately after cancellation check and before iterating steps, call `performanceDatasetRuntimeService.applyRow(datasetRuntimeContext, threadProcessFlow.getParameterContext(), threadNumber, loop)`.
  - Inject `PerformanceDatasetRuntimeService` into `ApiCallServiceImpl`.
- Preserve current parameter extraction behavior: extracted values from earlier steps can still update the thread-local context after dataset values are applied at loop start.
- Tests in `PerformanceDatasetRuntimeServiceTest`:
  - `roundRobinUsesThreadPlusLoop()` with three rows asserts thread 2 loop 2 selects row 1.
  - `requestMappingOverridesDefaultMapping()` asserts request mapping wins.
  - `nullValueBecomesEmptyString()` asserts mapped null writes empty string.
  - `missingMappedFieldFails()` asserts 400 for absent field.
  - `inactiveDatasetFails()` asserts 400 for inactive dataset.

**Done When:**
- A performance run with `testDataId` validates the dataset before the run starts.
- Each performance thread applies dataset row values to its own copied process flow parameter context.
- Existing callers of `executeFlowPerformanceTest(ProcessFlowDto, PerformanceResultDto)` still compile.
- Dataset runtime tests cover row selection, mapping precedence, null conversion, and validation failures.

**Verification:**
- Manual: `cd Apihub; mvn -Dtest=PerformanceDatasetRuntimeServiceTest test` should pass. If `mvn` is unavailable, report that blocker.
- Manual: `cd Apihub; mvn -DskipTests compile` should compile without method signature errors.

### TASK-04: SLO Score Calculation and Export

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceSloScoreService.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceAiReportService.java` (modify)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceSloScoreServiceTest.java` (create)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- `PerformanceSloScoreService` public method:
  - `PerformanceSloScore calculate(PerformanceRunSummary runSummary, PerformanceThresholdConfig thresholds, PerformanceComparisonResult baselineComparison)`
- Return null from `calculate` only when `runSummary` is null.
- Weights:
  - error rate: 30
  - p95: 25
  - p99: 15
  - average: 10
  - throughput: 10
  - baseline: 10
- If `baselineComparison` is null or has no metrics, redistribute the 10 baseline points proportionally across the other five metrics. Effective weights become error rate 33.33, p95 27.78, p99 16.67, average 11.11, throughput 11.11.
- Metric scoring:
  - For metrics where lower is better, score is full when `actual <= target`, linearly decreases to 0 when `actual >= target * 2`, and stays 0 above that.
  - For throughput where higher is better, score is full when `actual >= target`, linearly decreases to 0 when `actual <= target * 0.5`, and stays 0 below that.
  - If a target is null or `<= 0`, do not calculate that metric and redistribute its weight proportionally across calculable metrics.
- Baseline scoring:
  - Inspect `PerformanceComparisonResult.metrics()`.
  - Count metrics with `improvement == Boolean.FALSE` as regressions.
  - Baseline metric score is full if regressions are 0, half if regressions are 1, and 0 if regressions are 2 or more.
- Grade and status:
  - `A` 90-100 -> `EXCELLENT`
  - `B` 75-89 -> `GOOD`
  - `C` 60-74 -> `WARNING`
  - `D` 40-59 -> `CRITICAL`
  - `F` 0-39 -> `CRITICAL`
- Strengths examples:
  - Add `"Hata orani hedef icinde."` when error rate gets at least 90% of its metric points.
  - Add `"P95 gecikmesi hedef icinde."` when p95 gets at least 90% of its metric points.
  - Add `"Throughput hedefi karsiliyor."` when throughput gets at least 90% of its metric points.
- Weaknesses examples:
  - Add `"Hata orani hedefi asiyor."` when error rate gets below 60% of its metric points.
  - Add `"P95 gecikmesi hedefi asiyor."` when p95 gets below 60% of its metric points.
  - Add `"Throughput hedefin altinda."` when throughput gets below 60% of its metric points.
- Recommendations examples:
  - If error rate is weak, add `"Hata ureten adimlar icin log ve response govdelerini inceleyin."`
  - If p95 or p99 is weak, add `"Yavas adimlarda servis, sorgu ve bagimli sistem surelerini analiz edin."`
  - If throughput is weak, add `"Thread, kaynak limiti ve servis kapasitesini birlikte kontrol edin."`
- Modify `ApiCallServiceImpl.persistPerformanceResult`:
  - Inject `PerformanceSloScoreService`.
  - After automatic baseline comparison returns `compared`, calculate score from `compared.getRunSummary()`, `compared.getThresholdConfig()`, `compared.getBaselineComparison()`.
  - Set `compared.setSloScore(score)`.
  - Generate AI report after setting SLO score so the report can use it.
  - Save compared entity with SLO score, AI report, and validation checklist.
- Modify `PerformanceService.getAnalysis`, `PerformanceService.export`, and `toSummaryResult` to include `testDataId` and `sloScore` in `PerformanceExportPayload` and `PerformanceSummaryResult`.
- Modify `PerformanceExportService.buildPayload` to pass `result.getTestDataId()` and `result.getSloScore()`.
- Modify `PerformanceExportService.buildCsv`:
  - Add `appendSloScore(csv, payload.sloScore())` after report metadata and before AI report.
  - CSV section header: `SLO Score`.
  - Rows: `Score`, `Grade`, `Status`, `Strengths`, `Weaknesses`, `Recommendations`.
  - Add metric rows with columns `Metric,Score,Max Score,Actual,Target,Direction,Message`.
- Modify `PerformanceAiReportService`:
  - Add `PerformanceSloScore sloScore` to `AiReportInput`.
  - Include SLO score in prompt JSON.
  - In fallback report, if SLO score exists, executive summary starts with `SLO skoru {score}/100 ({grade})`.
  - In fallback good/bad/actions lists, merge SLO strengths, weaknesses, and recommendations when present.
- Tests in `PerformanceSloScoreServiceTest`:
  - `gradeAWhenAllMetricsMeetTargets()` asserts score >= 90 and grade A.
  - `gradeFWhenAllMetricsDoubleTargetsOrWorse()` asserts grade F.
  - `redistributesBaselineWeightWhenBaselineMissing()` asserts total max score still equals 100 across metric scores.
  - `baselineRegressionReducesScore()` asserts two regressed comparison metrics produce zero baseline metric score.
  - `nullRunSummaryReturnsNull()` asserts null result.
- Update `PerformanceExportServiceTest` to assert CSV contains `SLO Score`, `Score`, `Grade`, and metric names when payload has `sloScore`.

**Done When:**
- Completed performance runs persist `slo_score` on `perf_rslt`.
- Analysis, history, JSON export, and CSV export include `sloScore`.
- AI prompt and fallback report can use SLO score.
- SLO score tests cover grade boundaries, missing baseline, baseline regressions, and null run summary.

**Verification:**
- Manual: `cd Apihub; mvn -Dtest=PerformanceSloScoreServiceTest,PerformanceExportServiceTest test` should pass. If `mvn` is unavailable, report that blocker.
- Manual: `cd Apihub; mvn -DskipTests compile` should compile after DTO constructor updates.

### TASK-05: Schedule Backend and Dispatcher

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/controller/PerformanceScheduleController.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceScheduleService.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/service/PerformanceScheduleDispatcher.java` (create)
- `Apihub/src/main/java/etiya/omniAutomation/repository/PerformanceResultRepository.java` (modify)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceScheduleServiceTest.java` (create)
- `Apihub/src/test/java/etiya/omniAutomation/service/PerformanceScheduleDispatcherTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- `PerformanceScheduleController` uses `@RequestMapping("/performance/schedules")`.
- Use existing permissions:
  - List uses performance history view permission.
  - Create, update, deactivate, toggle, and run-now use performance test run permission.
- Controller methods:
  - `GET /performance/schedules?projectId={projectId}&processFlowId={processFlowId}` returns `List<PerformanceScheduleDto>`.
  - `POST /performance/schedules` accepts `PerformanceScheduleRequest` and returns `PerformanceScheduleDto`.
  - `PUT /performance/schedules/{scheduleId}` accepts `PerformanceScheduleRequest` and returns `PerformanceScheduleDto`.
  - `POST /performance/schedules/{scheduleId}/enabled?enabled={true|false}` returns `PerformanceScheduleDto`.
  - `DELETE /performance/schedules/{scheduleId}` sets `enabled=false`, `lastStatus=DISABLED`, and returns HTTP 204.
  - `POST /performance/schedules/{scheduleId}/run-now` returns `PerformanceResultDto`.
- `PerformanceScheduleRequest` shape from TASK-01:
  - `Long projectId`
  - `Long processFlowId`
  - `String name`
  - `String cronExpression`
  - `String timezone`
  - `Boolean enabled`
  - `PerformanceRequest requestSnapshot`
- `PerformanceScheduleService` dependencies:
  - `PerformanceScheduleRepository`
  - `PerformanceResultRepository`
  - `PerformanceService`
  - `ObjectMapper` if needed for defensive request snapshot copying.
- Public methods:
  - `List<PerformanceScheduleDto> list(Long projectId, Long processFlowId)`
  - `PerformanceScheduleDto create(PerformanceScheduleRequest request)`
  - `PerformanceScheduleDto update(Long scheduleId, PerformanceScheduleRequest request)`
  - `PerformanceScheduleDto setEnabled(Long scheduleId, boolean enabled)`
  - `void deactivate(Long scheduleId)`
  - `PerformanceResultDto runNow(Long scheduleId)`
  - `void runDueSchedules(Date now)`
  - `Date nextRun(String cronExpression, String timezone, Date after)`
- Validation:
  - `projectId`, `processFlowId`, `name`, `cronExpression`, `timezone`, and `requestSnapshot` are required.
  - Request snapshot must include `projectId`, `processFlowId`, `environment`, `threadCount`, and threshold values if `thresholdPreset=CUSTOM`.
  - Schedule request `projectId` and `processFlowId` must match `requestSnapshot.projectId` and `requestSnapshot.processFlowId`.
  - Validate timezone with `ZoneId.of(timezone)`.
  - Validate cron with `CronExpression.parse(cronExpression)`.
  - `nextRun` uses `CronExpression.next(ZonedDateTime.ofInstant(after.toInstant(), zoneId))` and returns `Date.from(next.toInstant())`.
- Running skip logic:
  - Add repository method `boolean existsByPerfRsltIdAndPerfStatusIn(Long perfRsltId, Collection<GeneralEnums.PerformanceStatus> statuses);`
  - Before scheduled run starts, if `lastResultId` exists and is `RUNNING` or `STOPPING`, set `lastStatus=SKIPPED_RUNNING`, compute next run, save, and do not call `PerformanceService`.
- `runNow`:
  - Loads schedule, requires enabled schedule, calls `PerformanceService.executePerformanceTest(schedule.getRequestSnapshot())`, sets `lastRunAt`, `lastResultId`, `lastStatus=STARTED`, computes `nextRunAt`, and returns result.
- `runDueSchedules`:
  - Finds `enabled=true` and `nextRunAt <= now`.
  - For each due schedule, apply running skip logic or start a run.
  - Catch start exceptions per schedule, set `lastStatus=FAILED_TO_START`, compute `nextRunAt`, and continue with other schedules.
- `PerformanceScheduleDispatcher`:
  - `@Service`
  - Constructor dependency `PerformanceScheduleService`
  - Method `@Scheduled(fixedDelay = 60000) public void dispatch()` calls `performanceScheduleService.runDueSchedules(new Date())`.
- Tests in `PerformanceScheduleServiceTest`:
  - `validCronCalculatesNextRun()` asserts next run is after input date.
  - `invalidCronFails()` asserts 400.
  - `timezoneDefaultsAreAccepted()` asserts `Europe/Istanbul` works.
  - `mismatchedSnapshotProjectFails()` asserts 400.
- Tests in `PerformanceScheduleDispatcherTest`:
  - `dueScheduleStartsRun()` mocks `PerformanceService` and verifies run called once and status `STARTED`.
  - `runningPreviousResultSkipsRun()` mocks repository active result true and verifies status `SKIPPED_RUNNING`.
  - `failedStartRecordsFailedToStart()` makes `PerformanceService` throw and verifies status `FAILED_TO_START`.

**Done When:**
- Schedule CRUD, enable/disable, deactivate, and run-now endpoints exist.
- Dispatcher starts due schedules once per minute.
- Due schedules skip when previous run is still active.
- Cron and timezone validation are covered by tests.

**Verification:**
- Manual: `cd Apihub; mvn -Dtest=PerformanceScheduleServiceTest,PerformanceScheduleDispatcherTest test` should pass. If `mvn` is unavailable, report that blocker.
- Manual: `cd Apihub; mvn -DskipTests compile` should compile with scheduling classes.

### TASK-06: Frontend Types and API Clients

**Targets:**
- `apihub-fe/types/performance.ts` (modify)
- `apihub-fe/services/performanceService.ts` (modify)
- `apihub-fe/services/performanceDatasetService.ts` (create)
- `apihub-fe/services/performanceScheduleService.ts` (create)
- `apihub-fe/messages/tr.json` (modify)
- `apihub-fe/messages/en.json` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Extend `PerformanceRequest` in `types/performance.ts`:
  - `datasetMapping?: Record<string, string> | null;`
- Add types:
  - `export type PerformanceDatasetSourceType = 'MANUAL' | 'CSV' | 'JSON';`
  - `export type PerformanceScheduleStatus = 'NEVER_RUN' | 'STARTED' | 'FAILED_TO_START' | 'SKIPPED_RUNNING' | 'DISABLED';`
  - `export type PerformanceSloGrade = 'A' | 'B' | 'C' | 'D' | 'F';`
  - `export type PerformanceSloStatus = 'EXCELLENT' | 'GOOD' | 'WARNING' | 'CRITICAL';`
  - `export interface PerformanceDataset { datasetId: number; projectId: number; name: string; description?: string | null; sourceType: PerformanceDatasetSourceType; columnSchema?: Record<string, unknown> | null; defaultMapping?: Record<string, string> | null; rowCount: number; active: boolean; createdAt?: string | null; updatedAt?: string | null; }`
  - `export interface PerformanceDatasetRow { rowId: number; datasetId: number; rowIndex: number; data: Record<string, unknown>; active: boolean; }`
  - `export interface PerformanceDatasetPreview { dataset: PerformanceDataset; rows: PerformanceDatasetRow[]; }`
  - `export interface PerformanceDatasetRequest { projectId: number; name: string; description?: string | null; defaultMapping?: Record<string, string> | null; }`
  - `export interface PerformanceDatasetRowRequest { data: Record<string, unknown>; }`
  - `export interface PerformanceSchedule { scheduleId: number; projectId: number; processFlowId: number; name: string; cronExpression: string; timezone: string; enabled: boolean; requestSnapshot: PerformanceRequest; lastRunAt?: string | null; nextRunAt?: string | null; lastResultId?: number | null; lastStatus?: PerformanceScheduleStatus | null; createdAt?: string | null; updatedAt?: string | null; }`
  - `export interface PerformanceScheduleRequest { projectId: number; processFlowId: number; name: string; cronExpression: string; timezone: string; enabled?: boolean | null; requestSnapshot: PerformanceRequest; }`
  - `export interface PerformanceSloMetricScore { metricName: string; score: number; maxScore: number; actualValue?: number | null; targetValue?: number | null; direction?: string | null; message?: string | null; }`
  - `export interface PerformanceSloScore { score: number; grade: PerformanceSloGrade; status: PerformanceSloStatus; metricScores?: PerformanceSloMetricScore[]; strengths?: string[]; weaknesses?: string[]; recommendations?: string[]; calculatedAt?: string | null; }`
- Add optional fields to `PerformanceExportPayload`, `PerformanceResultDto`, and `PerformanceHistoryItem`:
  - `testDataId?: number | null;`
  - `sloScore?: PerformanceSloScore | null;`
- Keep `performanceService.runPerformanceTest(request: PerformanceRequest)` unchanged in signature; it will pass `datasetMapping` automatically because it posts the whole request object.
- Create `performanceDatasetService` methods:
  - `list(projectId: number): Promise<PerformanceDataset[]>`
  - `preview(datasetId: number): Promise<PerformanceDatasetPreview>`
  - `create(request: PerformanceDatasetRequest): Promise<PerformanceDataset>`
  - `update(datasetId: number, request: PerformanceDatasetRequest): Promise<PerformanceDataset>`
  - `deactivate(datasetId: number): Promise<void>`
  - `addRow(datasetId: number, request: PerformanceDatasetRowRequest): Promise<PerformanceDatasetRow>`
  - `updateRow(datasetId: number, rowId: number, request: PerformanceDatasetRowRequest): Promise<PerformanceDatasetRow>`
  - `deactivateRow(datasetId: number, rowId: number): Promise<void>`
  - `upload(projectId: number, name: string, description: string | null, defaultMapping: Record<string, string>, file: File): Promise<PerformanceDataset>`
- `upload` uses `FormData` fields `projectId`, `name`, `description`, `defaultMapping`, and `file`.
- Create `performanceScheduleService` methods:
  - `list(projectId: number, processFlowId: number): Promise<PerformanceSchedule[]>`
  - `create(request: PerformanceScheduleRequest): Promise<PerformanceSchedule>`
  - `update(scheduleId: number, request: PerformanceScheduleRequest): Promise<PerformanceSchedule>`
  - `setEnabled(scheduleId: number, enabled: boolean): Promise<PerformanceSchedule>`
  - `deactivate(scheduleId: number): Promise<void>`
  - `runNow(scheduleId: number): Promise<PerformanceResultDto>`
- Add message keys under `performance` in `tr.json` and `en.json`:
  - `dataset`, `selectDataset`, `noDataset`, `datasetPreview`, `datasetMapping`, `uploadDataset`, `manualRows`, `addRow`, `rowDataJson`, `schedule`, `saveSchedule`, `scheduleName`, `cronExpression`, `timezone`, `nextRun`, `lastRun`, `runNow`, `enabled`, `disabled`, `sloScore`, `sloGrade`, `sloStatus`, `metricScores`, `strengths`, `weaknesses`, `recommendations`.

**Done When:**
- Frontend has typed dataset, schedule, and SLO contracts.
- Dataset and schedule API clients call the backend paths defined in this plan.
- Existing `runPerformanceTest` can send dataset mapping without changing its call signature.
- Turkish and English message files contain the listed performance keys.

**Verification:**
- Manual: `cd apihub-fe; npm run lint -- --file types/performance.ts --file services/performanceService.ts --file services/performanceDatasetService.ts --file services/performanceScheduleService.ts` should complete or report only unsupported CLI option if the local ESLint setup does not support `--file`.
- Manual: If the targeted lint command is unsupported, run `cd apihub-fe; npm run lint` and report the result.

### TASK-07: Frontend Dataset, Schedule, and SLO UI

**Targets:**
- `apihub-fe/components/performance/PerformanceDatasetPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceSchedulePanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceSloScorePanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceAiReportPanel.tsx` (modify)
- `apihub-fe/components/performance/PerformanceAnalysisPanel.tsx` (modify)
- `apihub-fe/components/performance/PerformanceRunSummaryTable.tsx` (modify)
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create `PerformanceSloScorePanel` props:
  - `interface PerformanceSloScorePanelProps { score?: PerformanceSloScore | null; compact?: boolean; }`
  - Render nothing when score is null.
  - Full mode renders score number, grade chip, status chip, strengths, weaknesses, recommendations, and metric rows.
  - Compact mode renders score number and grade chip only.
- Modify `PerformanceAiReportPanel`:
  - Add prop `sloScore?: PerformanceSloScore | null`.
  - Render `<PerformanceSloScorePanel score={sloScore} />` at the top before report text.
- Modify `PerformanceAnalysisPanel`:
  - Add prop `sloScore?: PerformanceSloScore | null`.
  - Render metric-level SLO details below existing threshold/failure content using `PerformanceSloScorePanel` with `compact={false}`.
- Modify `PerformanceRunSummaryTable`:
  - Add SLO score column after status.
  - For each history item, render `PerformanceSloScorePanel` with `compact` when `item.sloScore` exists; render `-` otherwise.
- Create `PerformanceDatasetPanel` props:
  - `projectId?: number | null`
  - `processFlowParameters?: string[]`
  - `selectedDatasetId?: number | null`
  - `datasetMapping: Record<string, string>`
  - `onDatasetChange: (datasetId: number | null) => void`
  - `onMappingChange: (mapping: Record<string, string>) => void`
- `PerformanceDatasetPanel` behavior:
  - Load datasets when `projectId` exists.
  - Dataset select sets selected id and loads preview.
  - Preview shows up to 20 rows in a table.
  - Mapping UI lists `processFlowParameters` and lets each map to a dataset field from preview schema.
  - Upload area uses file input, name, description, and current mapping. On upload success, select the new dataset and refresh list.
  - Manual row area accepts JSON object text. Validate client-side with `JSON.parse`, require object and not array, call add row, refresh preview.
  - Use MUI `Select`, `TextField`, `Button`, `Table`, `Chip`, `Switch`, and `Alert`.
- Create `PerformanceSchedulePanel` props:
  - `projectId?: number | null`
  - `processFlowId?: number | null`
  - `requestSnapshot: PerformanceRequest | null`
  - `onRunStarted: (result: PerformanceResultDto) => void`
- `PerformanceSchedulePanel` behavior:
  - Load schedules when project and flow exist.
  - Default timezone is `Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul'`.
  - Offer shortcut buttons:
    - hourly: `0 0 * * * *`
    - daily: `0 0 9 * * *`
    - weekly: `0 0 9 * * MON`
  - Save schedule posts `PerformanceScheduleRequest`.
  - List schedules with enabled switch, last run, next run, status, run now, and deactivate buttons.
  - `runNow` calls service and passes returned result to `onRunStarted`.
- Modify `PerformanceTestsContent`:
  - Add state `selectedDatasetId: number | null` and `datasetMapping: Record<string, string>`.
  - Build `processFlowParameters` from selected flow relation parameters if available. If parameter names cannot be found from the selected flow object, use `Object.keys(datasetMapping)` so existing mappings remain editable.
  - Include `<PerformanceDatasetPanel />` below threshold fields.
  - Build a `currentPerformanceRequest` object containing the same fields used by `runPerformanceTest`, plus `testDataId: selectedDatasetId` and `datasetMapping`.
  - Use `currentPerformanceRequest` for both immediate run and schedule snapshot.
  - Include `<PerformanceSchedulePanel />` below dataset panel.
  - When schedule run-now returns a result, prepend it to `results` the same way immediate run does.
  - Add `testDataId` and `sloScore` to `runningItems`.
  - Add `detailSloScore` derived from analysis data, selected history item, or selected running result.
  - Pass `detailSloScore` to Report and Analysis tabs.

**Done When:**
- Performance page can select/upload/preview/map datasets.
- Performance page can save schedules, toggle enabled state, run now, and deactivate schedules.
- Immediate performance run sends `testDataId` and `datasetMapping`.
- Detail Report tab shows SLO score above the AI report.
- History table shows compact SLO score/grade.

**Verification:**
- Manual: `cd apihub-fe; npm run lint -- --file components/performance/PerformanceDatasetPanel.tsx --file components/performance/PerformanceSchedulePanel.tsx --file components/performance/PerformanceSloScorePanel.tsx --file components/performance/PerformanceTestsContent.tsx` should complete or report unsupported CLI option.
- Manual: If targeted lint is unsupported, run `cd apihub-fe; npm run lint` and report the result.
- Manual: `cd apihub-fe; npm run build` should either pass or fail only on the known generated `.next/dev/types/validator.ts` issue; report the exact build result.
