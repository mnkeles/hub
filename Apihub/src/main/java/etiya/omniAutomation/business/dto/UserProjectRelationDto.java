package etiya.omniAutomation.business.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProjectRelationDto extends AbstractDto {

    private Long userProjectRelId;
    private boolean isActv;
    private ProjectDto projectDto;
    private UserDto userDto;

}
