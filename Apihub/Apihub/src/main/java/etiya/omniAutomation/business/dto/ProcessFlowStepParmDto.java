package etiya.omniAutomation.business.dto;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
public class ProcessFlowStepParmDto extends AbstractDto {

    private Long processFlowStepParmId;
    private String shortCode;
    private String valExpression;
    private String value;
    private Integer paramOrder;
    private boolean useContext;
    private String sql;
    private String code;

}
