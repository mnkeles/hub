package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateDto extends AbstractDto {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Long projectId;

}