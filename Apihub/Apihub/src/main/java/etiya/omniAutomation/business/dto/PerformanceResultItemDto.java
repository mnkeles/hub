package etiya.omniAutomation.business.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import etiya.omniAutomation.common.GeneralEnums;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PerformanceResultItemDto extends AbstractDto {

    @JsonIgnore
    private Long processFlowStepId;
    private String stepName;
    private GeneralEnums.PerformanceStatus performanceItemStatus;
    private double elapsedTime;
    private String errorMessage;
    private int threadNumber;
    private Date startedAt;
    private Date finishedAt;

}
