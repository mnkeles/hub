# Performance Frontend Restoration Design

## Goal

Restore the deleted performance frontend delivered through commit `eb416dd` on top of the current `31d46bd` frontend without changing the backend or discarding unrelated frontend work.

## Source of Truth

- Use commit `eb416dd` as the last known complete frontend implementation.
- Preserve the behavior defined by the existing performance AI report, dataset/schedule/SLO, and shared-screen design documents.
- Keep the current backend exactly as it is.

## Scope

Restore the deleted performance components, `lib/errorUtils.ts`, and the dataset and schedule clients named in the recovery request.

Also restore the frontend dependencies required for those files to compile and work:

- performance contracts in `types/performance.ts`
- performance API methods in `services/performanceService.ts`
- performance translations in `messages/tr.json` and `messages/en.json`
- the shared `PerformanceTestsContent` integration in both performance routes
- the SLO/report integration in the existing analysis, chart, history, and summary components

Preserve unrelated additions and changes from `31d46bd`, including authorization, authentication, dashboard, deployment, and general UI work. Preserve the user's existing `.idea/vcs.xml` working-tree change.

## Merge Strategy

Do not revert `31d46bd` wholesale. Restore files that were deleted verbatim from `eb416dd`, then selectively merge the performance-specific portions of files modified by both commits.

For shared files, retain current unrelated behavior while adding back only the contracts, API calls, translations, route wrappers, and component wiring required by the restored performance feature. Resolve incompatibilities against the current frontend instead of replacing an entire shared file when that would remove newer work.

## Runtime Behavior

- Both performance routes render the shared `PerformanceTestsContent` experience.
- Dataset selection, preview, mapping, upload, and manual rows use the existing backend dataset endpoints.
- Schedule creation, toggling, run-now, and deactivation use the existing backend schedule endpoints.
- The report detail tab displays the saved AI report and SLO score and handles missing report data gracefully.
- Existing analysis, charts, history, export, threshold, and live-monitor behavior remains available.
- Project-specific selections reset when project context changes, and schedule/analysis failures remain visible to the user.

## Error Handling

- API error messages use the restored shared error helper where applicable.
- Restored panels catch their own action failures and display an alert instead of producing unhandled promise rejections.
- Missing historical AI/SLO data renders the established empty state.
- No frontend recovery failure may trigger a backend change as part of this task.

## Verification

- Confirm the requested deleted files exist again.
- Parse both locale JSON files.
- Run targeted ESLint on the restored performance files and their shared dependencies.
- Run the frontend TypeScript/build command available in `apihub-fe`.
- Verify with Git that no file under `Apihub/` changed and `.idea/vcs.xml` remains untouched.

## Acceptance Criteria

- The complete performance frontend from `eb416dd` is usable on current main.
- The restored components compile with their required types and services.
- Both frontend performance routes use the shared screen.
- Dataset, schedule, AI report, and SLO UI integrations are present.
- Current unrelated frontend work is preserved.
- Backend files are unchanged.
