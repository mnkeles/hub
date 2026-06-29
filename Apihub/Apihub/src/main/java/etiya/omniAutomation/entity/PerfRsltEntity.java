package etiya.omniAutomation.entity;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.common.GeneralEnums;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "perf_rslt")
public class PerfRsltEntity {

    @Id
    @Column(name = "perf_rslt_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "perf_rslt_id_seq")
    @SequenceGenerator(name = "perf_rslt_id_seq", sequenceName = "perf_rslt_perf_rslt_id_seq", allocationSize = 1)
    private Long perfRsltId;

    @Column(name = "perf_status")
    @Enumerated(EnumType.STRING)
    private GeneralEnums.PerformanceStatus perfStatus;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "process_flow_id")
    private Long processFlowId;

    @Column(name = "thread_count")
    private Integer threadCount;

    @Column(name = "ramp_up_period")
    private Integer rampUpPeriod;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "loop_count")
    private Integer loopCount;

    @Column(name = "think_time_ms")
    private Integer thinkTimeMs;

    @Column(name = "timeout_ms")
    private Integer timeoutMs;

    @Column(name = "environment_base_url")
    private String environmentBaseUrl;

    @Column(name = "result_schema_version")
    private Integer resultSchemaVersion;

    @Column(name = "threshold_preset")
    @Enumerated(EnumType.STRING)
    private PerformanceThresholdPreset thresholdPreset;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "threshold_config", columnDefinition = "jsonb")
    private PerformanceThresholdConfig thresholdConfig;

    @Column(name = "is_baseline")
    private Boolean baseline;

    @Column(name = "baseline_result_id")
    private Long baselineResultId;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "baseline_comparison", columnDefinition = "jsonb")
    private PerformanceComparisonResult baselineComparison;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_checklist", columnDefinition = "jsonb")
    private PerformanceValidationChecklist validationChecklist;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON_ARRAY)
    @Column(name = "summary", columnDefinition = "jsonb")
    private List<PerformanceSummary> summary;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "run_summary", columnDefinition = "jsonb")
    private PerformanceRunSummary runSummary;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "threshold_result", columnDefinition = "jsonb")
    private PerformanceThresholdResult thresholdResult;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_summary", columnDefinition = "jsonb")
    private PerformanceAnalysisSummary analysisSummary;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_analysis", columnDefinition = "jsonb")
    private PerformanceErrorAnalysis errorAnalysis;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "environment_metrics", columnDefinition = "jsonb")
    private PerformanceEnvironmentMetrics environmentMetrics;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_flow_id", insertable = false, updatable = false)
    private ProcessFlowEntity processFlowEntity;

    @Column(name = "created_at")
    private Date createdAt;
}
