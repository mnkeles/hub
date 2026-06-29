# Performance Step Assessment Report Design

## Goal

Improve the existing performance `Rapor` tab so analysts and project managers can understand which test steps are problematic, which steps should be improved, which steps should be watched, and which steps are healthy.

The report must turn raw performance metrics into readable, action-oriented step analysis. The first view should answer:

- Which steps need improvement?
- Which steps should be watched?
- Which steps are in good condition?
- Why is each step classified that way?
- What action should be taken for each problematic step?

## Scope

This design covers the existing performance management report payload and the frontend `Rapor` tab.

Included:

- Backend step-level assessment generation.
- Extended report DTOs for grouped step analysis.
- Frontend rendering of step groups and per-step detail sections.
- Turkish user-facing report language.
- Backend unit tests and frontend lint/build verification.

Excluded:

- Distributed load generator support.
- CI/CD integrations.
- New external monitoring, tracing, or log correlation integrations.
- PDF/HTML export changes.
- New persistence tables or migrations.

## Information Architecture

The `Rapor` tab will be reorganized around step-level interpretation.

Top-level sections:

1. Short result summary
   - Overall test status.
   - Risk level.
   - Step assessment summary, for example: `12 adımdan 3'ü iyileştirilmeli, 2'si izlenmeli, 7'si iyi durumda.`
   - Most critical step when available.

2. Step groups
   - `İyileştirilmeli`
   - `İzlenmeli`
   - `İyi durumda`

3. Step cards
   Each step card shows:
   - Step name.
   - Status.
   - Priority.
   - Main reason.
   - Evidence metric.
   - Impact explanation.
   - Recommended action.

4. Per-step detail
   Each step card has its own detail toggle. Details show:
   - Average response time.
   - P90, P95, P99.
   - Error rate.
   - Total, successful, and failed request counts.
   - Throughput.
   - Last error, only when available.

5. Existing supporting sections
   - SLA summary remains, but moves below the step groups.
   - Trend summary remains.
   - Recommended actions remain.
   - The existing general detail explanation remains at the bottom.

## Step Classification

Each step is classified by the backend into one of three statuses.

### Needs Improvement

Status: `NEEDS_IMPROVEMENT`

A step needs improvement if at least one of these is true:

- Error rate exceeds the configured error-rate threshold.
- Average response time exceeds the configured average threshold.
- P95 exceeds the configured P95 threshold.
- P99 exceeds the configured P99 threshold.
- The step is identified by existing analysis as `problemStepName`, `slowestStepName`, or `highestErrorStepName`.

### Watch

Status: `WATCH`

A step should be watched if it does not exceed thresholds but is near risk:

- Error rate is at least 70% of the configured threshold.
- Average, P95, or P99 is at least 80% of its configured threshold.
- Failed requests exist but error rate remains under threshold.
- Response times are noticeably unstable based on standard deviation.

### Good

Status: `GOOD`

A step is good if it has meaningful traffic, does not exceed thresholds, is not near risk, and has no significant failure signal.

## Main Reason Priority

Each step gets one primary reason. If multiple reasons apply, the backend chooses the first matching reason in this order:

1. Error rate.
2. P99.
3. P95.
4. Average response time.
5. Response-time instability / standard deviation.
6. Low throughput.
7. Good condition.

This keeps each card readable and avoids overwhelming the user with competing explanations.

## Backend Data Model

Extend `PerformanceManagementReport` with step-level assessment fields.

New fields:

- `stepAssessmentSummary: String`
- `stepAssessments: List<PerformanceManagementStepAssessment>`

New DTO: `PerformanceManagementStepAssessment`

Fields:

- `stepName: String`
- `status: PerformanceManagementStepStatus`
- `priority: PerformanceManagementRiskLevel`
- `mainReason: String`
- `evidence: String`
- `impact: String`
- `recommendation: String`
- `sampleCount: long`
- `successCount: long`
- `failureCount: long`
- `errorRate: double`
- `averageMs: double`
- `p90Ms: double`
- `p95Ms: double`
- `p99Ms: double`
- `throughputPerSecond: double`
- `lastError: String`

New enum: `PerformanceManagementStepStatus`

Values:

- `NEEDS_IMPROVEMENT`
- `WATCH`
- `GOOD`

Existing `problemAreas` remains for backward compatibility, but the frontend step analysis is based primarily on `stepAssessments`.

## Backend Generation

`PerformanceManagementReportBuilder` will generate `stepAssessments` from `stepSummaries`.

Inputs:

- `PerformanceRunSummary`
- `PerformanceThresholdResult`
- `PerformanceAnalysisSummary`
- `PerformanceErrorAnalysis`
- `PerformanceEnvironmentMetrics`
- `PerformanceComparisonResult`
- `List<PerformanceSummary> stepSummaries`

Rules:

- Use `PerformanceThresholdConfig.defaults()` when no threshold config is available.
- Use the same threshold config for SLA and step assessment to avoid contradictory conclusions.
- Generate one assessment per step summary.
- Sort grouped display by severity first, then by strongest evidence:
  - `NEEDS_IMPROVEMENT`
  - `WATCH`
  - `GOOD`
  Within problematic groups, higher error rate and higher P95/P99 should appear earlier.

## Report Language

User-facing report text must use clear Turkish.

Required labels:

- `Başarılı`
- `Başarısız`
- `Çalışıyor`
- `İyileştirilmeli`
- `İzlenmeli`
- `İyi durumda`
- `Düşük Risk`
- `Orta Risk`
- `Yüksek Risk`
- `Kritik Risk`

Technical terms such as P95, P99, throughput, and threshold may remain, but each should be paired with a plain-language explanation when used in narrative text.

Examples:

- `P95 hedefin üzerinde olduğu için kullanıcıların bir kısmı yavaşlık yaşayabilir.`
- `Hata oranı beklenen sınırı aştığı için bu adım öncelikli incelenmelidir.`
- `Bu adım hedeflerin içinde çalışıyor; ek aksiyon gerekmiyor.`

## Frontend Rendering

Update `PerformanceManagementReportPanel`.

Display behavior:

- The top summary shows status, risk, step assessment summary, and the most critical step.
- Step assessments are grouped into three sections:
  - `İyileştirilmeli`
  - `İzlenmeli`
  - `İyi durumda`
- Empty groups may be hidden. If all groups are empty, show `Bu test için adım bazlı analiz üretilemedi.`
- Each step card shows short, action-oriented content first.
- Per-step detail is collapsed by default.

Card short view:

- Step name.
- Status chip.
- Priority chip.
- Main reason.
- Evidence.
- Recommendation.

Detail view:

- Average, P90, P95, P99.
- Error rate.
- Total request count.
- Successful request count.
- Failed request count.
- Throughput.
- Last error when present.

Risk and status enum values must be translated in the frontend rather than shown as raw enum values.

## Empty and Edge States

- If `stepAssessments` is missing or empty, show `Bu test için adım bazlı analiz üretilemedi.`
- If `lastError` is empty, do not render the last-error row.
- If a metric is unavailable, render `-`.
- If a step has zero samples, classify it as `WATCH` with an explanation that no meaningful traffic was observed.
- If every step is `GOOD`, the summary should explicitly state that no step currently requires improvement.

## Testing

Backend unit tests:

- A step exceeding error threshold is classified as `NEEDS_IMPROVEMENT`.
- A step exceeding P95/P99 threshold is classified as `NEEDS_IMPROVEMENT`.
- A near-threshold step is classified as `WATCH`.
- A healthy step is classified as `GOOD`.
- Step assessment summary counts are correct.
- The main reason priority chooses error rate before latency when both apply.

Frontend verification:

- `npm run lint`
- `npm run build`
- Confirm that the report panel renders the three status groups.
- Confirm that per-step detail toggles reveal metric details.

Backend verification:

- Run targeted Maven tests for the performance report builder and export service when Maven is available on PATH.

## Compatibility

This change extends the existing export payload. Existing consumers can continue using old fields because `problemAreas`, `slaSummary`, `trendSummary`, `recommendedActions`, and `detailExplanation` remain.

Frontend must tolerate older backend responses where `stepAssessmentSummary` or `stepAssessments` are absent.
