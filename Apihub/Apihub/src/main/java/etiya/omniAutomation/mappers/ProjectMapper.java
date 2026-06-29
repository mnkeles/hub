package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.entity.ProjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {GenaralWebSystemMapper.class})
public interface ProjectMapper {

    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);

    @Mapping(target = "generalWebSystemDtoList", source = "gnlWebSysEntityList")
    ProjectDto toDto(ProjectEntity entity);

    @Mapping(target = "gnlWebSysEntityList", source = "generalWebSystemDtoList")
    ProjectEntity toEntity(ProjectDto dto);

    List<ProjectDto> toDtoList(List<ProjectEntity> entityList);

}
