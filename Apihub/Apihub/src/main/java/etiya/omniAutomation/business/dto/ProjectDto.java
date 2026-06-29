package etiya.omniAutomation.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProjectDto extends AbstractDto {

    private Long projectId;
    private String name;
    private String description;
    private String shortCode;
    private List<GeneralWebSystemDto> generalWebSystemDtoList;

}
