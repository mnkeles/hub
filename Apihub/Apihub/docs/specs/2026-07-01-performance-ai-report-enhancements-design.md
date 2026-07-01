# Performance AI Report Enhancements Design

## Goal

Extend the existing Performance AI Insight Report into a more controlled reporting tool.

The enhancement focuses on six areas:

- Stronger AI quality validation.
- Report schema/version metadata.
- AI report regeneration from the UI.
- Richer CSV export and frontend print/PDF export.
- A visual risk matrix in the `Rapor` tab.
- Prompt observability metadata without storing raw prompts.

Maven wrapper or build tool setup is explicitly out of scope.

## Decisions

- AI report regeneration overwrites the current `aiManagementReport` JSONB value.
- AI generation history is not stored.
- Invalid or contradictory AI output is fully rejected; partial AI narrative is not shown.
- PDF export is frontend print/PDF only; no backend PDF endpoint or PDF dependency is added.
- Prompt observability stores hashes and metadata, not raw prompt or raw response body.
- No new database columns are required; existing JSONB report fields are expanded.

## Backend Contract

Extend `PerformanceInsightReport` with:

- `schemaVersion`
- `generatedByVersion`

Extend `PerformanceAiManagementReport` with:

- `schemaVersion`
- `generatedByVersion`
- `durationMs`
- `attemptCount`
- `failureReason`
- `validationErrors`
- `promptHash`
- `inputSummaryHash`
- `responseSize`
- `promptTokens`
- `completionTokens`
- `totalTokens`

Token fields are optional and remain null when the AI provider response does not expose token usage.

Existing persisted JSONB values remain valid. Older records may have null version and metadata fields. Frontend rendering must treat those fields as optional.

## AI Validation

Add a focused backend validator:

- `PerformanceAiReportValidator`

Responsibilities:

- Validate required AI narrative fields.
- Reject unsupported action item priorities outside `P0`, `P1`, and `P2`.
- Require every action item to reference either a deterministic problem step or a deterministic problem metric.
- Reject AI text that contradicts deterministic release readiness.
- Reject AI text that calls the result healthy when deterministic threshold result failed.
- Reject AI text that ignores failed P95, P99, error rate, or throughput signals.
- Return validation errors as structured messages.

Validation is strict. If the AI response fails validation, the report becomes:

- `generated=false`
- `failureReason=VALIDATION_FAILED`
- `validationErrors` populated

The deterministic insight report and run status are not changed by AI validation failure.

## AI Regeneration

Add an endpoint:

```text
POST /performance/{performanceResultId}/ai-report/regenerate
```

Flow:

1. Load the performance result.
2. Build the current management report dynamically.
3. Reuse persisted `insightReport` when present.
4. Recompute deterministic insight only when the result has no persisted insight report.
5. Generate a new AI management report.
6. Validate the AI output.
7. Persist the new `aiManagementReport`, overwriting the previous value.
8. Return the updated `PerformanceAiManagementReport`.

If AI generation or validation fails, persist a `generated=false` report with metadata and validation/failure reason. The performance run status must not change.

## Prompt Observability

The AI service records:

- model name
- duration in milliseconds
- attempt count
- failure reason
- validation errors
- generated-by version
- prompt hash
- input summary hash
- response size
- token usage when available

Raw prompts, raw response bodies, request bodies, response bodies, headers, tokens, API keys, dataset rows, and raw thread detail are not persisted.

Hashes should be deterministic for the generated system prompt and compact input summary so support teams can compare generation inputs without seeing sensitive data.

## Export

JSON export continues to return the full payload including `insightReport` and `aiManagementReport`.

CSV export is extended with new sections:

- `Decision Summary`
  - release readiness
  - anomaly score and level
  - Apdex
  - SLO compliance
  - regression score
  - bottleneck type
  - AI generated status
- `Metric Insights`
  - metric
  - severity
  - actual
  - expected
  - explanation
- `Root Cause Hints`
  - severity
  - category
  - signal
  - explanation
  - recommendation
- `AI Action Plan`
  - priority
  - title
  - description
  - related step
  - related metric
- `AI Observability`
  - model
  - duration
  - attempt count
  - failure reason
  - validation errors
  - prompt hash
  - input summary hash
  - response size
  - token counts when present

CSV export must remain null-safe for legacy results.

## Frontend Print/PDF

No backend PDF endpoint is added.

The `Rapor` tab gets a print/PDF action. The action uses `window.print()` and print-friendly CSS so the user can save the report as PDF from the browser.

Printable content includes:

- decision summary
- risk matrix
- executive summary
- technical findings
- step risks
- root-cause hints
- AI action plan
- AI observability metadata

The print layout should hide unrelated dashboard chrome and preserve compact, readable report sections.

## Risk Matrix UI

Add `PerformanceRiskMatrixPanel` below the decision summary in the `Rapor` tab.

Inputs:

- `managementReport.stepAssessments`
- `insightReport.metricInsights`
- `insightReport.rootCauseHints`
- `baselineComparison.metrics`

The first version derives risk rows on the frontend from existing deterministic data. No new backend risk matrix DTO is required.

Rows can represent:

- `Step`
- `Regression`
- `Root Cause`
- `Metric`

The panel shows the top five highest-risk rows and groups them into:

- Critical / High
- Watch
- Good / Info

Each row displays:

- source type
- step, metric, or signal name
- risk level
- short explanation

The component must render safely when any input is missing.

## Regenerate UI

The `Rapor` tab shows an AI regenerate button near the AI narrative or decision header.

Behavior:

- Button remains available when AI generation previously failed.
- Click calls the regenerate endpoint.
- Loading state blocks duplicate clicks.
- On success, parent report state is updated with the returned `aiManagementReport`.
- On failure, the UI shows a clear error while keeping the current deterministic report visible.

## API Surface

Expected frontend service addition:

```ts
regenerateAiReport(performanceResultId: number): Promise<PerformanceAiManagementReport>
```

Expected backend controller addition:

```java
@PostMapping("/{performanceResultId}/ai-report/regenerate")
PerformanceAiManagementReport regenerateAiReport(@PathVariable Long performanceResultId)
```

Exact controller placement should follow the existing performance controller/service structure.

## Error Handling

Backend:

- Missing performance result returns not found behavior consistent with existing performance endpoints.
- Missing insight report triggers deterministic insight recomputation if enough result data exists.
- AI client exception persists a fallback report with metadata.
- AI validation failure persists a fallback report with validation errors.
- Run status is never changed because of AI generation.

Frontend:

- Missing AI metadata renders `-`.
- Missing AI report renders fallback text.
- Regenerate failure keeps current report visible.
- Print/PDF action works even when AI report is missing.

## Testing

Backend tests:

- Validator rejects healthy/release-ready AI text when deterministic result is blocked or failed.
- Validator rejects action items not tied to deterministic problem step or metric.
- Validator writes validation errors into fallback report metadata.
- Regenerate flow overwrites `aiManagementReport`.
- Regenerate failure persists `generated=false` and does not change run status.
- CSV export includes decision summary, metric insights, root-cause hints, AI action plan, and AI observability sections.
- Legacy results with null metadata still export.

Frontend verification:

- `npm run lint`
- Risk matrix renders with full, partial, and missing data.
- Regenerate button updates AI report state.
- Regenerate failure shows error without hiding deterministic sections.
- Print/PDF button calls `window.print()`.
- Report remains responsive on narrow screens.

## Out Of Scope

- Maven wrapper setup.
- AI generation history.
- Backend PDF generation.
- New AI prompt template editor.
- New database tables for AI audit.
- Storing raw prompts or raw AI responses.
