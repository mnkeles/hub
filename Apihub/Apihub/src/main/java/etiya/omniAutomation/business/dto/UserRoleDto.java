package etiya.omniAutomation.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserRoleDto extends AbstractDto {

    @JsonProperty("id")
    private long userRoleId;

    private String firstName;

    private String lastName;

    private String email;

    private String shortCode;
}
