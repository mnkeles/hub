package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto implements Serializable {
    private Long permissionId;
    private String permissionKey;
    private String name;
    private String description;
    private String category;
    private Boolean uiVisible;
    private Boolean serviceAssignable;
    private int enabled;
}
