package etiya.omniAutomation.business.dto;

import lombok.*;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessFlowInstanceDto extends AbstractDto {

    private long id;

    private String plIn;

    private String plOut;

    private int statusCode;

    private UUID txnId;

    private String shortCode;

    private String srvcName;

    private Date cdate;
}
