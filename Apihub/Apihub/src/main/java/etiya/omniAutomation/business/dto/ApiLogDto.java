package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ApiLogDto extends AbstractDto {

    private long providerSystemId;

    private String providerSystemDescription;

    private int statusCode;

    private String plOut;

    private UUID txnId;

    private long apiInformationId;

}
