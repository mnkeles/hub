package etiya.omniAutomation.business.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import etiya.omniAutomation.common.GeneralEnums;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PerformanceResultDto extends AbstractDto {

    private Long performanceResultId;
    private GeneralEnums.PerformanceStatus performanceStatus;
    @JsonIgnore
    private Long projectId;
    @JsonIgnore
    private Long processFlowId;
    @JsonIgnore
    private ProjectDto projectDto;
    @JsonIgnore
    private ProcessFlowDto processFlowDto;
    private Integer threadCount;
    private Integer rampUpPeriod;
    private PerformanceThreadGroup threadGroup;
    private PerformanceRunSummary runSummary;
    private List<PerformanceSummary> performanceSummaries;
    private PerformanceThresholdResult thresholdResult;
    private PerformanceAnalysisSummary analysisSummary;
    private PerformanceErrorAnalysis errorAnalysis;
    private PerformanceEnvironmentMetrics environmentMetrics;
    private PerformanceInsightReport insightReport;
    private PerformanceAiManagementReport aiManagementReport;
    private Integer resultSchemaVersion;
    private PerformanceThresholdPreset thresholdPreset;
    private PerformanceThresholdConfig thresholdConfig;
    private Boolean baseline;
    private Long baselineResultId;
    private PerformanceComparisonResult baselineComparison;
    private PerformanceValidationChecklist validationChecklist;
    private Integer durationSeconds;
    private Integer loopCount;
    private Integer thinkTimeMs;
    private Integer timeoutMs;
    private String environmentBaseUrl;
    private Date createdAt;

}
