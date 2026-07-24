# Shared Performance Screen and AI Report Fixes Design

## Goal

Use one performance test implementation across both frontend routes, display the persisted AI report returned by the current backend contract, and fix the reliability issues found while reviewing the performance workflow.

## Scope

In scope:

- Use `PerformanceTestsContent` from both `/dashboard/performance` and `/{projectShortCode}/performance`.
- Preserve each route's existing page shell, heading, and chat entry point.
- Display `aiReport` and `sloScore` in the first detail-modal tab.
- Support Next.js 16 asynchronous dynamic route parameters.
- Reset project-dependent selections when the selected project changes.
- Consolidate duplicate high-load confirmation dialogs.
- Surface analysis request failures without preventing available detail data from rendering.
- Handle schedule action failures inside the schedule panel.
- Generate the saved AI report once, after baseline comparison and SLO scoring.
- Add the missing `performance.reportUnavailable` translations.
- Run frontend lint/build and available backend verification.

Out of scope:

- Restoring the removed management/insight/AI-management backend report model.
- Database schema or Liquibase changes.
- Changing the `PerformanceAiReport` public contract.
- Removing legacy report components that may still be useful for later work.
- Adding a new AI provider, model, or configuration surface.

## Frontend Architecture

`PerformanceTestsContent` is the single owner of performance test state and behavior, including test execution, live monitoring, history, details, datasets, schedules, comparisons, and exports.

The dashboard route becomes a thin client wrapper that retains `DashboardLayout`, the page title, and `FloatingChat`, and renders `PerformanceTestsContent` with dashboard project context enabled.

The project-specific route retains its container, heading, floating chat button, and chat dialog. It unwraps the Next.js 16 `params` promise and passes `projectShortCode` to `PerformanceTestsContent` with dashboard project context disabled.

The first detail tab renders `PerformanceAiReportPanel`. Its data resolution order is:

1. `/performance/analysis` payload
2. selected history item
3. selected in-memory result

The panel receives both `aiReport` and `sloScore`. A missing report continues to render the existing `reportNotGenerated` message rather than a raw translation key.

## Frontend Reliability

### Project-Dependent State

When project context changes, the screen must not retain values belonging to the previous project. Flow and environment loading must explicitly select the first valid value or reset to an empty value when none exists. Dataset selection and dataset mapping must also reset for a new project.

This prevents a request from combining a new project ID with an old flow, environment, or dataset ID.

### High-Load Confirmation

A test request that meets a high-load warning condition must show one confirmation dialog. The dialog includes the selected threshold configuration so users can make the decision with the relevant limits visible. Cancelling leaves the form unchanged and does not start a request.

### Detail and Analysis Loading

Detail and analysis calls remain independent. If thread detail loads but `/performance/analysis` fails, the modal still renders data already available from history or the selected result and shows a user-visible error for the failed analysis request. The failure must not be silently converted to `null`.

If the required detail request fails, the existing page-level error handling remains responsible for reporting that failure.

### Schedule Actions

Schedule enable/disable, run-now, and deactivate operations catch API failures and render them in the schedule panel's existing error alert. Successful operations retain the current reload behavior.

### Translations

Add these messages under the existing `performance` namespace:

- Turkish: `"reportUnavailable": "Rapor verisi bulunamadı."`
- English: `"reportUnavailable": "Report data is not available."`

## Backend AI Report Flow

Performance result persistence computes and assigns run summaries, thresholds, analysis, errors, and environment metrics before its initial save. The initial save exists so automatic baseline comparison can work with the completed result.

After baseline comparison:

1. Calculate and assign the SLO score.
2. Generate the AI report once using the completed result, baseline comparison, and SLO score.
3. Build the validation checklist.
4. Save the completed entity.

The existing `PerformanceAiReportService` behavior remains unchanged: AI client, parse, or configuration failures return a persisted `FALLBACK` report and do not fail the performance run.

Removing the pre-baseline AI call avoids duplicate model cost and latency and ensures the retained report sees the most complete result data.

## Error Handling

- Missing AI data displays the localized not-generated state.
- AI generation failure persists the deterministic fallback report.
- Analysis loading failure is visible but does not discard available history/result report data.
- Project changes cannot leave stale selectable IDs active.
- Schedule action failures remain inside the schedule UI instead of becoming unhandled promise rejections.
- Existing unrelated page and API error behavior remains unchanged.

## Testing and Verification

Frontend:

- Install the locked dependencies with `npm ci` when network access is available.
- Run targeted ESLint for both route pages, `PerformanceTestsContent`, `PerformanceAiReportPanel`, `PerformanceSchedulePanel`, the performance service, and performance types.
- Run `npm run build`.
- Parse both locale JSON files.
- Start the development server and confirm it provides a local URL.
- Confirm both route files render `PerformanceTestsContent` and the shared detail tab renders `PerformanceAiReportPanel`.

Backend:

- Run the performance-focused Maven tests when Maven or a Maven wrapper is available.
- If Maven remains unavailable, record that limitation and perform source-level verification of the single AI generation call and fallback path.

Acceptance criteria:

- Both performance routes use the shared screen.
- The report tab displays the backend `aiReport` instead of `performance.reportUnavailable`.
- Existing records without a report show a localized empty state.
- Switching projects cannot submit stale flow, environment, or dataset values.
- High-load requests show at most one confirmation.
- Analysis failures are visible while available detail/report data remains usable.
- Schedule action failures are handled visibly.
- A completed performance run invokes AI report generation once after SLO scoring.
- Frontend lint and build results are known and reported.
