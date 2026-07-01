# Performance Step Assessment Report - Implementation Plan

<!-- EXECUTION CONTRACT - read before touching any task -->
> When the user asks for a specific task (e.g. "do TASK-03"):
> 1. Read **only** that task's block. Do not preview other tasks.
> 2. Stay strictly inside its **Targets** - do not edit files outside that list.
> 3. Follow the **Implementation Notes**; do not invent extra scope.
> 4. When **Done When** and **Verification** are satisfied, **stop and report**. Wait for approval before moving to the next task.
> 5. If verification fails, report the failure and stop. Do not attempt fixes outside the task's Targets.

**Goal:** Turn the existing performance `Rapor` tab into an action-oriented step assessment report that shows which steps need improvement, which steps should be watched, and which steps are healthy.

**Architecture:** The backend extends the existing `PerformanceManagementReport` payload with step-level assessments generated from `PerformanceSummary` records and existing threshold/analysis fields. The frontend keeps using the existing `managementReport` field and renders the new `stepAssessments` into grouped step cards with per-step detail toggles; no database migration or new API endpoint is required.

**Tech / dependencies:** Spring Boot 3.5, Java 21 records/services, JUnit 5, Next.js 16, React 19, MUI 7, TypeScript, next-intl. No new runtime dependency is required.

**File map:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementStepStatus.java` - Backend enum for step assessment status groups.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementStepAssessment.java` - Backend record for one assessed performance step.
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementReport.java` - Existing backend report DTO; extended with step assessment summary and list.
- `src/main/java/etiya/omniAutomation/service/PerformanceManagementReportBuilder.java` - Existing backend builder; extended with step classification, step evidence, recommendations, summary text, and sorted assessments.
- `src/test/java/etiya/omniAutomation/service/PerformanceManagementReportBuilderTest.java` - Backend unit tests for needs-improvement, watch, good, zero-sample, summary counts, and reason priority.
- `../../apihub-fe/apihub-fe/types/performance.ts` - Frontend TypeScript contract for step status and step assessments.
- `../../apihub-fe/apihub-fe/messages/tr.json` - Turkish labels for step groups, status chips, detail labels, and empty states.
- `../../apihub-fe/apihub-fe/messages/en.json` - English labels matching the report UI keys.
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` - Existing frontend report panel; reorganized around grouped step assessments while keeping SLA/trend/actions/detail sections.

---

### TASK-01: Backend Step Assessment Model And Classification

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementStepStatus.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementStepAssessment.java` (create)
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementReport.java` (modify)
- `src/main/java/etiya/omniAutomation/service/PerformanceManagementReportBuilder.java` (modify)
- `src/test/java/etiya/omniAutomation/service/PerformanceManagementReportBuilderTest.java` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Create this enum:
  ```java
  package etiya.omniAutomation.business.dto;

  public enum PerformanceManagementStepStatus {
      NEEDS_IMPROVEMENT,
      WATCH,
      GOOD
  }
  ```
- Create this record:
  ```java
  package etiya.omniAutomation.business.dto;

  public record PerformanceManagementStepAssessment(
          String stepName,
          PerformanceManagementStepStatus status,
          PerformanceManagementRiskLevel priority,
          String mainReason,
          String evidence,
          String impact,
          String recommendation,
          long sampleCount,
          long successCount,
          long failureCount,
          double errorRate,
          double averageMs,
          double p90Ms,
          double p95Ms,
          double p99Ms,
          double throughputPerSecond,
          String lastError
  ) {
  }
  ```
- Extend `PerformanceManagementReport` by adding these fields after `riskLevel` and before `executiveSummary`:
  ```java
  String stepAssessmentSummary,
  List<PerformanceManagementStepAssessment> stepAssessments,
  ```
  The full constructor order must become:
  ```java
  String overallStatus,
  PerformanceManagementRiskLevel riskLevel,
  String stepAssessmentSummary,
  List<PerformanceManagementStepAssessment> stepAssessments,
  String executiveSummary,
  List<PerformanceManagementSlaItem> slaSummary,
  List<PerformanceManagementProblemArea> problemAreas,
  String trendSummary,
  List<String> recommendedActions,
  String detailExplanation
  ```
- Update `PerformanceManagementReportBuilder.build` to compute:
  ```java
  List<PerformanceManagementStepAssessment> stepAssessments =
          stepAssessments(analysisSummary, stepSummaries, thresholdResult);
  String stepAssessmentSummary = stepAssessmentSummary(stepAssessments);
  ```
  and pass both fields into the `PerformanceManagementReport` constructor.
- Use `PerformanceThresholdConfig.defaults()` when `thresholdResult == null` or `thresholdResult.thresholds() == null`.
- Add a private method with this behavior:
  ```java
  private List<PerformanceManagementStepAssessment> stepAssessments(
          PerformanceAnalysisSummary analysisSummary,
          List<PerformanceSummary> stepSummaries,
          PerformanceThresholdResult thresholdResult
  )
  ```
  It returns `List.of()` when `stepSummaries` is null or empty. Otherwise it creates one assessment per non-null `PerformanceSummary`.
- A step is `NEEDS_IMPROVEMENT` when any of these are true:
  - `summary.errorRate() > thresholds.maxErrorRatePercent()`
  - `summary.averageElapsedTime() > thresholds.maxAverageMs()`
  - `summary.p95ElapsedTime() > thresholds.maxP95Ms()`
  - `summary.p99ElapsedTime() > thresholds.maxP99Ms()`
  - `summary.stepName()` equals `analysisSummary.problemStepName()`, `analysisSummary.slowestStepName()`, or `analysisSummary.highestErrorStepName()`
- A step is `WATCH` when it is not `NEEDS_IMPROVEMENT` and any of these are true:
  - `summary.sampleCount() <= 0`
  - `summary.errorRate() >= thresholds.maxErrorRatePercent() * 0.70`
  - `summary.averageElapsedTime() >= thresholds.maxAverageMs() * 0.80`
  - `summary.p95ElapsedTime() >= thresholds.maxP95Ms() * 0.80`
  - `summary.p99ElapsedTime() >= thresholds.maxP99Ms() * 0.80`
  - `summary.failureCount() > 0`
  - `summary.standardDeviation() >= summary.averageElapsedTime() * 0.50` and `summary.sampleCount() >= 2`
  - `summary.throughputPerSecond() < thresholds.minThroughputPerSecond()` and `summary.sampleCount() > 0`
- A step is `GOOD` when it is neither `NEEDS_IMPROVEMENT` nor `WATCH`.
- Choose `mainReason` with this priority order:
  1. Error rate: `Hata oranı hedefin üzerinde.`
  2. P99: `P99 hedefin üzerinde.`
  3. P95: `P95 hedefin üzerinde.`
  4. Average response time: `Ortalama yanıt süresi hedefin üzerinde.`
  5. Response-time instability: `Yanıt süreleri dalgalı.`
  6. Low throughput: `Throughput beklenen seviyenin altında.`
  7. Analysis-highlighted step: `Mevcut analiz bu adımı öncelikli alan olarak işaretledi.`
  8. Zero samples: `Bu adımda anlamlı trafik oluşmadı.`
  9. Good condition: `Bu adım hedeflerin içinde çalışıyor.`
- Generate `evidence` as a short measurable string:
  - Error reason: `Hata oranı %.2f%%, hedef <= %.2f%%`
  - P99 reason: `P99 %.0f ms, hedef <= %.0f ms`
  - P95 reason: `P95 %.0f ms, hedef <= %.0f ms`
  - Average reason: `Ortalama %.0f ms, hedef <= %.0f ms`
  - Instability reason: `Standart sapma %.0f ms, ortalama %.0f ms`
  - Throughput reason: `Throughput %.2f req/s, hedef >= %.2f req/s`
  - Zero samples: `Toplam istek 0`
  - Good condition: `Hata oranı %.2f%%, P95 %.0f ms`
  Use `Locale.US` for numeric formatting to match existing builder formatting.
- Generate `impact` and `recommendation` in clear Turkish:
  - Error reason impact: `Başarısız istekler kullanıcı akışını kesebilir.` Recommendation: `Hata alan istekleri ve son hata mesajını adım bazında inceleyin.`
  - P95/P99/average impact: `Kullanıcıların bir kısmı bu adımda yavaşlık yaşayabilir.` Recommendation: `Bu adımın servis, veritabanı ve downstream bağımlılık sürelerini inceleyin.`
  - Instability impact: `Adım bazı çalışmalarda hızlı, bazı çalışmalarda yavaş yanıt veriyor.` Recommendation: `Dalgalanmayı artıran kaynak kullanımı veya dış servis beklemelerini kontrol edin.`
  - Throughput impact: `Adım beklenen yükü karşılamakta zorlanabilir.` Recommendation: `Bu adım için kapasite, bağlantı havuzu ve throttling ayarlarını kontrol edin.`
  - Zero samples impact: `Bu adım için performans yorumu yapmak için yeterli veri yok.` Recommendation: `Test senaryosunda bu adımın gerçekten çalıştığını doğrulayın.`
  - Good condition impact: `Bu adım mevcut koşullarda hedefleri karşılıyor.` Recommendation: `Ek aksiyon gerekmiyor; sonraki koşumlarda izlemeye devam edin.`
- Map `priority` from status and severity:
  - `CRITICAL` when `errorRate >= thresholds.maxErrorRatePercent() * 2` or `p99ElapsedTime >= thresholds.maxP99Ms() * 2`
  - `HIGH` for other `NEEDS_IMPROVEMENT`
  - `MEDIUM` for `WATCH`
  - `LOW` for `GOOD`
- Sort returned assessments by:
  1. Status order: `NEEDS_IMPROVEMENT`, `WATCH`, `GOOD`
  2. Priority order: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
  3. Higher `errorRate`
  4. Higher `p99ElapsedTime`
  5. Higher `p95ElapsedTime`
  6. Step name ascending, nulls last
- Add `stepAssessmentSummary(List<PerformanceManagementStepAssessment> assessments)`:
  - Empty list: `Bu test için adım bazlı analiz üretilemedi.`
  - All good: `%d adımın tamamı iyi durumda. Şu anda iyileştirme gerektiren adım bulunmuyor.`
  - Mixed: `%d adımdan %d adım iyileştirilmeli, %d adım izlenmeli, %d adım iyi durumda.`
- Update existing builder tests for the new constructor fields and add these test methods:
  - `stepExceedingErrorThresholdNeedsImprovementAndUsesErrorReason`
  - `stepExceedingP95NeedsImprovement`
  - `nearThresholdStepIsWatch`
  - `healthyStepIsGood`
  - `zeroSampleStepIsWatch`
  - `stepAssessmentSummaryCountsGroups`
  - `errorReasonWinsOverLatencyReason`

**Done When:**
- Backend report payload has `stepAssessmentSummary()` and `stepAssessments()` accessors.
- `PerformanceManagementReportBuilder.build` returns sorted step assessments for every supplied step summary.
- Existing report fields still populate as before.
- Tests cover needs-improvement, watch, good, zero-sample, summary counts, and main reason priority.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -q "-Dtest=PerformanceManagementReportBuilderTest" test`. Expected: all `PerformanceManagementReportBuilderTest` tests pass.
- Manual: If `mvn` is not installed or not on PATH, stop and report that backend verification is blocked by missing Maven.

---

### TASK-02: Frontend Step Assessment Types And Labels

**Targets:**
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify)
- `../../apihub-fe/apihub-fe/messages/tr.json` (modify)
- `../../apihub-fe/apihub-fe/messages/en.json` (modify)

**Model Tier:** T2

**Implementation Notes:**
- In `types/performance.ts`, add:
  ```ts
  export type PerformanceManagementStepStatus = 'NEEDS_IMPROVEMENT' | 'WATCH' | 'GOOD';

  export interface PerformanceManagementStepAssessment {
      stepName?: string | null;
      status?: PerformanceManagementStepStatus | null;
      priority?: PerformanceManagementRiskLevel | null;
      mainReason?: string | null;
      evidence?: string | null;
      impact?: string | null;
      recommendation?: string | null;
      sampleCount?: number;
      successCount?: number;
      failureCount?: number;
      errorRate?: number;
      averageMs?: number;
      p90Ms?: number;
      p95Ms?: number;
      p99Ms?: number;
      throughputPerSecond?: number;
      lastError?: string | null;
  }
  ```
- Extend `PerformanceManagementReport` with:
  ```ts
  stepAssessmentSummary?: string | null;
  stepAssessments?: PerformanceManagementStepAssessment[];
  ```
  Keep all existing report fields.
- Add these keys under the `performance` object in `messages/tr.json`:
  ```json
  "stepAssessments": "Adım Durum Analizi",
  "needsImprovement": "İyileştirilmeli",
  "watch": "İzlenmeli",
  "goodCondition": "İyi durumda",
  "noStepAssessments": "Bu test için adım bazlı analiz üretilemedi.",
  "mostCriticalStep": "Öncelikli Adım",
  "priority": "Öncelik",
  "mainReason": "Ana Neden",
  "evidence": "Kanıt",
  "recommendation": "Öneri",
  "stepDetails": "Adım Detayları",
  "showStepDetails": "Detayı Göster",
  "hideStepDetails": "Detayı Gizle",
  "totalRequests": "Toplam İstek",
  "successfulRequests": "Başarılı İstek",
  "failedRequests": "Hatalı İstek",
  "averageResponseTime": "Ortalama Yanıt Süresi",
  "riskLow": "Düşük Risk",
  "riskMedium": "Orta Risk",
  "riskHigh": "Yüksek Risk",
  "riskCritical": "Kritik Risk"
  ```
- Add matching English keys under the `performance` object in `messages/en.json`:
  ```json
  "stepAssessments": "Step Assessment",
  "needsImprovement": "Needs Improvement",
  "watch": "Watch",
  "goodCondition": "Good Condition",
  "noStepAssessments": "Step-based analysis could not be generated for this test.",
  "mostCriticalStep": "Priority Step",
  "priority": "Priority",
  "mainReason": "Main Reason",
  "evidence": "Evidence",
  "recommendation": "Recommendation",
  "stepDetails": "Step Details",
  "showStepDetails": "Show Details",
  "hideStepDetails": "Hide Details",
  "totalRequests": "Total Requests",
  "successfulRequests": "Successful Requests",
  "failedRequests": "Failed Requests",
  "averageResponseTime": "Average Response Time",
  "riskLow": "Low Risk",
  "riskMedium": "Medium Risk",
  "riskHigh": "High Risk",
  "riskCritical": "Critical Risk"
  ```
- Keep existing keys such as `slaSummary`, `trendSummary`, `recommendedActions`, `detailExplanation`, `expected`, `actual`, `impact`, `passed`, and `failed`.
- Use valid JSON syntax with commas in the surrounding object. Do not duplicate an existing key in the same object.

**Done When:**
- `PerformanceManagementReport.stepAssessmentSummary` and `PerformanceManagementReport.stepAssessments` are typed.
- Frontend has type support for `NEEDS_IMPROVEMENT`, `WATCH`, and `GOOD`.
- Turkish and English message files contain every label needed by the updated report panel.
- Existing performance types remain available.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `node -e "JSON.parse(require('fs').readFileSync('messages/tr.json','utf8')); JSON.parse(require('fs').readFileSync('messages/en.json','utf8')); console.log('ok')"`. Expected: prints `ok`.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: command exits 0. Existing warnings are acceptable if there are 0 errors.

---

### TASK-03: Frontend Step Assessment Report Panel

**Targets:**
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (modify)

**Model Tier:** T3

**Implementation Notes:**
- Update the existing client component to import the new types:
  ```ts
  import {
      PerformanceManagementReport,
      PerformanceManagementRiskLevel,
      PerformanceManagementStepAssessment,
      PerformanceManagementStepStatus,
  } from '@/types/performance';
  ```
- Keep the component signature:
  ```ts
  interface PerformanceManagementReportPanelProps {
      report?: PerformanceManagementReport | null;
  }

  export default function PerformanceManagementReportPanel({ report }: PerformanceManagementReportPanelProps)
  ```
- Replace the single `detailsOpen` boolean with per-step detail state:
  ```ts
  const [openStepDetails, setOpenStepDetails] = useState<Record<string, boolean>>({});
  const [reportDetailsOpen, setReportDetailsOpen] = useState(false);
  ```
  `reportDetailsOpen` controls the existing bottom `detailExplanation` section.
- Add local color/format helpers in the same file:
  ```ts
  function riskColor(risk?: PerformanceManagementRiskLevel | null): 'success' | 'warning' | 'error' | 'default' {
      if (risk === 'LOW') return 'success';
      if (risk === 'MEDIUM') return 'warning';
      if (risk === 'HIGH' || risk === 'CRITICAL') return 'error';
      return 'default';
  }

  function stepStatusColor(status?: PerformanceManagementStepStatus | null): 'success' | 'warning' | 'error' | 'default' {
      if (status === 'GOOD') return 'success';
      if (status === 'WATCH') return 'warning';
      if (status === 'NEEDS_IMPROVEMENT') return 'error';
      return 'default';
  }

  function formatNumber(value?: number | null, suffix = ''): string {
      return typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(2)}${suffix}` : '-';
  }

  function formatPercent(value?: number | null): string {
      return typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(2)}%` : '-';
  }

  function formatMs(value?: number | null): string {
      return typeof value === 'number' && Number.isFinite(value) ? `${Math.round(value)} ms` : '-';
  }
  ```
- Inside `PerformanceManagementReportPanel`, after `const t = useTranslations('performance');`, create label maps:
  ```ts
  const riskLabels: Record<PerformanceManagementRiskLevel, string> = {
      LOW: t('riskLow'),
      MEDIUM: t('riskMedium'),
      HIGH: t('riskHigh'),
      CRITICAL: t('riskCritical'),
  };

  const stepStatusLabels: Record<PerformanceManagementStepStatus, string> = {
      NEEDS_IMPROVEMENT: t('needsImprovement'),
      WATCH: t('watch'),
      GOOD: t('goodCondition'),
  };
  ```
- Use this risk label mapping:
  - `LOW -> t('riskLow')`
  - `MEDIUM -> t('riskMedium')`
  - `HIGH -> t('riskHigh')`
  - `CRITICAL -> t('riskCritical')`
  - fallback `'-'`
- Use this step status label mapping:
  - `NEEDS_IMPROVEMENT -> t('needsImprovement')`
  - `WATCH -> t('watch')`
  - `GOOD -> t('goodCondition')`
  - fallback `'-'`
- Use `const stepAssessments = report.stepAssessments ?? [];`.
- Group assessments in the component:
  ```ts
  const needsImprovement = stepAssessments.filter((step) => step.status === 'NEEDS_IMPROVEMENT');
  const watch = stepAssessments.filter((step) => step.status === 'WATCH');
  const good = stepAssessments.filter((step) => step.status === 'GOOD');
  const priorityStep = needsImprovement[0] ?? watch[0] ?? null;
  ```
- The top summary `Paper` must show:
  - `t('managementReport')`
  - `report.overallStatus` chip when present.
  - `riskLabel(t, report.riskLevel)` chip when risk exists.
  - `report.stepAssessmentSummary || report.executiveSummary || t('reportUnavailable')`
  - If `priorityStep` exists, show `{t('mostCriticalStep')}: {priorityStep.stepName}`.
- Render a step assessment section before SLA:
  - Heading: `t('stepAssessments')`
  - If `stepAssessments.length === 0`, show `<Alert severity="info">{t('noStepAssessments')}</Alert>`.
  - Otherwise render up to three groups in this order: needs improvement, watch, good.
  - Hide empty groups instead of rendering empty boxes.
- Each group heading shows the translated group name and count, for example `İyileştirilmeli (3)`.
- Each step card short view shows:
  - `step.stepName || '-'`
  - step status chip using `stepStatusLabel`
  - priority chip using `riskLabel`
  - `{t('mainReason')}: {step.mainReason || '-'}`
  - `{t('evidence')}: {step.evidence || '-'}`
  - `{t('impact')}: {step.impact || '-'}`
  - `{t('recommendation')}: {step.recommendation || '-'}`
- Each step card has a details button keyed by `step.stepName ?? index` plus status:
  ```ts
  const stepKey = `${step.status ?? 'UNKNOWN'}-${step.stepName ?? index}`;
  ```
  Button text uses `openStepDetails[stepKey] ? t('hideStepDetails') : t('showStepDetails')`.
- The expanded step details show a compact metric grid/list:
  - `{t('totalRequests')}: {step.sampleCount ?? '-'}`
  - `{t('successfulRequests')}: {step.successCount ?? '-'}`
  - `{t('failedRequests')}: {step.failureCount ?? '-'}`
  - `{t('errorRate')}: {formatPercent(step.errorRate)}`
  - `{t('averageResponseTime')}: {formatMs(step.averageMs)}`
  - `P90: {formatMs(step.p90Ms)}`
  - `P95: {formatMs(step.p95Ms)}`
  - `P99: {formatMs(step.p99Ms)}`
  - `{t('throughput')}: {formatNumber(step.throughputPerSecond, ' req/s')}`
  - Render `{t('lastError')}: {step.lastError}` only when `step.lastError` is a non-empty string.
- Keep these existing sections below the new step assessment section:
  - SLA / threshold summary
  - Existing `problemAreas` section
  - Trend summary
  - Recommended actions
  - Bottom detail explanation with `reportDetailsOpen`
- Preserve the null report empty state:
  ```tsx
  if (!report) {
      return <Alert severity="info">{t('reportUnavailable')}</Alert>;
  }
  ```
- Do not modify `app/dashboard/performance/page.tsx`; the existing detail modal already passes `analysisData?.managementReport` to this panel.

**Done When:**
- Report panel renders grouped step assessments before the SLA section.
- Raw enum risk values such as `LOW` and `HIGH` are not displayed to users; translated labels are shown instead.
- Raw enum step statuses such as `NEEDS_IMPROVEMENT` are not displayed to users; translated labels are shown instead.
- Each step card has its own independent detail toggle.
- Existing SLA, problem area, trend, recommendation, and report detail sections still render.
- Missing `stepAssessments` falls back to the `noStepAssessments` message without crashing.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: command exits 0. Existing warnings are acceptable if there are 0 errors.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run build`. Expected: production build completes successfully. The existing `baseline-browser-mapping` age warnings are acceptable if the build exits 0.

---

### TASK-04: Final Cross-Contract Verification

**Targets:**
- `src/main/java/etiya/omniAutomation/business/dto/PerformanceManagementReport.java` (modify only if verification reveals constructor/order mismatch)
- `src/main/java/etiya/omniAutomation/service/PerformanceManagementReportBuilder.java` (modify only if verification reveals classification/report generation mismatch)
- `src/test/java/etiya/omniAutomation/service/PerformanceManagementReportBuilderTest.java` (modify only if verification reveals missing assertion coverage)
- `../../apihub-fe/apihub-fe/types/performance.ts` (modify only if verification reveals frontend/backend contract mismatch)
- `../../apihub-fe/apihub-fe/components/performance/PerformanceManagementReportPanel.tsx` (modify only if verification reveals rendering/type mismatch)
- `../../apihub-fe/apihub-fe/messages/tr.json` (modify only if verification reveals missing report key)
- `../../apihub-fe/apihub-fe/messages/en.json` (modify only if verification reveals missing report key)

**Model Tier:** T2

**Implementation Notes:**
- This is a narrow compatibility pass. Do not add new report features.
- Confirm the backend DTO field names exactly match frontend field names:
  - `stepAssessmentSummary`
  - `stepAssessments`
  - `stepName`
  - `status`
  - `priority`
  - `mainReason`
  - `evidence`
  - `impact`
  - `recommendation`
  - `sampleCount`
  - `successCount`
  - `failureCount`
  - `errorRate`
  - `averageMs`
  - `p90Ms`
  - `p95Ms`
  - `p99Ms`
  - `throughputPerSecond`
  - `lastError`
- Confirm frontend handles older responses where `stepAssessmentSummary` and `stepAssessments` are absent.
- Confirm the report panel still handles the old fields:
  - `slaSummary`
  - `problemAreas`
  - `trendSummary`
  - `recommendedActions`
  - `detailExplanation`
- If backend tests fail because expected constructor order changed, update only the constructor call sites in targeted tests.
- If frontend build fails due a missing translation key, add the exact missing key to both `messages/tr.json` and `messages/en.json`.
- If frontend build fails due optional fields, make the TypeScript interface field optional rather than forcing non-null API data.

**Done When:**
- Backend and frontend agree on the new report contract field names.
- Frontend remains backward compatible with report payloads that do not yet include step assessment fields.
- Existing management report fields still render after the new step assessment section.

**Verification:**
- Manual: From `C:\GitRepo\Apihub\Apihub\Apihub`, run `mvn -q "-Dtest=PerformanceManagementReportBuilderTest,PerformanceExportServiceTest" test`. Expected: tests pass. If `mvn` is unavailable, stop and report that backend verification is blocked by missing Maven.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `node -e "JSON.parse(require('fs').readFileSync('messages/tr.json','utf8')); JSON.parse(require('fs').readFileSync('messages/en.json','utf8')); console.log('ok')"`. Expected: prints `ok`.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run lint`. Expected: command exits 0. Existing warnings are acceptable if there are 0 errors.
- Manual: From `C:\GitRepo\Apihub\apihub-fe\apihub-fe`, run `npm run build`. Expected: production build exits 0.
