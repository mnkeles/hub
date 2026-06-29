package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.ProcessFlowStepRelationDto;
import etiya.omniAutomation.entity.ProcessFlowStepRelationEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ProcessFlowStepParmMapper.class})
public interface ProcessFlowStepRelationMapper {

    ProcessFlowStepRelationMapper INSTANCE = Mappers.getMapper(ProcessFlowStepRelationMapper.class);

    @Mappings({
            @Mapping(source = "processFlowStep", target = "processFlowStep", ignore = true),
            @Mapping(source = "processFlowStepParm", target = "processFlowStepParameters")
    })
    ProcessFlowStepRelationDto toDto(ProcessFlowStepRelationEntity entity);

    List<ProcessFlowStepRelationDto> toDtoList(List<ProcessFlowStepRelationEntity> entityList);

    @Mapping(target = "processFlowStep", ignore = true)
    @Mapping(target = "processFlowStepParm", ignore = true)
    @Mapping(target = "processFlowStepId", source = "processFlowStepId")
    @Mapping(target = "processFlowStepParmId", source = "processFlowStepParmId")
    @Mapping(target = "projectId", source = "projectId")
    ProcessFlowStepRelationEntity toEntity(ProcessFlowStepRelationDto dto);

}
