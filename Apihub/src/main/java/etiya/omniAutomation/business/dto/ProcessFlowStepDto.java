package etiya.omniAutomation.business.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProcessFlowStepDto extends AbstractDto {

    private Long processFlowStepId;
    private Long gnlApiInformationId;
    private Long processFlowId;
    private Integer stepOrder;
    private String stepShortCode;
    private ApiInformationDto apiInformation;
    private List<ProcessFlowStepRelationDto> processFlowStepRelationList;
    private String plIn;
    private String headerExtractor;
    private String parameterExtractor;
    private List<ProcessFlowStepParmDto> processFlowStepParmList;
    private String preHeader;
    
}
