package etiya.omniAutomation.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceValidationNoteRequest {

    private Long performanceResultId;
    private String note;
}
