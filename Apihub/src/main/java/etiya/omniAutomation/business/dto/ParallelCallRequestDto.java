package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParallelCallRequestDto {

    private ParameterRequestDto parameterRequest;
    private int totalCalls = 1;
}
