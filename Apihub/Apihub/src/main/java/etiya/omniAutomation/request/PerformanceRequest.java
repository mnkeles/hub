package etiya.omniAutomation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceRequest {

    private Long processFlowId;
    private Long processFlowStepId;
    @NotEmpty
    private String environment;
    @NotNull
    private Long projectId;
    @Positive
    private Integer threadCount;
    private Integer rampUpPeriod = 0;
    private Integer durationSeconds;
    private Integer loopCount;
    private Integer thinkTimeMs;
    private Integer timeoutMs;
    private Long testDataId;
    private String environmentBaseUrl;
    private PerformanceThresholdPreset thresholdPreset = PerformanceThresholdPreset.NORMAL;
    private Double maxErrorRatePercent;
    private Double maxAverageMs;
    private Double maxP95Ms;
    private Double maxP99Ms;
    private Double minThroughputPerSecond;

}
