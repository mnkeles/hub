# Performance Frontend Restoration - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Restore the performance frontend from `eb416dd` on current main while preserving current authorization and unrelated frontend work and leaving `Apihub/` untouched.

**Architecture:** Rebuild deleted files from the last complete frontend commit, then selectively merge only performance-specific additions into shared files. The active screen remains `PerformanceTestsContent`, rendered by thin dashboard and project-route wrappers; legacy management-report components are restored as dormant source files and are not connected to the current backend's `PerformanceAiReport` flow.

**Tech / dependencies:** Next.js 16, React 19, TypeScript 5, Material UI 7, next-intl 4, Axios, Recharts, existing Spring performance endpoints, Git commit `eb416dd` as recovery source.

**File map:**
- `apihub-fe/types/performance.ts` - frontend request, result, AI report, dataset, schedule, SLO, comparison, validation, and legacy report contracts.
- `apihub-fe/services/performanceService.ts` - core run, detail, history, baseline, analysis, live, stop, compare, export, and legacy regenerate client calls.
- `apihub-fe/services/performanceDatasetService.ts` - dataset CRUD, preview, row, and upload client.
- `apihub-fe/services/performanceScheduleService.ts` - schedule CRUD, toggle, run-now, and deactivate client.
- `apihub-fe/lib/errorUtils.ts` - safe extraction of generic and Axios-style API errors.
- `apihub-fe/components/PerformanceRunner.tsx` - legacy route runner using the shared API error extractor.
- `apihub-fe/messages/en.json` - English labels for reports, datasets, schedules, and SLO data.
- `apihub-fe/messages/tr.json` - Turkish labels for reports, datasets, schedules, and SLO data.
- `apihub-fe/components/performance/PerformanceActionPlanPanel.tsx` - prioritized management and AI action lists.
- `apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx` - AI executive and technical narrative display.
- `apihub-fe/components/performance/PerformanceAiObservabilityPanel.tsx` - AI generation and report observability metadata.
- `apihub-fe/components/performance/PerformanceAiRegenerateButton.tsx` - dormant legacy AI regeneration action.
- `apihub-fe/components/performance/PerformanceExecutiveSummaryPanel.tsx` - management report executive summary.
- `apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` - composition root for the dormant management-report bundle.
- `apihub-fe/components/performance/PerformancePrintActions.tsx` - browser print trigger.
- `apihub-fe/components/performance/PerformanceRegressionTrendPanel.tsx` - baseline regression and trend presentation.
- `apihub-fe/components/performance/PerformanceReportDecisionHeader.tsx` - release-readiness and top-level report decision.
- `apihub-fe/components/performance/PerformanceRiskMatrixPanel.tsx` - report risk matrix.
- `apihub-fe/components/performance/PerformanceRootCauseHintsPanel.tsx` - root-cause hint list.
- `apihub-fe/components/performance/PerformanceStepRiskPanel.tsx` - per-step risk and evidence details.
- `apihub-fe/components/performance/PerformanceTechnicalFindingsPanel.tsx` - technical metrics and findings.
- `apihub-fe/app/globals.css` - print-only visibility rules for the management report.
- `apihub-fe/components/performance/PerformanceAiReportPanel.tsx` - active persisted AI report and SLO presentation.
- `apihub-fe/components/performance/PerformanceDatasetPanel.tsx` - dataset selection, preview, mapping, upload, and manual-row UI.
- `apihub-fe/components/performance/PerformanceSchedulePanel.tsx` - schedule creation and action UI with visible API failures.
- `apihub-fe/components/performance/PerformanceSloScorePanel.tsx` - full and compact SLO score rendering.
- `apihub-fe/components/performance/PerformanceAnalysisPanel.tsx` - analysis view with SLO detail.
- `apihub-fe/components/performance/PerformanceChartsPanel.tsx` - response, throughput/error, active-thread, and step percentile charts.
- `apihub-fe/components/performance/PerformanceRunSummaryTable.tsx` - history table with compact SLO score.
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` - shared performance state, execution, history, details, datasets, schedules, and exports.
- `apihub-fe/app/dashboard/performance/page.tsx` - permission-protected dashboard wrapper for the shared screen.
- `apihub-fe/app/[projectShortCode]/performance/page.tsx` - project-route wrapper with asynchronous Next.js route params and chat.

---

### TASK-01: Restore performance contracts and API foundations

**Targets:**
- `apihub-fe/types/performance.ts` (modify)
- `apihub-fe/services/performanceService.ts` (modify)
- `apihub-fe/services/performanceDatasetService.ts` (create)
- `apihub-fe/services/performanceScheduleService.ts` (create)
- `apihub-fe/lib/errorUtils.ts` (create)
- `apihub-fe/components/PerformanceRunner.tsx` (modify)
- `apihub-fe/messages/en.json` (modify)
- `apihub-fe/messages/tr.json` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Use `git show eb416dd:<path>` as the content source. Do not revert or check out a directory or commit.
- Expand `PerformanceRequest` with `datasetMapping?: Record<string, string> | null` and restore the following type families from `eb416dd`: `PerformanceAiReport`, management/insight/AI-management report types, dataset types, schedule types, and SLO score types.
- Restore `aiReport`, `managementReport`, `insightReport`, `aiManagementReport`, `testDataId`, and `sloScore` as optional nullable fields on the result/export/history contracts where they exist in `eb416dd`. Retain every current status, threshold, validation, summary, detail, and comparison type.
- Restore `performanceDatasetService` with these signatures:
  - `list(projectId: number): Promise<PerformanceDataset[]>`
  - `preview(datasetId: number): Promise<PerformanceDatasetPreview>`
  - `create(request: PerformanceDatasetRequest): Promise<PerformanceDataset>`
  - `update(datasetId: number, request: PerformanceDatasetRequest): Promise<PerformanceDataset>`
  - `deactivate(datasetId: number): Promise<void>`
  - `addRow(datasetId: number, request: PerformanceDatasetRowRequest): Promise<PerformanceDatasetRow>`
  - `updateRow(datasetId: number, rowId: number, request: PerformanceDatasetRowRequest): Promise<PerformanceDatasetRow>`
  - `deactivateRow(datasetId: number, rowId: number): Promise<void>`
  - `upload(projectId: number, name: string, description: string | null, defaultMapping: Record<string, string>, file: File): Promise<PerformanceDataset>`
- Dataset paths are `/performance/datasets`, `/performance/datasets/{id}/preview`, `/performance/datasets/{id}/rows`, and `/performance/datasets/upload`. Upload must send multipart fields `projectId`, `name`, optional `description`, JSON-stringified `defaultMapping`, and `file`.
- Restore `performanceScheduleService` with `list`, `create`, `update`, `setEnabled`, `deactivate`, and `runNow` using `/performance/schedules`. `setEnabled` posts a null body with the `enabled` query parameter; `runNow` returns `PerformanceResultDto`.
- Keep all current `performanceService` methods and restore only `regenerateAiReport(performanceResultId: number): Promise<PerformanceAiManagementReport>`, posting to `/performance/{id}/ai-report/regenerate`. This method exists for the restored dormant legacy component; do not connect it to the active AI report screen.
- Restore `getErrorMessage(error, fallback)`, `getApiErrorMessage(error, fallback)`, and `getApiErrorStatus(error)` from `eb416dd`. All parameters use `unknown`; do not add `any`.
- Update `PerformanceRunner` to import `getApiErrorMessage` and use it in its catch branch with fallback `Test başlatılamadı`, removing the current `catch (err: any)` response probing.
- Under the existing `performance` object in both locale files, restore exactly these missing keys from `eb416dd`: `report`, `aiReport`, `executiveSummary`, `businessImpact`, `goodPoints`, `badPoints`, `risks`, `recommendedActions`, `technicalDetails`, `reportSource`, `generatedAt`, `reportNotGenerated`, `reportUnavailable`, `projectLoadFailed`, `dataset`, `selectDataset`, `noDataset`, `datasetPreview`, `datasetMapping`, `uploadDataset`, `manualRows`, `addRow`, `rowDataJson`, `schedule`, `saveSchedule`, `scheduleName`, `cronExpression`, `timezone`, `nextRun`, `lastRun`, `runNow`, `enabled`, `disabled`, `sloScore`, `sloGrade`, `sloStatus`, `metricScores`, `strengths`, `weaknesses`, and `recommendations`. Preserve all current non-performance translations and `timeSeriesUnavailable`.

**Done When:**
- Every restored component can import its required performance contracts.
- Dataset and schedule clients match the existing backend routes and return typed values.
- Core performance client behavior is unchanged apart from the restored dormant regeneration method.
- Both locale files parse and contain all active report, dataset, schedule, and SLO keys.
- `PerformanceRunner` handles unknown API errors without `any`.

**Verification:**
- Manual: `cd apihub-fe; npx eslint types/performance.ts services/performanceService.ts services/performanceDatasetService.ts services/performanceScheduleService.ts lib/errorUtils.ts components/PerformanceRunner.tsx` must exit successfully.
- Manual: `cd apihub-fe; @('messages/en.json','messages/tr.json') | ForEach-Object { Get-Content -Raw $_ | ConvertFrom-Json | Out-Null }` must exit without a JSON parse error.
- Manual: `git diff --check -- apihub-fe/types/performance.ts apihub-fe/services apihub-fe/lib/errorUtils.ts apihub-fe/components/PerformanceRunner.tsx apihub-fe/messages` must report no whitespace errors.

### TASK-02: Restore the legacy management-report component bundle

**Targets:**
- `apihub-fe/components/performance/PerformanceActionPlanPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceAiObservabilityPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceAiRegenerateButton.tsx` (create)
- `apihub-fe/components/performance/PerformanceExecutiveSummaryPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (create)
- `apihub-fe/components/performance/PerformancePrintActions.tsx` (create)
- `apihub-fe/components/performance/PerformanceRegressionTrendPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceReportDecisionHeader.tsx` (create)
- `apihub-fe/components/performance/PerformanceRiskMatrixPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceRootCauseHintsPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceStepRiskPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceTechnicalFindingsPanel.tsx` (create)
- `apihub-fe/app/globals.css` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Recreate each component exactly from its `eb416dd` version. These files form one internal component bundle centered on `PerformanceManagementReportPanel`.
- `PerformanceManagementReportPanel` accepts nullable `report`, `insightReport`, `aiReport`, and `baselineComparison`, plus optional `performanceResultId` and success/error/update callbacks. It composes all restored report sections and renders `performance.reportUnavailable` if every report input is absent.
- Preserve the print classes `performance-report-print-root` and `performance-report-print-hidden`. Restore the `@media print` block from `eb416dd` so only the report root is visible, is positioned at the printable page origin, and hides action buttons.
- Keep this bundle dormant: do not import it from `PerformanceTestsContent` or either route. The current backend payload exposes `aiReport: PerformanceAiReport` and does not expose the legacy management-report fields through its active result/export contract.
- Do not change backend files, add endpoints, or substitute `PerformanceAiReport` for `PerformanceAiManagementReport`.

**Done When:**
- All thirteen legacy report component files exist and their internal imports resolve.
- The management report composition and browser-print action match `eb416dd`.
- The active performance screen has no new dependency on the legacy report bundle.
- Global print styling is restored without changing existing body/theme rules.

**Verification:**
- Manual: `cd apihub-fe; npx eslint components/performance/PerformanceActionPlanPanel.tsx components/performance/PerformanceAiNarrativePanel.tsx components/performance/PerformanceAiObservabilityPanel.tsx components/performance/PerformanceAiRegenerateButton.tsx components/performance/PerformanceExecutiveSummaryPanel.tsx components/performance/PerformanceManagementReportPanel.tsx components/performance/PerformancePrintActions.tsx components/performance/PerformanceRegressionTrendPanel.tsx components/performance/PerformanceReportDecisionHeader.tsx components/performance/PerformanceRiskMatrixPanel.tsx components/performance/PerformanceRootCauseHintsPanel.tsx components/performance/PerformanceStepRiskPanel.tsx components/performance/PerformanceTechnicalFindingsPanel.tsx` must exit successfully.
- Manual: `Select-String -Path apihub-fe/app/globals.css -Pattern 'performance-report-print-root|performance-report-print-hidden'` must find both restored print selectors.
- Manual: `rg -n 'PerformanceManagementReportPanel' apihub-fe/app` must print no route import and exit with the normal no-match status.

### TASK-03: Restore active report, dataset, schedule, SLO, and chart panels

**Targets:**
- `apihub-fe/components/performance/PerformanceAiReportPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceDatasetPanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceSchedulePanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceSloScorePanel.tsx` (create)
- `apihub-fe/components/performance/PerformanceAnalysisPanel.tsx` (modify)
- `apihub-fe/components/performance/PerformanceChartsPanel.tsx` (modify)
- `apihub-fe/components/performance/PerformanceRunSummaryTable.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Use the `eb416dd` versions as the recovery source for all seven targets.
- `PerformanceAiReportPanel` accepts `{ report?: PerformanceAiReport | null; sloScore?: PerformanceSloScore | null }`, renders `PerformanceSloScorePanel` first, shows `reportNotGenerated` when the report is absent, and otherwise renders status/source chips, executive summary, business impact, positive/negative lists, risks, recommendations, technical detail, and generated timestamp.
- `PerformanceSloScorePanel` accepts `{ score?: PerformanceSloScore | null; compact?: boolean }`. Null score renders nothing; compact mode shows score and grade only; full mode includes status, strengths, weaknesses, recommendations, and metric score rows.
- `PerformanceDatasetPanel` accepts project ID, process-flow parameter names, selected dataset ID, mapping, and dataset/mapping callbacks. It must load active datasets for the project, preview up to the server-provided rows, map process parameters to dataset fields, upload CSV/JSON with the current mapping, validate manual-row JSON as a non-array object, add the row, and show API/parse failures in its alert.
- `PerformanceSchedulePanel` accepts `{ projectId, processFlowId, requestSnapshot, onRunStarted }`, defaults timezone to the browser zone or `Europe/Istanbul`, provides hourly/daily/weekly cron shortcuts, creates schedules, toggles enabled state, runs now, and deactivates. Every action (`create`, `setEnabled`, `runNow`, `deactivate`) must catch failures, set the panel error alert, and retain successful reload behavior from `eb416dd`.
- Restore `PerformanceAnalysisPanel`'s optional `sloScore` prop and render the full SLO panel above threshold/analysis/environment details. Its empty state must account for SLO-only data.
- Restore `PerformanceChartsPanel`'s active-thread time series. Build `+1` events from valid `startedAt` and `-1` events from valid `finishedAt`, order same-timestamp start events before finish events, clamp the running count to zero, and render `Active Threads Over Time` only when the series is non-empty.
- Restore `PerformanceRunSummaryTable`'s SLO column after status and use compact `PerformanceSloScorePanel`; render `-` for records without a score.

**Done When:**
- The active AI report and SLO panels render current backend `aiReport` and `sloScore` shapes.
- Dataset UI supports select, preview, mapping, upload, and manual rows.
- Schedule UI supports create, toggle, run-now, and deactivate with visible failures.
- Analysis and history display SLO data.
- Active-thread charting is restored without changing existing response-time, throughput/error, or percentile charts.

**Verification:**
- Manual: `cd apihub-fe; npx eslint components/performance/PerformanceAiReportPanel.tsx components/performance/PerformanceDatasetPanel.tsx components/performance/PerformanceSchedulePanel.tsx components/performance/PerformanceSloScorePanel.tsx components/performance/PerformanceAnalysisPanel.tsx components/performance/PerformanceChartsPanel.tsx components/performance/PerformanceRunSummaryTable.tsx` must exit successfully.
- Manual: `rg -n 'PerformanceSloScorePanel|Active Threads Over Time|performanceDatasetService|performanceScheduleService' apihub-fe/components/performance` must find the restored integrations.
- Manual: `git diff --check -- apihub-fe/components/performance` must report no whitespace errors.

### TASK-04: Restore the shared performance screen and route integration

**Targets:**
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` (create)
- `apihub-fe/app/dashboard/performance/page.tsx` (modify)
- `apihub-fe/app/[projectShortCode]/performance/page.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Recreate `PerformanceTestsContent` from `eb416dd` with props `{ projectShortCode?: string; useDashboardProjectContext?: boolean }`. It is the single owner of run settings, datasets, schedules, live results, history, details, comparisons, baselines, validation notes, and exports.
- Dashboard context uses `useProject()`. Project-route context resolves the project with `projectService.getByShortCode(projectShortCode)`. Load flows with `processFlowService.getAll()` and active environments with `generalWebSystemService.getAll()`, filter both by selected project, select the first valid flow/environment, and leave the value empty when none exists.
- Build one memoized `PerformanceRequest` containing environment, flow/project IDs, load settings, thresholds, `testDataId`, `datasetMapping`, and optional base URL. Use the same request object for immediate runs and schedule snapshots.
- Keep a single high-load confirmation when `threadCount > 500` or `threadCount >= 100 && rampUpPeriod === 0`; include threshold values in the confirmation text and do not start the API call when cancelled.
- Detail loading must use `Promise.allSettled` for detail and analysis. A detail failure closes the modal and reports the error. An analysis-only failure keeps detail/history/result data visible and sets a user-facing analysis error.
- Resolve detail report data in this order: analysis payload, selected history record, selected in-memory result. Pass both `detailAiReport` and `detailSloScore` to the first Report tab; pass `detailSloScore` to Analysis. Preserve the eight-tab order: Report, Analysis, Validation, Step Summary, Thread Detail, Charts, Error Analysis, Export.
- When project context changes, prevent stale flow/environment/dataset selections by loading a first valid value or an empty value. The dashboard route must render `PerformanceTestsContent` with `key={selectedProject?.projectId ?? 'no-project'}`, which remounts dataset and mapping state for a new project.
- Replace the duplicated dashboard page implementation with the thin wrapper from `eb416dd`, but retain the current authorization addition: `<DashboardLayout requiredPermission="MENU.PERFORMANCE.VIEW">`. Preserve the title, FloatingChat suggestions, bottom offset, and selected project short code.
- Replace the legacy runner/results-grid project page with the shared screen wrapper from `eb416dd`. Keep `params` typed as `Promise<{ projectShortCode: string }>` and unwrap it with React `use(params)` for Next.js 16. Preserve the existing heading, fixed chat button, and `AiChatDialog`.
- Do not modify `Apihub/`, authorization services/components, `DashboardLayout`, or `.idea/vcs.xml`.

**Done When:**
- Both routes render the same shared performance implementation.
- Dashboard access remains protected by `MENU.PERFORMANCE.VIEW`.
- Project route uses asynchronous Next.js 16 params.
- Immediate and scheduled runs share dataset, threshold, timeout, and load configuration.
- Report and Analysis tabs display current AI/SLO data, while analysis-only failures leave available detail data usable.
- All 21 files named in the recovery request exist.
- The frontend builds, backend files remain unchanged, and the pre-existing `.idea/vcs.xml` edit is untouched.

**Verification:**
- Manual: `cd apihub-fe; npx eslint components/performance/PerformanceTestsContent.tsx app/dashboard/performance/page.tsx 'app/[projectShortCode]/performance/page.tsx'` must exit successfully.
- Manual: `cd apihub-fe; npm run build` must complete successfully. If it fails, report the exact error and stop under the execution contract.
- Manual: `rg -n 'PerformanceTestsContent' apihub-fe/app/dashboard/performance/page.tsx 'apihub-fe/app/[projectShortCode]/performance/page.tsx'` must find the shared component in both routes.
- Manual: `rg -n 'requiredPermission="MENU\.PERFORMANCE\.VIEW"' apihub-fe/app/dashboard/performance/page.tsx` must find the retained authorization guard.
- Manual: run `@('apihub-fe/components/performance/PerformanceActionPlanPanel.tsx','apihub-fe/components/performance/PerformanceAiNarrativePanel.tsx','apihub-fe/components/performance/PerformanceAiObservabilityPanel.tsx','apihub-fe/components/performance/PerformanceAiRegenerateButton.tsx','apihub-fe/components/performance/PerformanceAiReportPanel.tsx','apihub-fe/components/performance/PerformanceDatasetPanel.tsx','apihub-fe/components/performance/PerformanceExecutiveSummaryPanel.tsx','apihub-fe/components/performance/PerformanceManagementReportPanel.tsx','apihub-fe/components/performance/PerformancePrintActions.tsx','apihub-fe/components/performance/PerformanceRegressionTrendPanel.tsx','apihub-fe/components/performance/PerformanceReportDecisionHeader.tsx','apihub-fe/components/performance/PerformanceRiskMatrixPanel.tsx','apihub-fe/components/performance/PerformanceRootCauseHintsPanel.tsx','apihub-fe/components/performance/PerformanceSchedulePanel.tsx','apihub-fe/components/performance/PerformanceSloScorePanel.tsx','apihub-fe/components/performance/PerformanceStepRiskPanel.tsx','apihub-fe/components/performance/PerformanceTechnicalFindingsPanel.tsx','apihub-fe/components/performance/PerformanceTestsContent.tsx','apihub-fe/lib/errorUtils.ts','apihub-fe/services/performanceDatasetService.ts','apihub-fe/services/performanceScheduleService.ts') | ForEach-Object { if (-not (Test-Path -LiteralPath $_)) { throw "Missing: $_" } }`; it must produce no missing-file error.
- Manual: `git diff --exit-code 31d46bd -- Apihub` must show no backend changes.
- Manual: `(Get-FileHash -Algorithm SHA256 '.idea/vcs.xml').Hash` from the repository root must still equal `6B55E9DF7155BDC5055892FED83F8C6F6B7826FEFE4A0EE4C8F1EBB4683938AD`.
- Manual: `git diff --check` must report no whitespace errors.
