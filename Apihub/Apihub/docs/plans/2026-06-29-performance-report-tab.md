# Performance Report Tab - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Add a project-manager-friendly **Rapor** tab to the performance detail modal, backed by a rule-based management report generated from existing performance analysis data.

**Architecture:** The backend will produce a dynamic `PerformanceManagementReport` from already persisted performance result data; no database migration is required. The existing `/performance/analysis` payload will include the new report field, and the frontend will render it in a new tab inside the existing detail modal without changing the existing technical tabs.

**Tech / dependencies:** Spring Boot 3.5, Java 21 records/services, JUnit 5, Next.js 16, React 19, MUI 7, TypeScript, next-intl. No new runtime dependency is required.

**File map:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementRiskLevel.java` - Backend enum for report risk levels.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementProblemArea.java` - Backend record for a highlighted report problem area.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementSlaItem.java` - Backend record for each SLA/threshold report item.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementReport.java` - Backend report DTO returned in analysis payloads.
- `src/main/java/etiya/omniAutomation/service/PerformanceManagementReportBuilder.java` - Rule-based builder that derives the report from existing summaries and analysis fields.
- `src/test/java/etiya/omniAutomation/service/PerformanceManagementReportBuilderTest.java` - Backend unit tests for risk level, summaries, problem areas, recommendations, and missing data.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` - Adds the `managementReport` field to the analysis/export payload.
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` - Populates the new report field when building payloads.
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` - Uses `PerformanceExportService` for `/performance/analysis` so the management report is included.
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` - Updates constructor expectations and verifies the report is present.
- `../../apihub-fe/apihub-fe/types/performance.ts` - Frontend TypeScript contract for the management report.
- `../../apihub-fe/apihub-fe/messages/tr.json` - Turkish labels for the report tab and panel.
- `../../apihub-fe/apihub-fe/messages/en.json` - English labels for the report tab and panel.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` - New frontend panel for the report tab.
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` - Adds the **Rapor** tab and passes fetched report data to the panel.

---

### TASK-01: Backend Management Report Model And Builder

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementRiskLevel.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementProblemArea.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementSlaItem.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementReport.java` (create)
- `src/main/java/etiya/omniAutomation/service/PerformanceManagementReportBuilder.java` (create)
- `src/test/java/etiya/omniAutomation/service/PerformanceManagementReportBuilderTest.java` (create)

**Model Tier:** T3

**Implementation Notes:**
- Add enum:
  ```java
  package etiya.omniAutomation.business.dto;

  public enum PerformanceManagementRiskLevel {
      LOW,
      MEDIUM,
      HIGH,
      CRITICAL
  }
  ```
- Add record:
  ```java
  package etiya.omniAutomation.business.dto;

  public record PerformanceManagementProblemArea(
          String title,
          String stepName,
          String metric,
          String value,
          String impact
  ) {
  }
  ```
- Add record:
  ```java
  package etiya.omniAutomation.business.dto;

  public record PerformanceManagementSlaItem(
          String metric,
          boolean passed,
          String expected,
          String actual,
          String explanation
  ) {
  }
  ```
- Add record:
  ```java
  package etiya.omniAutomation.business.dto;

  import java.util.List;

  public record PerformanceManagementReport(
          String overallStatus,
          PerformanceManagementRiskLevel riskLevel,
          String executiveSummary,
          List<PerformanceManagementSlaItem> slaSummary,
          List<PerformanceManagementProblemArea> problemAreas,
          String trendSummary,
          List<String> recommendedActions,
          String detailExplanation
  ) {
  }
  ```
- Add `@Service public class PerformanceManagementReportBuilder`.
- Implement this public method:
  ```java
  public PerformanceManagementReport build(
          PerformanceRunSummary runSummary,
          PerformanceThresholdResult thresholdResult,
          PerformanceAnalysisSummary analysisSummary,
          PerformanceErrorAnalysis errorAnalysis,
          PerformanceEnvironmentMetrics environmentMetrics,
          PerformanceComparisonResult baselineComparison,
          List<PerformanceSummary> stepSummaries
  )
  ```
- `overallStatus` must be one of these readable labels:
  - `Passed` for `COMPLETED_PASSED` or threshold passed.
  - `Failed` for `COMPLETED_FAILED`, failed threshold, or failed/error metrics.
  - `Stopped` for `STOPPED`.
  - `Error` for `ERROR`.
  - `Running` for `RUNNING` or `STOPPING`.
  - `Unknown` when status cannot be derived.
- Risk level rules:
  - `CRITICAL` when run status is `ERROR`, or `errorRate >= 10`, or P99 is at least 2x configured max P99.
  - `HIGH` when threshold failed, or `errorRate >= 1`, or P95 exceeds configured max P95.
  - `MEDIUM` when baseline comparison contains at least one metric with `improvement == false`, or environment metrics contain warnings.
  - `LOW` when the test passed and no warning/regression condition applies.
- Build `slaSummary` for these five metrics when `runSummary` exists: `Error rate`, `Average response time`, `P95`, `P99`, `Throughput`. Use `thresholdResult.thresholds()` when present; otherwise use `PerformanceThresholdConfig.defaults()`. Expected/actual fields must be formatted as strings with units, for example `<= 3000 ms`, `4200 ms`, `>= 20.00 req/s`, `18.44 req/s`, `<= 1.00%`, `2.40%`.
- Build `problemAreas` from `analysisSummary`:
  - Add one item for `problemStepName` when present.
  - Add one item for `slowestStepName` when present and different from problem step.
  - Add one item for `highestErrorStepName` when present and different from earlier entries.
  - Look up matching `PerformanceSummary` by `stepName` to include the most relevant metric value.
- `trendSummary` must be:
  - `Baseline comparison is not available. Select a baseline run to compare performance trend.` when `baselineComparison` is null or has no metrics.
  - Otherwise summarize counts of metrics where `improvement == true`, `improvement == false`, and `improvement == null`.
- `recommendedActions` must be deterministic:
  - If threshold failed, include `Review the threshold failure reasons and prioritize the listed slowest or failing step.`
  - If P95/P99 threshold failed, include `Investigate high percentile latency on the problem step before increasing load.`
  - If error count is greater than zero, include `Inspect failed requests and group errors by step before rerunning the test.`
  - If environment metrics are unavailable, include `Connect environment metrics to separate application, database, and infrastructure causes.`
  - If baselineComparison has regressions, include `Compare the regressed metrics with the selected baseline before approving the release.`
  - If no rule adds an action, include `Keep this result as a baseline candidate and monitor the next run for regressions.`
- `detailExplanation` must be a paragraph-style string that mentions the overall result, risk level, main problem step when available, and baseline availability.
- Add unit tests covering:
  - Passed threshold produces `LOW` and a baseline-missing trend message.
  - Failed P95/P99 produces `HIGH` or `CRITICAL` depending on severity and includes percentile action.
  - `ERROR` run produces `CRITICAL`.
  - Baseline regression produces at least `MEDIUM` and a regression action.
  - Missing environment metrics produces the environment metrics action.

**Done When:**
- The four new DTO files compile as Java records/enums.
- `PerformanceManagementReportBuilder.build(...)` returns a non-null report for null-safe and full-data inputs.
- Unit tests assert risk levels, trend text, problem areas, and recommended actions.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceManagementReportBuilderTest test`. Expected: build succeeds and the new test class passes. If `mvn` is not installed in the environment, report that verification is blocked by missing Maven.

---

### TASK-02: Backend Analysis Payload Integration

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceExportPayload.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceExportService.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceService.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceExportServiceTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Extend the `PerformanceExportPayload` record with a new field named `managementReport` directly after `environmentMetrics` and before `stepSummaries`:
  ```java
  PerformanceManagementReport managementReport,
  ```
- Modify `PerformanceExportService` to inject `PerformanceManagementReportBuilder` through constructor injection. The class is already a `@Service`, so use a `private final PerformanceManagementReportBuilder performanceManagementReportBuilder;` field and a constructor.
- In `buildPayload(PerfRsltEntity result, PerformanceThreadGroup threadDetail)`, call:
  ```java
  PerformanceManagementReport managementReport = performanceManagementReportBuilder.build(
          result.getRunSummary(),
          result.getThresholdResult(),
          result.getAnalysisSummary(),
          result.getErrorAnalysis(),
          result.getEnvironmentMetrics(),
          result.getBaselineComparison(),
          result.getSummary()
  );
  ```
  Pass that value into the `PerformanceExportPayload` constructor.
- Modify `PerformanceService.getAnalysis(long performanceResultId)` to delegate to `performanceExportService.buildPayload(result, loadThreadGroup(performanceResultId))` instead of manually constructing `PerformanceExportPayload`. Keep the same exception behavior for missing result IDs.
- Keep `PerformanceService.export(...)` behavior intact; it should automatically include `managementReport` because it already uses `PerformanceExportService.buildPayload(...)`.
- Update `PerformanceExportServiceTest`:
  - Instantiate `PerformanceExportService` with `new PerformanceManagementReportBuilder()`.
  - Update all `PerformanceExportPayload` constructor expectations to include the new `managementReport` field.
  - Add an assertion in `buildsJsonPayloadFromEntityAndThreadDetail` that `payload.managementReport()` is not null.
  - Add an assertion in `buildsCsvWithRunThresholdStepAndErrorSections` that CSV generation still contains existing sections. CSV does not need to include the management report in Phase 1.

**Done When:**
- `/performance/analysis` response model includes `managementReport`.
- JSON export includes `managementReport`.
- CSV export remains compatible and does not fail when `managementReport` exists.
- Existing analysis fields remain in the same payload.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -Dtest=PerformanceExportServiceTest,PerformanceManagementReportBuilderTest test`. Expected: both test classes pass. If `mvn` is unavailable, report that verification is blocked by missing Maven.

---

### TASK-03: Frontend Performance Report Contract And Labels

**Targets:**
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify)
- `../../apihub-fe/apihub-fe/messages/tr.json` (modify)
- `../../apihub-fe/apihub-fe/messages/en.json` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Add these frontend types to `types/performance.ts` near the existing performance analysis types:
  ```ts
  export type PerformanceManagementRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

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

  export interface PerformanceManagementReport {
      overallStatus?: string | null;
      riskLevel?: PerformanceManagementRiskLevel | null;
      executiveSummary?: string | null;
      slaSummary?: PerformanceManagementSlaItem[];
      problemAreas?: PerformanceManagementProblemArea[];
      trendSummary?: string | null;
      recommendedActions?: string[];
      detailExplanation?: string | null;
  }
  ```
- Add `managementReport?: PerformanceManagementReport | null;` to `PerformanceExportPayload`.
- Add these keys under the `performance` object in `messages/tr.json`:
  - `"report": "Rapor"`
  - `"managementReport": "Performans Raporu"`
  - `"executiveSummary": "Yönetici Özeti"`
  - `"riskLevel": "Risk Seviyesi"`
  - `"slaSummary": "SLA / Threshold Özeti"`
  - `"problemAreas": "Problemli Alanlar"`
  - `"trendSummary": "Trend Özeti"`
  - `"recommendedActions": "Önerilen Aksiyonlar"`
  - `"detailExplanation": "Detay Açıklaması"`
  - `"showReportDetails": "Detayları Göster"`
  - `"hideReportDetails": "Detayları Gizle"`
  - `"reportUnavailable": "Rapor verisi bulunamadı."`
  - `"expected": "Beklenen"`
  - `"actual": "Gerçekleşen"`
  - `"impact": "Etki"`
  - `"passed": "Geçti"`
  - `"failed": "Kaldı"`
- Add matching English keys under the `performance` object in `messages/en.json`:
  - `"report": "Report"`
  - `"managementReport": "Performance Report"`
  - `"executiveSummary": "Executive Summary"`
  - `"riskLevel": "Risk Level"`
  - `"slaSummary": "SLA / Threshold Summary"`
  - `"problemAreas": "Problem Areas"`
  - `"trendSummary": "Trend Summary"`
  - `"recommendedActions": "Recommended Actions"`
  - `"detailExplanation": "Detail Explanation"`
  - `"showReportDetails": "Show Details"`
  - `"hideReportDetails": "Hide Details"`
  - `"reportUnavailable": "Report data is not available."`
  - `"expected": "Expected"`
  - `"actual": "Actual"`
  - `"impact": "Impact"`
  - `"passed": "Passed"`
  - `"failed": "Failed"`
- Keep existing keys unchanged.

**Done When:**
- `PerformanceExportPayload.managementReport` is typed in the frontend.
- Turkish and English translations include every key used by the future report panel.
- Existing performance type exports remain available.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint completes without missing type or translation-related TypeScript errors. If dependencies are not installed, report that verification is blocked by missing frontend dependencies.

---

### TASK-04: Frontend Report Panel Component

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (create)

**Model Tier:** T2

**Implementation Notes:**
- Create a client component:
  ```tsx
  'use client';

  import {
      Alert,
      Box,
      Button,
      Chip,
      Divider,
      List,
      ListItem,
      ListItemText,
      Paper,
      Typography,
  } from '@mui/material';
  import { useState } from 'react';
  import { useTranslations } from 'next-intl';
  import { PerformanceManagementReport, PerformanceManagementRiskLevel } from '@/types/performance';

  interface PerformanceManagementReportPanelProps {
      report?: PerformanceManagementReport | null;
  }
  ```
- Export default `function PerformanceManagementReportPanel({ report }: PerformanceManagementReportPanelProps)`.
- If `report` is null or undefined, render `<Alert severity="info">{t('reportUnavailable')}</Alert>`.
- Add a local helper:
  ```ts
  function riskColor(risk?: PerformanceManagementRiskLevel | null): 'success' | 'warning' | 'error' | 'default' {
      if (risk === 'LOW') return 'success';
      if (risk === 'MEDIUM') return 'warning';
      if (risk === 'HIGH' || risk === 'CRITICAL') return 'error';
      return 'default';
  }
  ```
- Render a top `Paper` containing:
  - `t('managementReport')` as heading.
  - `report.overallStatus` as a small chip or text.
  - `report.riskLevel` as a colored chip using `riskColor`.
  - `report.executiveSummary`, or `t('reportUnavailable')` when absent.
- Render SLA summary:
  - Use `report.slaSummary ?? []`.
  - Each item shows `metric`, a chip with `t('passed')` or `t('failed')`, `expected`, `actual`, and `explanation`.
  - If the list is empty, show `t('reportUnavailable')` in an info alert inside the section.
- Render problem areas:
  - Use `report.problemAreas ?? []`.
  - Each item shows `title`, `stepName`, `metric`, `value`, and `impact`.
  - Use `t('impact')` as the label for impact.
- Render trend summary:
  - Show `report.trendSummary` or `t('reportUnavailable')`.
- Render recommended actions:
  - Use `report.recommendedActions ?? []`.
  - Render each action as a list item.
  - If empty, show `t('reportUnavailable')`.
- Render detail explanation:
  - Add a state variable `detailsOpen`.
  - Button text uses `detailsOpen ? t('hideReportDetails') : t('showReportDetails')`.
  - When open, show `report.detailExplanation` or `t('reportUnavailable')`.
- Keep layout readable and consistent with existing MUI performance panels. Use `Paper variant="outlined" sx={{ p: 2 }}` for sections and a vertical `Box` with `gap: 2`.

**Done When:**
- `PerformanceManagementReportPanel` renders a useful empty state when report data is missing.
- The panel renders executive summary, risk, SLA items, problem areas, trend, actions, and expandable details when report data exists.
- The component only depends on its `report` prop and translation keys.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: TypeScript/ESLint pass for the new component. If dependencies are not installed, report that verification is blocked by missing frontend dependencies.

---

### TASK-05: Wire The Rapor Tab Into The Detail Modal

**Targets:**
- `../../apihub-fe/apihub-fe/app/dashboard/performance/page.tsx` (modify)

**Model Tier:** T2

**Implementation Notes:**
- Import the new panel:
  ```ts
  import PerformanceManagementReportPanel from '@/components/performance/PerformanceManagementReportPanel';
  ```
- After existing detail data derivations, add:
  ```ts
  const detailManagementReport = analysisData?.managementReport ?? null;
  ```
- Update the detail modal tabs so the first tab is `Rapor`:
  ```tsx
  <Tab label={t('report')} />
  <Tab label={t('analysis')} />
  <Tab label={t('validation')} />
  <Tab label={t('stepSummary')} />
  <Tab label={t('threadDetail')} />
  <Tab label={t('charts')} />
  <Tab label={t('errorAnalysis')} />
  <Tab label={t('export')} />
  ```
- Update tab content indexes:
  - `detailTab === 0`: render `<PerformanceManagementReportPanel report={detailManagementReport} />`.
  - `detailTab === 1`: existing `PerformanceAnalysisPanel`.
  - `detailTab === 2`: existing `PerformanceValidationChecklistPanel`.
  - `detailTab === 3`: existing `PerformanceStepSummaryTable`.
  - `detailTab === 4`: existing thread filters and `PerformanceThreadDetailTable`.
  - `detailTab === 5`: existing `PerformanceChartsPanel`.
  - `detailTab === 6`: existing `PerformanceErrorAnalysisPanel`.
  - `detailTab === 7`: existing `PerformanceExportActions`.
- Keep `setDetailTab(0)` in `handleViewDetail`, so the modal opens on the new `Rapor` tab.
- Do not change the history table, run button, live monitor, or existing technical panel props.

**Done When:**
- Opening performance details shows the **Rapor** tab first.
- Existing technical tabs remain reachable and render the same data as before.
- If `managementReport` is missing from the API response, the report tab shows the panel empty state instead of crashing.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: lint completes without TypeScript errors.
- Manual: Start the frontend with `npm run dev`, open the performance dashboard, open a test detail modal, and verify the tab order starts with **Rapor**. If the backend is unavailable, mock the observation by confirming the UI renders the empty state for the report tab.
