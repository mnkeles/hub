package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.PerformanceResultDto;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {PerformanceResultItemMapper.class, ProjectMapper.class, ProcessFlowMapper.class})
public interface PerformanceResultMapper {

    PerformanceResultMapper INSTANCE = Mappers.getMapper(PerformanceResultMapper.class);

    @Mapping(target = "performanceResultId", source = "perfRsltId")
    @Mapping(target = "performanceStatus", source = "perfStatus")
    @Mapping(target = "projectDto", ignore = true)
    @Mapping(target = "processFlowDto", source = "processFlowEntity")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "runSummary", source = "runSummary")
    @Mapping(target = "thresholdResult", source = "thresholdResult")
    @Mapping(target = "analysisSummary", source = "analysisSummary")
    @Mapping(target = "errorAnalysis", source = "errorAnalysis")
    @Mapping(target = "environmentMetrics", source = "environmentMetrics")
    @Mapping(target = "insightReport", source = "insightReport")
    @Mapping(target = "aiManagementReport", source = "aiManagementReport")
    @Mapping(target = "performanceSummaries", source = "summary")
    @Mapping(target = "resultSchemaVersion", source = "resultSchemaVersion")
    @Mapping(target = "thresholdPreset", source = "thresholdPreset")
    @Mapping(target = "thresholdConfig", source = "thresholdConfig")
    @Mapping(target = "baseline", source = "baseline")
    @Mapping(target = "baselineResultId", source = "baselineResultId")
    @Mapping(target = "baselineComparison", source = "baselineComparison")
    @Mapping(target = "validationChecklist", source = "validationChecklist")
    @Named("toDto")
    PerformanceResultDto toDto(PerfRsltEntity entity);

    @Mapping(target = "perfRsltId", source = "performanceResultId")
    @Mapping(target = "perfStatus", source = "performanceStatus")
    @Mapping(target = "processFlowEntity", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "runSummary", source = "runSummary")
    @Mapping(target = "thresholdResult", source = "thresholdResult")
    @Mapping(target = "analysisSummary", source = "analysisSummary")
    @Mapping(target = "errorAnalysis", source = "errorAnalysis")
    @Mapping(target = "environmentMetrics", source = "environmentMetrics")
    @Mapping(target = "insightReport", source = "insightReport")
    @Mapping(target = "aiManagementReport", source = "aiManagementReport")
    @Mapping(target = "summary", source = "performanceSummaries")
    @Mapping(target = "resultSchemaVersion", source = "resultSchemaVersion")
    @Mapping(target = "thresholdPreset", source = "thresholdPreset")
    @Mapping(target = "thresholdConfig", source = "thresholdConfig")
    @Mapping(target = "baseline", source = "baseline")
    @Mapping(target = "baselineResultId", source = "baselineResultId")
    @Mapping(target = "baselineComparison", source = "baselineComparison")
    @Mapping(target = "validationChecklist", source = "validationChecklist")
    PerfRsltEntity toEntity(PerformanceResultDto dto);
}
