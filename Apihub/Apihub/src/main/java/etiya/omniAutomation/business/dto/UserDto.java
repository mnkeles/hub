package etiya.omniAutomation.business.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto extends AbstractDto {

    private String firstName;
    private String lastName;
    private String email;
    private String token;
    private Long projectId;

}
