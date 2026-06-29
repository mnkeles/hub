package etiya.omniAutomation.business.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ProcessFlowStepRelationParmDto extends AbstractDto {

    private int processFlowStepRelationParmId;

    private String shortCode;

    private String valueExpr;
    
}
