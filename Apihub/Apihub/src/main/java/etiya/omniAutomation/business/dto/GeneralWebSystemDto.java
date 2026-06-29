package etiya.omniAutomation.business.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GeneralWebSystemDto extends AbstractDto {
    private Long gnlWebSysId;
    private String name;
    private String shortCode;
    private String url;
    private boolean isActv;
    private Long projectId;
    private boolean isTokenUrl;
    private String baseUrlShortCode;
}
