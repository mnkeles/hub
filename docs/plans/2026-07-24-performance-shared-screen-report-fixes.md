# Shared Performance Screen and AI Report Fixes - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Use the existing shared performance screen from both routes, show the persisted AI report, and correct the reliability problems found in the performance workflow.

**Architecture:** `PerformanceTestsContent` remains the only implementation of the performance workflow while both route pages become thin shells. The frontend consumes the current backend `aiReport` contract, and backend persistence generates that report once after baseline comparison and SLO scoring so the saved report uses complete result data.

**Tech / dependencies:** Next.js 16, React 19, TypeScript, next-intl, Material UI, Spring Boot, Spring Data JPA, Spring AI, Maven. No new runtime dependency or database migration is required; frontend verification uses the existing `package-lock.json` through `npm ci` when dependencies are absent.

**File map:**
- `apihub-fe/app/dashboard/performance/page.tsx` - dashboard route shell, project-scoped shared screen, title, and floating chat.
- `apihub-fe/app/[projectShortCode]/performance/page.tsx` - project route shell, Next.js 16 parameter unwrapping, shared screen, and project chat dialog.
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` - shared test workflow, project-dependent selections, high-load confirmation, and detail/analysis loading.
- `apihub-fe/components/performance/PerformanceSchedulePanel.tsx` - schedule operations and user-visible action errors.
- `apihub-fe/messages/tr.json` - Turkish missing-report message.
- `apihub-fe/messages/en.json` - English missing-report message.
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` - completed-result persistence order and single AI report generation call.

---

### TASK-01: Connect Both Routes To The Shared Report Screen

**Targets:**
- `apihub-fe/app/dashboard/performance/page.tsx` (modify)
- `apihub-fe/app/[projectShortCode]/performance/page.tsx` (modify)
- `apihub-fe/messages/tr.json` (modify)
- `apihub-fe/messages/en.json` (modify)

**Model Tier:** T3 - Power

**Implementation Notes:**
- Replace the duplicated dashboard performance implementation with a thin client component that imports and renders:
  - `DashboardLayout`
  - `PerformanceTestsContent`
  - `FloatingChat`
  - `useProject`
  - `useTranslations('performance')`
- Preserve the dashboard heading with `t('title')`. Render the shared screen as:
  ```tsx
  <PerformanceTestsContent
      key={selectedProject?.projectId ?? 'no-project'}
      useDashboardProjectContext
  />
  ```
  The key intentionally remounts local performance state when the dashboard project changes, so a flow, environment, dataset, or mapping from the previous project cannot survive the change.
- Preserve the existing dashboard `FloatingChat` title, subtitle, three translated suggestions, bottom-right position, `bottomOffset={96}`, and `projectShortCode={selectedProject?.shortCode}`. The shared component does not render its own page shell or floating chat.
- Replace the project-specific route's legacy `PerformanceRunner` and `PerformanceResultsGrid` with `PerformanceTestsContent`.
- Keep the project route as a client component because it owns the chat dialog state. Use the Next.js 16 route shape:
  ```tsx
  import { use, useState } from 'react';

  interface PageProps {
      params: Promise<{ projectShortCode: string }>;
  }

  const { projectShortCode } = use(params);
  ```
- Render the project shared screen as:
  ```tsx
  <PerformanceTestsContent
      key={projectShortCode}
      projectShortCode={projectShortCode}
      useDashboardProjectContext={false}
  />
  ```
- Preserve the project route's `Container`, `Performans Test Runner` heading, floating MUI chat button, and `AiChatDialog`. Pass the unwrapped `projectShortCode` to the chat dialog.
- Under the existing `performance` object in both locale files, add exactly:
  - Turkish: `"reportUnavailable": "Rapor verisi bulunamadı."`
  - English: `"reportUnavailable": "Report data is not available."`
- Do not remove the legacy runner, result grid, management report components, or their types; they are outside this task.

**Done When:**
- Both route files render `PerformanceTestsContent`.
- Neither route contains a second implementation of the performance workflow.
- The dashboard remounts shared performance state when `selectedProject.projectId` changes.
- The project route consumes a promised `params` value and passes the resolved short code to the shared screen and chat dialog.
- `performance.reportUnavailable` resolves in Turkish and English.

**Verification:**
- Manual: from `C:\GitRepo\hub`, run `rg -n "PerformanceTestsContent|PerformanceRunner|PerformanceResultsGrid|params: Promise|use\(params\)|reportUnavailable" apihub-fe/app/dashboard/performance/page.tsx apihub-fe/app/[projectShortCode]/performance/page.tsx apihub-fe/messages`. Both pages should reference `PerformanceTestsContent`; the project page should have no `PerformanceRunner` or `PerformanceResultsGrid` match.
- Manual: parse both message files with PowerShell: `Get-Content -Raw apihub-fe/messages/tr.json | ConvertFrom-Json | Out-Null; Get-Content -Raw apihub-fe/messages/en.json | ConvertFrom-Json | Out-Null`. The command should complete without an exception.
- Automated: if `apihub-fe/node_modules` is absent, run `npm ci` from `apihub-fe`, then run `npx eslint app/dashboard/performance/page.tsx "app/[projectShortCode]/performance/page.tsx"`. Expected result: exit code 0.

---

### TASK-02: Correct Shared Screen State And Detail Loading

**Targets:**
- `apihub-fe/components/performance/PerformanceTestsContent.tsx` (modify)

**Model Tier:** T3 - Power

**Implementation Notes:**
- Keep the component's public props unchanged:
  ```ts
  interface PerformanceTestsContentProps {
      projectShortCode?: string;
      useDashboardProjectContext?: boolean;
  }
  ```
- In `fetchProcessFlows`, always replace both the list and selection after filtering:
  ```ts
  setProcessFlows(filtered);
  setProcessFlowId(filtered.find((flow) => flow.processFlowId != null)?.processFlowId ?? 0);
  ```
  This must reset the selection to `0` when the project has no valid flow.
- In `fetchEnvironments`, always replace both the list and selection:
  ```ts
  setEnvironments(filtered);
  setEnvironment(filtered[0]?.shortCode ?? '');
  ```
  This must reset the selection to an empty string when the project has no active environment.
- Route wrappers remount this component on project identity changes, which resets `selectedDatasetId` and `datasetMapping` together with the other local state. Do not add a second project-state synchronization mechanism inside this component.
- Remove the separate `threadCount > 500` and `threadCount >= 100 && rampUpPeriod === 0` confirmation blocks. Retain one combined warning after custom-threshold validation:
  ```ts
  const requiresHighLoadConfirmation = threadCount > 500 || (threadCount >= 100 && rampUpPeriod === 0);
  if (requiresHighLoadConfirmation && !window.confirm(thresholdSummary)) {
      return;
  }
  ```
  `thresholdSummary` must continue to include preset, max error rate, average, P95, P99, and minimum throughput values.
- Replace the silent `performanceService.getAnalysis(performanceResultId).catch(() => null)` with independent settled results:
  ```ts
  const [detailResult, analysisResult] = await Promise.allSettled([
      performanceService.getPerformanceDetail(performanceResultId),
      performanceService.getAnalysis(performanceResultId),
  ]);
  ```
- If `detailResult.status === 'rejected'`, throw its `reason` so the existing outer catch reports that the required detail failed.
- If detail succeeds, call `setDetailData(detailResult.value)` even when analysis fails.
- If analysis succeeds, call `setAnalysisData(analysisResult.value)`. If it fails, leave `analysisData` as `null` and call `setError(getErrorMessage(analysisResult.reason, 'Analiz verisi yüklenemedi'))`.
- Preserve the existing report resolution order: `analysisData?.aiReport`, selected history `aiReport`, then selected in-memory result `aiReport`. This is what allows a saved history report to remain visible during an analysis endpoint failure.
- Do not reintroduce `PerformanceManagementReportPanel` or `aiManagementReport` handling.

**Done When:**
- Empty flow/environment results clear their corresponding selections.
- A high-load request produces at most one confirmation dialog.
- A failed analysis request is visible to the user.
- Successful thread detail and history/result report data remain usable when analysis fails.
- The report tab continues to render `PerformanceAiReportPanel` with `detailAiReport` and `detailSloScore`.

**Verification:**
- Manual: run `rg -n "setProcessFlowId|setEnvironment|requiresHighLoadConfirmation|Promise.allSettled|Analiz verisi yüklenemedi|PerformanceAiReportPanel" apihub-fe/components/performance/PerformanceTestsContent.tsx`. Confirm there is one high-load `window.confirm` path and no `getAnalysis(...).catch(() => null)`.
- Automated: from `apihub-fe`, run `npx eslint components/performance/PerformanceTestsContent.tsx`. Expected result: exit code 0.

---

### TASK-03: Surface Schedule Action Failures

**Targets:**
- `apihub-fe/components/performance/PerformanceSchedulePanel.tsx` (modify)

**Model Tier:** T2 - Balanced

**Implementation Notes:**
- Keep the existing schedule service methods and successful reload behavior.
- Update `handleToggle`, `handleRunNow`, and `handleDeactivate` so each method:
  1. calls `setError(null)` before the request;
  2. wraps its service call and `loadSchedules()` call in `try/catch`;
  3. uses the existing `errorMessage(error, fallback)` helper in the catch;
  4. stores a specific fallback in the existing `error` state.
- Use these fallback messages:
  - toggle: `Schedule durumu güncellenemedi`
  - run now: `Schedule çalıştırılamadı`
  - deactivate: `Schedule pasifleştirilemedi`
- In `handleRunNow`, call `onRunStarted(result)` only after `performanceScheduleService.runNow(...)` succeeds. A failed request must not add an in-memory running result.
- Do not change the schedule request contract, buttons, cron presets, or layout.

**Done When:**
- Toggle, run-now, and deactivate failures render in the panel's existing error alert.
- These handlers no longer produce unhandled rejected promises.
- Successful operations still reload the schedule list, and run-now still reports its result to the parent.

**Verification:**
- Manual: run `rg -n "handleToggle|handleRunNow|handleDeactivate|setError\(null\)|güncellenemedi|çalıştırılamadı|pasifleştirilemedi" apihub-fe/components/performance/PerformanceSchedulePanel.tsx`. Each handler should contain a catch path.
- Automated: from `apihub-fe`, run `npx eslint components/performance/PerformanceSchedulePanel.tsx`. Expected result: exit code 0.

---

### TASK-04: Generate The Completed AI Report Once

**Targets:**
- `Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java` (modify)

**Model Tier:** T2 - Balanced

**Implementation Notes:**
- In `persistPerformanceResult(...)`, preserve construction and assignment of final status, step summaries, run summary, threshold result, analysis summary, error analysis, and environment metrics.
- Remove the first call that currently assigns an AI report before the initial repository save:
  ```java
  entity.setAiReport(performanceAiReportService.generateReport(entity, runningItem));
  ```
- Keep the initial `performanceResultRepository.save(entity)` because automatic baseline comparison needs a completed saved entity.
- Preserve this post-save order exactly:
  ```java
  PerfRsltEntity saved = performanceResultRepository.save(entity);
  PerfRsltEntity compared = performanceBaselineService.applyAutomaticBaselineComparison(saved);
  compared.setSloScore(performanceSloScoreService.calculate(
          compared.getRunSummary(),
          compared.getThresholdConfig(),
          compared.getBaselineComparison()
  ));
  compared.setAiReport(performanceAiReportService.generateReport(compared, runningItem));
  compared.setValidationChecklist(performanceValidationChecklistBuilder.build(compared, runningItem));
  performanceResultRepository.save(compared);
  ```
- Do not change `PerformanceAiReportService`: it already catches AI client and parsing failures and returns a `FALLBACK` report.
- Do not change lazy legacy-report creation in `PerformanceService.getAnalysis(...)`.

**Done When:**
- `persistPerformanceResult(...)` contains exactly one `performanceAiReportService.generateReport(...)` call.
- The retained call occurs after automatic baseline comparison and SLO score assignment.
- Fallback behavior and final persistence remain intact.

**Verification:**
- Manual: from `C:\GitRepo\hub`, run `rg -n -C 8 "generateReport\(compared|setSloScore|applyAutomaticBaselineComparison" Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java`. The output should show baseline comparison, SLO score, one AI report call, validation, and final save in that order.
- Manual: run `$count = (Select-String -Path Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java -Pattern 'performanceAiReportService.generateReport' | Measure-Object).Count; $count` from PowerShell. Expected result: `1`.
- Automated: if `Apihub/mvnw.cmd` exists, run `.\mvnw.cmd -Dtest=PerformanceAiReportServiceTest test` from `Apihub`; otherwise, if `mvn` exists, run `mvn -Dtest=PerformanceAiReportServiceTest test`. Expected result: exit code 0. If neither exists, record Maven as unavailable and rely on the two source-level checks above.

---

### TASK-05: Run Integrated Performance Verification

**Targets:**
- No file modifications (verification only)

**Model Tier:** T2 - Balanced

**Implementation Notes:**
- Do not edit source, generated, lock, or configuration files in this task.
- From `apihub-fe`, run `npm ci` only if `node_modules` is absent. Use the committed `package-lock.json`; do not run `npm install` and do not update dependency versions.
- Run targeted lint:
  ```powershell
  npx eslint app/dashboard/performance/page.tsx "app/[projectShortCode]/performance/page.tsx" components/performance/PerformanceTestsContent.tsx components/performance/PerformanceAiReportPanel.tsx components/performance/PerformanceSchedulePanel.tsx services/performanceService.ts types/performance.ts
  ```
- Run the full production build with `npm run build`.
- Parse `messages/tr.json` and `messages/en.json` with `ConvertFrom-Json`.
- Run these source checks from the repository root:
  ```powershell
  rg -n "PerformanceTestsContent|PerformanceAiReportPanel|detailAiReport" apihub-fe/app apihub-fe/components/performance
  rg -n "performanceAiReportService.generateReport" Apihub/src/main/java/etiya/omniAutomation/service/ApiCallServiceImpl.java
  ```
- If Maven or the Maven wrapper is available, run all performance-focused backend tests:
  ```powershell
  mvn -Dtest='*Performance*Test' test
  ```
  Use `.\mvnw.cmd` in place of `mvn` when the wrapper exists.
- Start the frontend development server with `npm run dev`. If port `4054` is occupied, try `npx next dev -p 4055` and increment the port number by one until an unused port is found. Keep the server running and report the resulting local URL.
- Verification failures must be reported with the failing command and first actionable error. Because this task permits no file changes, stop rather than fixing a failure here.

**Done When:**
- Targeted frontend lint status is known.
- Full Next.js build status is known.
- Both locale files parse successfully.
- Source checks show both routes using the shared screen and exactly one completed-run AI generation call.
- Backend test status is known or Maven unavailability is recorded.
- The frontend development server is running and its URL is reported.

**Verification:**
- Manual: confirm the command results listed in Implementation Notes and report each as passed, failed, or unavailable.
- Automated: the targeted ESLint, Next.js build, locale parse, and available Maven test commands above are the automated verification for this task.
