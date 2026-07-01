package etiya.omniAutomation.business.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProcessFlowStepRelationDto extends AbstractDto {

    private Long processFlowStepRelationId;
    private Long projectId;
    private ProcessFlowStepDto processFlowStep;
    private ProcessFlowStepParmDto processFlowStepParameters;
    private Long processFlowStepId;
    private Long processFlowStepParmId;
    private List<ProcessFlowStepParmDto> processFlowStepParms;
}
