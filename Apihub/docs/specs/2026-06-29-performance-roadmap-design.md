# Performance Test Roadmap Design

## Purpose

This document defines the product and technical roadmap for extending the existing Apihub performance test module. The roadmap keeps the current Spring Boot backend and Next/MUI frontend architecture, and evolves the existing performance detail experience into a broader performance analysis product.

The most visible user-facing addition is a new **Rapor** tab inside the existing performance detail modal. This tab turns technical performance results into a project-manager-friendly report. The report starts with a rule-based executive summary and grows as deeper metrics, profiles, trend analysis, and root-cause integrations are added.

## Explicitly Out Of Scope

- Distributed load generation.
- Multiple load generator nodes.
- Load generator health and saturation management.
- CI/CD integration.
- Build or pull request performance gates.
- AI-generated report content in the first version.

## Roadmap Phases

### Phase 1: Rapor Tab And Executive Summary

Add a **Rapor** tab to the existing performance detail modal.

The tab will show a project-manager-friendly summary instead of raw technical tables. It will use existing performance result data:

- Run summary.
- Threshold result.
- Analysis summary.
- Error analysis.
- Environment metrics, when available.
- Baseline comparison, when available.
- Step summaries.

The first version will be rule-based. It will not require AI.

The report will include:

- Overall status: passed, failed, stopped, or error.
- Risk level: `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- Executive summary.
- SLA / threshold summary.
- Problem areas.
- Trend summary.
- Recommended actions.
- Expandable detail explanation.

### Phase 2: Load Profiles

Extend the performance test run configuration to support richer load shapes:

- Ramp-up, steady-state, and ramp-down.
- Spike test.
- Stress test.
- Soak / endurance test.
- Step load test.
- Target RPS / TPS based test.
- Scheduled test execution.

Phase 2 will add these request/configuration fields:

- `loadProfileType`: `STANDARD`, `SPIKE`, `STRESS`, `SOAK`, `STEP`, `TARGET_RPS`.
- `steadyStateDurationSeconds`.
- `rampDownSeconds`.
- `targetRatePerSecond`.
- `scheduleAt`.
- Safety limits for high-risk runs.

The existing in-process execution engine remains the execution boundary. No distributed workers are introduced.

### Phase 3: Advanced Scenario Modeling

Add scenario modeling capabilities for more realistic performance tests:

- Multiple flows in the same test run.
- Weighted scenarios across flows.
- Conditional steps.
- Retry policy.
- Think time distribution: fixed, random, gaussian.
- Correlation support: extracting response values and using them in later requests.
- Dataset use and data parameterization.

This phase is large enough to deserve its own detailed implementation spec before development.

### Phase 4: Deeper Metrics And Real-Time Observation

Extend request-level and run-level metrics:

- Apdex score.
- SLA / SLO compliance.
- P50, P75, P90, P95, P99, and P99.9.
- Request queue time, when measurable.
- Connection time, when measurable.
- DNS / connect / TLS / TTFB timing, when measurable.
- Payload size and bandwidth.
- Retry count.
- Timeout count.
- Per-endpoint RPS.

Improve live monitoring:

- Second-level time-series buckets.
- Live charts during a test run.
- Live threshold warnings.
- Automatic stop rules.
- Error spike detection.

The live monitor must be backed by real intermediate metrics, not only final persisted results.

### Phase 5: Root Cause Analysis

Add optional root-cause data sources and correlate them with test results:

- CPU and memory usage.
- JVM heap and non-heap usage.
- GC count and GC time.
- DB connection pool usage.
- Slow SQL count.
- Application log error counts.
- Trace/span data, when available.
- Downstream service latency.

Supported integration sources may include:

- Spring Boot Actuator.
- Prometheus.
- Grafana-backed metrics APIs.
- APM tools.
- Application logs.
- Database monitoring.

These integrations must be optional. If a source is unavailable, the UI must show a clear unavailable message and continue rendering the rest of the report.

### Phase 6: Reporting Extensions

Extend the reporting experience beyond the in-app **Rapor** tab:

- PDF report.
- HTML report.
- Trend report.
- Release-based comparison.
- Baseline regression report.
- SLA violation report.
- Executive summary report.
- Technical detail report.
- Shareable report view or link, if access control supports it.

At this phase, report snapshot persistence can be introduced so downloaded and shared reports remain stable even if later calculation rules change.

### Phase 7: Test Management, Security, And Data Governance

Add governance around performance tests:

- Test plan versioning.
- Test run tags.
- Environment-based configuration.
- User/role-based authorization.
- Test run approval flow for high-risk runs.
- Quota and limit management for large tests.
- Secret masking.
- Token refresh management.
- Test data masking.
- Sensitive header/body hiding.
- Credential vault integration, if available.

## Rapor Tab Design

The **Rapor** tab lives inside the existing performance detail modal. It does not replace the technical tabs. Existing tabs such as analysis, validation, step summary, thread detail, charts, error analysis, and export remain available.

The tab targets project managers, team leads, and product owners. It must explain the result in business-facing language while staying grounded in actual metrics.

### Report Sections

#### Executive Summary

Shows:

- Overall result.
- Risk level.
- Short readable summary.

Example style:

> Test failed from a performance perspective. The main risk is high response time in the customer creation step. Error rate is acceptable, but P95 and P99 exceed the configured limits.

#### SLA / Threshold Summary

Shows whether the main limits passed or failed:

- Error rate.
- Average response time.
- P95.
- P99.
- Throughput.

Failure reasons are rewritten into concise non-technical language.

#### Problem Areas

Highlights:

- Slowest step.
- Highest P95 step.
- Highest P99 step.
- Highest error step.
- Likely user impact.

#### Trend Summary

Uses baseline comparison when available. If no baseline exists, it shows:

> Baseline comparison is not available. Select a baseline run to compare performance trend.

#### Recommended Actions

Generated by deterministic rules in the first version. Examples:

- Review the step with the highest P95/P99.
- Check timeout and downstream dependency behavior.
- Review DB connection pool and slow SQL metrics when environment metrics indicate pressure.
- Re-run with a lower ramp-up or smaller thread count if the failure appears load-shape related.

#### Detail Explanation

An expandable area that explains the result in more depth. It may include metric values, but it should not become another raw metrics table.

## Backend Design

### New Report Model

Add a dynamic report DTO:

`PerformanceManagementReport`

Fields:

- `overallStatus`
- `riskLevel`
- `executiveSummary`
- `slaSummary`
- `problemAreas`
- `trendSummary`
- `recommendedActions`
- `detailExplanation`

Supporting DTOs can be introduced for structured sections, but they should stay small and focused.

### Report Builder

Add:

`PerformanceManagementReportBuilder`

Responsibilities:

- Read existing run, threshold, analysis, error, environment, comparison, and step summary data.
- Compute the report risk level.
- Produce executive summary text.
- Produce SLA summary.
- Identify problem areas.
- Produce trend summary.
- Produce rule-based recommended actions.

This service should not execute tests, mutate test results, or query unrelated entities. It is a pure report-building component over existing result data.

### Persistence Strategy

Phase 1 uses dynamic report generation. The report is derived from already persisted result data and does not require a database schema change.

Report snapshot persistence is deferred until PDF/HTML/shareable reports are introduced.

### API Shape

Preferred first integration:

- Extend `/performance/analysis` response with the management report field.

Reason:

- The detail modal already fetches analysis data.
- The report is conceptually part of analysis.
- It avoids an additional round trip for the first version.

If the payload becomes too large in later phases, a dedicated endpoint can be added:

- `GET /performance/report?performanceResultId=...`

## Frontend Design

### Detail Modal

Add a new tab named **Rapor** to the existing detail modal.

The tab order should place **Rapor** near the front, before highly technical tabs. Existing technical tabs remain unchanged.

### New Component

Add a focused component:

`PerformanceManagementReportPanel`

Responsibilities:

- Render executive summary.
- Render risk status.
- Render SLA/threshold summary.
- Render problem areas.
- Render trend summary.
- Render recommended actions.
- Render expandable detail explanation.
- Handle missing report data gracefully.

The component should not compute business logic that belongs in the backend. It may do presentation-only formatting.

## Error Handling And Empty States

The UI must handle:

- No report data.
- Missing baseline comparison.
- Missing environment metrics.
- Stopped test.
- Error test.
- Legacy performance result without new report fields.

All empty states should be explicit and readable.

## Risks And Constraints

- The existing execution code can become too large if load profiles, live metrics, and scenario modeling are added directly into one service. Future phases should separate execution, scheduling, metrics collection, and report building.
- Persisting raw samples in JSONB can become expensive for large tests. Time-series buckets, histograms, or compact summary models may be needed in later phases.
- Project-manager-facing report text can become too technical. The report should explain impact and recommended action first, then supporting metrics.
- Some deeper network timings may not be available through the current WebClient abstraction without additional instrumentation.
- Root-cause integrations depend on external observability systems and must remain optional.

## Test Strategy

### Backend Unit Tests

Add tests for:

- Risk level calculation.
- Executive summary generation.
- SLA summary generation.
- Problem area selection.
- Recommended action rules.
- Missing baseline handling.
- Missing environment metrics handling.
- Stopped and error test handling.

### Backend Service Tests

Add tests that verify:

- `/performance/analysis` includes the report field.
- Legacy results without optional fields still produce a safe report.
- Failed threshold results produce meaningful recommendations.

### Frontend Tests

Add component tests for:

- Report with complete data.
- Report without baseline.
- Report without environment metrics.
- Failed performance result.
- Passed performance result.
- Stopped/error performance result.

### Regression Checks

Existing tabs must continue to work:

- Analysis.
- Validation.
- Step summary.
- Thread detail.
- Charts.
- Error analysis.
- Export.

## Acceptance Criteria For Phase 1

- The performance detail modal contains a tab named **Rapor**.
- The **Rapor** tab shows a readable executive summary.
- The report shows overall status and risk level.
- The report explains threshold/SLA result in non-technical language.
- The report highlights the most important problem areas.
- The report shows baseline/trend status when available.
- The report shows a clear message when baseline is unavailable.
- The report shows rule-based recommended actions.
- The report handles missing environment metrics without breaking the screen.
- No database migration is required for Phase 1.
- Existing detail modal tabs continue to work.
