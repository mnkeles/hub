# Performance AI Report Design

## Goal

Complete the remaining performance test screen gaps and add a saved AI-generated report that makes a performance test result understandable for managers and non-specialist users.

The report must answer, in plain business language:

- Is the test acceptable or problematic?
- What went well?
- What went badly?
- What are the main risks?
- What should the team do next?
- Which technical metrics support that conclusion?

## Scope

In scope:

- Generate an AI report when a performance test finishes.
- Save the report with the performance result.
- Show a new `Rapor` tab as the first tab in the performance detail modal.
- Keep a deterministic fallback report if AI generation fails.
- Add the saved report to analysis/history/export payloads.
- Complete the known performance UI gaps that do not require external infrastructure.
- Wire `timeoutMs` into the request execution path.
- Keep environment metrics as optional fallback data until a real metrics integration is available.

Out of scope:

- Real Prometheus, Grafana, Actuator, Kubernetes, database, or APM metrics integration.
- Test data set selection, because no concrete test data source or UI contract exists in the current code.
- AI report regeneration from the UI.
- Excel, PDF, JTL, and HTML report generation.
- Fixing unrelated generated `.next/dev` validator cache issues, except for reporting verification results.

## AI Report Model

Add a new backend DTO named `PerformanceAiReport`.

Fields:

- `executiveSummary`: short manager-friendly summary.
- `overallStatus`: concise result label such as `PASSED`, `FAILED`, `STOPPED`, or `ERROR`.
- `businessImpact`: plain-language impact statement.
- `goodPoints`: list of positive observations.
- `badPoints`: list of problematic observations.
- `risks`: list of risks or uncertainties.
- `recommendedActions`: list of concrete next steps.
- `technicalDetails`: concise technical explanation for engineers.
- `source`: `AI` or `FALLBACK`.
- `generatedAt`: generation timestamp.

Persist this report in `perf_rslt.ai_report` as JSONB.

Expose `aiReport` from:

- `PerformanceResultDto`
- `PerformanceSummaryResult`
- `PerformanceExportPayload`
- `/performance/analysis`
- `/performance/getHistory`
- JSON export

CSV export should include at least the executive summary, overall status, source, and recommended actions.

## AI Report Generation

Add a backend service named `PerformanceAiReportService`.

The service generates the report after a performance run is finalized, using only saved and computed performance data:

- run summary
- threshold result
- automatic analysis summary
- error analysis
- step summaries
- baseline comparison, if available
- environment metrics availability state

The AI prompt must not send raw thread detail unless necessary. It should send compact structured data to control token cost:

- top problem step
- slowest step
- highest P95/P99 steps
- highest error step
- threshold failure reasons
- summary metrics
- grouped errors
- a small list of worst step summaries

The prompt must instruct the model to return strict JSON matching the `PerformanceAiReport` shape. If the response cannot be parsed, the service must create a fallback report.

The AI call must use the existing Spring AI/OpenAI configuration from `application.yml`. The OpenAI key must stay server-side and must not be exposed to frontend code or DTOs.

AI report generation must never fail the performance test. On timeout, model error, parsing error, or missing AI configuration, save a fallback report with `source = FALLBACK`.

## Fallback Report

The fallback report is built from rules and saved in the same `PerformanceAiReport` structure.

Rules:

- If threshold result passed, summarize the test as acceptable and list the strongest metrics.
- If threshold result failed, summarize the failed threshold reasons and highlight the most problematic step.
- If errors exist, include grouped error information and failed request count.
- If no environment metrics are available, add a risk explaining that infrastructure root-cause analysis is limited.
- Recommended actions should be concrete, such as investigating the highest P95/P99 step, checking error-heavy endpoints, and comparing against the baseline.

## Performance Screen Completion

### Route Consistency

The route `app/[projectShortCode]/performance/page.tsx` currently uses legacy components with an empty result grid. It must no longer show the outdated experience.

Create a shared upgraded performance screen component and use it from both `app/dashboard/performance/page.tsx` and `app/[projectShortCode]/performance/page.tsx`. The shared component must accept project context explicitly so the project route does not depend on dashboard-only selected-project state.

The user must see the same completed performance experience regardless of which performance route they enter.

### Charts

Keep the existing charts:

- Response Time Over Time
- Throughput / Error Rate Over Time
- Step Average/P95/P99

Add:

- Active Threads Over Time

The active thread chart can be derived from thread detail timestamps. No new time-series persistence is required for this scope.

### Test Start Parameters

Keep existing working behavior:

- `durationSeconds`
- `loopCount`
- `thinkTimeMs`

Wire:

- `timeoutMs` into the actual request execution path so request calls honor the configured timeout.

Keep as metadata only:

- `environmentBaseUrl`, because the current request execution path resolves target URLs from the existing flow/system definitions and this scope must not introduce a second URL resolution path.

Do not add test data selection UI yet:

- `testDataId` remains backend request model metadata until a real test data source is defined.

### Environment Metrics

Environment metrics remain optional fallback data.

When unavailable:

- UI must show a clear unavailable message.
- AI/fallback report must mention that infrastructure metrics are unavailable and root-cause confidence is limited.

No real metrics connector is added in this scope.

## UI Design

Add `PerformanceAiReportPanel`.

Detail modal tab order:

1. `Rapor`
2. `Analiz`
3. `Validation`
4. `Adim Ozeti`
5. `Thread Detay`
6. `Grafikler`
7. `Hata Analizi`
8. `Export`

The `Rapor` tab shows:

- top-level status chip
- executive summary
- business impact
- good points
- bad points
- risks
- recommended actions
- technical details
- source indicator (`AI` or `Fallback`)
- generated timestamp

If no report exists, show a clear message: `Rapor henuz olusturulmadi.`

The report should be readable by managers: short paragraphs, concrete lists, minimal jargon. Technical detail remains below the executive section.

## Error Handling

- AI report generation errors must not change the performance run status.
- AI parse errors must produce fallback reports.
- Missing environment metrics must not break UI or report generation.
- Missing report data in old records must be handled gracefully by the UI.
- Long prompts should be avoided by summarizing input data before the AI call.

## Testing

Backend unit tests:

- AI JSON response creates `source = AI`.
- AI exception creates `source = FALLBACK`.
- Invalid AI JSON creates `source = FALLBACK`.
- Fallback report contains good points, bad points, risks, and recommended actions.
- Failed threshold data appears in the report.
- Export payload includes `aiReport`.
- CSV export includes report summary fields.

Frontend checks:

- ESLint for the performance page and new performance report component.
- Detail modal renders `Rapor` as the first tab.
- Empty or missing report renders a clear fallback message.
- Fallback source is visible when `source = FALLBACK`.

Verification commands:

- `npx eslint app/dashboard/performance/page.tsx components/performance/*.tsx services/performanceService.ts types/performance.ts`
- `npm run build`
- Backend performance tests if Maven is available.

If `npm run build` still fails because of generated `.next/dev/types/validator.ts`, report that separately from the feature implementation.
