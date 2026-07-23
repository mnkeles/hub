package etiya.omniAutomation.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceScheduleRequest {

    private Long projectId;
    private Long processFlowId;
    private String name;
    private String cronExpression;
    private String timezone;
    private Boolean enabled;
    private PerformanceRequest requestSnapshot;
}
