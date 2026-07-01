'use client';

import { Alert, Box } from '@mui/material';
import { useTranslations } from 'next-intl';
import {
    PerformanceAiManagementReport,
    PerformanceComparisonResult,
    PerformanceInsightReport,
    PerformanceManagementReport,
} from '@/types/performance';
import PerformanceActionPlanPanel from './PerformanceActionPlanPanel';
import PerformanceAiNarrativePanel from './PerformanceAiNarrativePanel';
import PerformanceAiObservabilityPanel from './PerformanceAiObservabilityPanel';
import PerformanceAiRegenerateButton from './PerformanceAiRegenerateButton';
import PerformanceExecutiveSummaryPanel from './PerformanceExecutiveSummaryPanel';
import PerformancePrintActions from './PerformancePrintActions';
import PerformanceRegressionTrendPanel from './PerformanceRegressionTrendPanel';
import PerformanceReportDecisionHeader from './PerformanceReportDecisionHeader';
import PerformanceRiskMatrixPanel from './PerformanceRiskMatrixPanel';
import PerformanceRootCauseHintsPanel from './PerformanceRootCauseHintsPanel';
import PerformanceStepRiskPanel from './PerformanceStepRiskPanel';
import PerformanceTechnicalFindingsPanel from './PerformanceTechnicalFindingsPanel';

interface PerformanceManagementReportPanelProps {
    report?: PerformanceManagementReport | null;
    insightReport?: PerformanceInsightReport | null;
    aiReport?: PerformanceAiManagementReport | null;
    baselineComparison?: PerformanceComparisonResult | null;
    performanceResultId?: number;
    onAiReportUpdated?: (report: PerformanceAiManagementReport) => void;
    onSuccess?: (message: string) => void;
    onError?: (message: string) => void;
}

export default function PerformanceManagementReportPanel({
    report,
    insightReport,
    aiReport,
    baselineComparison,
    performanceResultId,
    onAiReportUpdated,
    onSuccess,
    onError,
}: PerformanceManagementReportPanelProps) {
    const t = useTranslations('performance');

    if (!report && !insightReport && !aiReport) {
        return <Alert severity="info">{t('reportUnavailable')}</Alert>;
    }

    return (
        <Box className="performance-report-print-root" sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box
                className="performance-report-print-hidden"
                sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, flexWrap: 'wrap' }}
            >
                <PerformancePrintActions />
                <PerformanceAiRegenerateButton
                    performanceResultId={performanceResultId}
                    onRegenerated={(updatedReport) => onAiReportUpdated?.(updatedReport)}
                    onSuccess={onSuccess}
                    onError={onError}
                />
            </Box>
            <PerformanceReportDecisionHeader managementReport={report} insightReport={insightReport} aiReport={aiReport} />
            <PerformanceRiskMatrixPanel managementReport={report} insightReport={insightReport} baselineComparison={baselineComparison} />
            <PerformanceExecutiveSummaryPanel managementReport={report} aiReport={aiReport} />
            <PerformanceTechnicalFindingsPanel insightReport={insightReport} managementReport={report} />
            <PerformanceStepRiskPanel managementReport={report} insightReport={insightReport} />
            <PerformanceRegressionTrendPanel
                insightReport={insightReport}
                baselineComparison={baselineComparison}
                trendSummary={report?.trendSummary}
            />
            <PerformanceRootCauseHintsPanel insightReport={insightReport} />
            <PerformanceAiNarrativePanel aiReport={aiReport} />
            <PerformanceAiObservabilityPanel aiReport={aiReport} insightReport={insightReport} />
            <PerformanceActionPlanPanel aiReport={aiReport} managementReport={report} />
        </Box>
    );
}
