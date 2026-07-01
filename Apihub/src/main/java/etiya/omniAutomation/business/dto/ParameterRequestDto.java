package etiya.omniAutomation.business.dto;

import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
public class ParameterRequestDto {
    private Map<String, Object> parameterContext;
    private Map<String, Object> globalHeaders;
    private boolean combination  = false;
}
