package etiya.omniAutomation.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PerformanceDatasetRequest {

    private Long projectId;
    private String name;
    private String description;
    private Map<String, String> defaultMapping;
}
