# Performance AI Insight Report Design

## Goal

Improve the existing performance `Rapor` tab from a rule-based summary into a hybrid analysis product.

The system must keep all pass/fail, risk, scoring, readiness, and bottleneck decisions deterministic in the backend. AI is used only to turn those decisions and metrics into richer Turkish narratives and prioritized recommendations.

The first screen of the report should answer:

- Is this run suitable for release?
- Which metrics failed or regressed?
- Is the result abnormal compared with expectations or baseline?
- Which step is the main bottleneck?
- What is the likely root-cause area?
- What should the team do next?

## Scope

Included:

- Deterministic insight scoring.
- Automatic AI report generation after test completion.
- Persisted insight and AI report snapshots.
- Extended `/performance/analysis` and JSON export payloads.
- A more detailed `Rapor` tab UI.
- Graceful fallback when AI is unavailable or returns invalid content.
- Backend unit tests for deterministic scores and AI fallback handling.
- Frontend empty-state and legacy-response tolerance.

Excluded:

- Scenario execution changes.
- Multi-flow scenario modeling.
- Dataset management.
- Correlation/retry execution.
- Distributed load generation.
- CI/CD gates.
- PDF/HTML report export.
- User-editable AI prompt templates.

## Current Context

The project already has:

- `PerformanceManagementReportBuilder` for rule-based report generation.
- `PerformanceManagementReportPanel` for the current `Rapor` tab.
- `PerformanceExportPayload` returned by `/performance/analysis`.
- JSONB persistence for run summary, threshold result, analysis summary, error analysis, environment metrics, baseline comparison, and validation checklist.
- Spring AI / OpenAI configuration through `OPENAI_API_KEY`.
- `OpenAiChatService` for existing chat behavior.

This design extends the performance reporting path without replacing the existing chat feature.

## Backend Architecture

### Deterministic Insight Builder

Add `PerformanceInsightBuilder`.

Responsibilities:

- Read run summary, threshold result, step summaries, error analysis, environment metrics, and baseline comparison.
- Compute deterministic score and classification fields.
- Produce a stable, testable `PerformanceInsightReport`.
- Avoid AI calls, persistence, HTTP calls, and unrelated repository access.

Main DTO: `PerformanceInsightReport`.

Fields:

- `anomalyScore: double`
- `anomalyLevel: PerformanceAnomalyLevel`
- `regressionScore: Double`
- `regressionAvailable: boolean`
- `apdexScore: Double`
- `apdexEstimated: boolean`
- `sloCompliancePercent: double`
- `bottleneckType: PerformanceBottleneckType`
- `releaseReadiness: PerformanceReleaseReadiness`
- `rootCauseHints: List<PerformanceRootCauseHint>`
- `metricInsights: List<PerformanceMetricInsight>`
- `stepInsights: List<PerformanceStepInsight>`

Suggested enums:

- `PerformanceAnomalyLevel`: `NORMAL`, `WATCH`, `ANOMALOUS`, `CRITICAL`
- `PerformanceBottleneckType`: `NONE`, `ERROR`, `LATENCY`, `THROUGHPUT`, `INSTABILITY`, `ENVIRONMENT`, `MIXED`
- `PerformanceReleaseReadiness`: `READY`, `CONDITIONAL`, `BLOCKED`, `UNKNOWN`
- `PerformanceInsightSeverity`: `INFO`, `WARNING`, `HIGH`, `CRITICAL`

### AI Report Service

Add `PerformanceAiReportService`.

Responsibilities:

- Build a compact prompt from structured performance data.
- Call Spring AI / OpenAI using backend configuration.
- Request strict JSON output.
- Validate and sanitize the AI response.
- Return a `PerformanceAiManagementReport`.
- Never change deterministic decisions.

Main DTO: `PerformanceAiManagementReport`.

Fields:

- `generated: boolean`
- `generatedAt: Date`
- `model: String`
- `executiveNarrative: String`
- `technicalNarrative: String`
- `rootCauseNarrative: String`
- `recommendedActionPlan: List<PerformanceAiActionItem>`
- `releaseReadinessNarrative: String`
- `limitations: List<String>`
- `errorMessage: String`

Action DTO: `PerformanceAiActionItem`.

Fields:

- `priority: String` with values `P0`, `P1`, `P2`
- `title: String`
- `description: String`
- `relatedStepName: String`
- `relatedMetric: String`

### Report Assembly

The `/performance/analysis` payload should expose:

- Existing `managementReport`
- New `insightReport`
- New `aiManagementReport`

The current rule-based report remains available for compatibility and fallback.

## Persistence

Add two nullable JSONB columns to `perf_rslt`:

- `insight_report`
- `ai_management_report`

Entity fields:

- `PerformanceInsightReport insightReport`
- `PerformanceAiManagementReport aiManagementReport`

Persist both after final run metrics are available.

Generation order after a run finishes:

1. Run summary.
2. Threshold result.
3. Analysis summary.
4. Error analysis.
5. Environment metrics.
6. Baseline comparison.
7. Validation checklist.
8. Insight report.
9. AI management report.

If AI generation fails, persist:

- `generated=false`
- `errorMessage` with a concise failure reason
- any deterministic insight report already produced

The final performance status must not become `ERROR` because AI generation failed.

## AI Prompt Rules

Only summarized, non-sensitive data is sent to AI:

- Run summary.
- Threshold result.
- Insight report.
- Step insight summaries.
- Error analysis summary.
- Environment metrics summary.
- Baseline comparison summary.
- Existing rule-based management report.

Do not send:

- Raw thread detail.
- Request bodies.
- Response bodies.
- Headers.
- Tokens.
- API keys.
- Full error payloads.
- Full dataset rows.

The system prompt must require:

- Turkish output.
- Strict JSON.
- No invented metric values.
- No pass/fail override.
- No release readiness override.
- Cautious root-cause language.
- Separate executive and technical narratives.
- Explicit limitations when evidence is missing.

AI output must be rejected or partially ignored when:

- JSON cannot be parsed.
- Required fields are missing.
- Output is empty or too short.
- Output contradicts deterministic status or release readiness.
- Output contains unsupported claims not present in input data.

## Scoring Rules

### Apdex Score

First version:

- `T = thresholdConfig.maxAverageMs`
- if missing, `T = 1000 ms`
- satisfied: elapsed time `<= T`
- tolerating: elapsed time `> T` and `<= 4T`
- frustrated: elapsed time `> 4T`
- formula: `(satisfied + tolerating / 2) / totalSamples`

Use raw thread detail when available. If raw samples are unavailable, estimate from step buckets and set `apdexEstimated=true`.

### SLO Compliance

Based on five threshold criteria:

- Error rate.
- Average response time.
- P95.
- P99.
- Throughput.

Formula:

`passedThresholdCount / totalThresholdCount * 100`

### Regression Score

Only available when baseline comparison exists.

Start from 100:

- P99 regressed: `-25`
- P95 regressed: `-20`
- average response time regressed: `-15`
- error rate regressed: `-25`
- throughput regressed: `-15`

Informational metrics such as total sample count do not lower the score.

Minimum score is 0. If no baseline exists, set `regressionAvailable=false`.

### Anomaly Score

First version is rule-based.

Signals:

- P99 greater than 2x threshold: `+30`
- P95 threshold exceeded: `+20`
- Error rate threshold exceeded: `+25`
- Throughput below threshold: `+15`
- Standard deviation greater than 50% of average: `+10`
- Critical baseline regression exists: `+20`

Maximum score is 100.

Levels:

- `0-24`: `NORMAL`
- `25-49`: `WATCH`
- `50-74`: `ANOMALOUS`
- `75-100`: `CRITICAL`

### Bottleneck Type

Classification:

- High error rate: `ERROR`
- High P95/P99: `LATENCY`
- Low throughput: `THROUGHPUT`
- High standard deviation: `INSTABILITY`
- Environment warning plus latency signal: `ENVIRONMENT`
- Multiple strong signals: `MIXED`
- No strong signal: `NONE`

### Release Readiness

Values:

- `READY`: thresholds passed, anomaly is low, no regression.
- `CONDITIONAL`: warnings exist but no critical threshold failure.
- `BLOCKED`: threshold failed, critical regression exists, or risk is high/critical.
- `UNKNOWN`: run is running, stopped, error, or key data is unavailable.

AI may explain readiness, but must not change this value.

## Rapor Tab UI

Refactor the current report panel into focused sections.

### Decision Header

Component: `PerformanceReportDecisionHeader`.

Shows:

- Overall status.
- Risk level.
- Release readiness.
- Apdex.
- SLO compliance.
- Regression score.
- Anomaly score.
- AI generated / fallback chip.

### Executive Summary

Component: `PerformanceExecutiveSummaryPanel`.

Behavior:

- Use AI `executiveNarrative` when generated.
- Otherwise use rule-based `executiveSummary`.
- Show fallback message when AI failed.

### Technical Findings

Component: `PerformanceTechnicalFindingsPanel`.

Shows:

- Bottleneck type.
- Problem metrics.
- Most critical step.
- Threshold violations.
- Baseline regressions.

### Step Risks

Component: `PerformanceStepRiskPanel`.

Keeps current step assessment behavior but improves layout:

- `Iyilestirilmeli` and `Izlenmeli` groups visible by default.
- `Iyi durumda` collapsible.
- Each step card shows status, priority, reason, evidence, impact, recommendation, average, P95, P99, error rate, and throughput.

### Regression And Trend

Component: `PerformanceRegressionTrendPanel`.

Shows:

- Improved metrics.
- Regressed metrics.
- Neutral/informational changes.
- Most critical regression.
- Baseline missing state.

### Root Cause Hints

Component: `PerformanceRootCauseHintsPanel`.

Shows:

- Environment-based hints when available.
- DB pool, CPU, memory, GC, slow SQL, HTTP 5xx, and restart signals.
- Clear missing-observability message when environment metrics are unavailable.

### AI Narrative

Component: `PerformanceAiNarrativePanel`.

Shows:

- AI status.
- Technical narrative.
- Root-cause narrative.
- Release readiness narrative.
- Limitations.
- AI failure message when generation failed.

### Action Plan

Component: `PerformanceActionPlanPanel`.

Shows:

- AI action plan when generated.
- Rule-based recommended actions otherwise.
- Priorities `P0`, `P1`, `P2`.

## API And Export

Extend `PerformanceExportPayload` with:

- `PerformanceInsightReport insightReport`
- `PerformanceAiManagementReport aiManagementReport`

JSON export includes both fields.

CSV export can remain unchanged in the first version, except it must not fail when the new fields are present.

## Error Handling

The UI must handle:

- Missing `insightReport`.
- Missing `aiManagementReport`.
- `aiManagementReport.generated=false`.
- Legacy performance result with only old report fields.
- Running/stopped/error tests.
- Missing baseline.
- Missing environment metrics.

Backend must handle:

- Null run summary.
- Null threshold config.
- Empty step summaries.
- Missing baseline comparison.
- AI timeout/error.
- Invalid AI JSON.

## Testing

Backend unit tests:

- Apdex score from raw samples.
- Estimated Apdex from buckets.
- SLO compliance count.
- Regression score with critical regression.
- Regression unavailable when no baseline exists.
- Anomaly level boundaries.
- Bottleneck type selection.
- Release readiness rules.
- AI invalid JSON fallback.
- AI contradiction fallback.
- Payload includes insight and AI report fields.

Frontend verification:

- Report renders with full insight and AI data.
- Report renders when AI failed.
- Report renders legacy response without new fields.
- Step risk groups remain accessible.
- Decision header does not overflow on mobile.

Acceptance criteria:

- Test completion automatically persists `insight_report`.
- Test completion attempts to persist `ai_management_report`.
- AI failure does not fail the test run.
- `/performance/analysis` returns rule-based report, insight report, and AI report.
- Rapor tab shows decision header, AI narrative, root-cause hints, trend, and prioritized action plan.
- Deterministic decisions remain unchanged even when AI output is different.
- No sensitive request/header/token data is sent to AI.
