package etiya.omniAutomation.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PerformanceDatasetRowRequest {

    private Map<String, Object> data;
}
