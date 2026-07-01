package etiya.omniAutomation.business.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfigDto extends AbstractDto {
    private Long dbConfigId;
    private String shortCode;
    private String url;
    private String username;
    private String password;
    private boolean isActv;
    private String schema;
    private String driver;
    private Long projectId;
}
