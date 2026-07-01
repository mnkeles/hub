package etiya.omniAutomation.business.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessFlowWithRelationsDto {
    
    private Long processFlowId;
    private String shortCode;
    private String name;
    private Long projectId;
    private List<StepWithParametersDto> processFlowStepRelations;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepWithParametersDto {
        private Long processFlowStepId;
        private String stepShortCode;
        private Integer stepOrder;
        private Long gnlApiInformationId;
        private List<ProcessFlowStepParmDto> processFlowStepParms;
    }
}
