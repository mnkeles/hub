package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.ProcessFlowStepDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepParmDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepRelationDto;
import etiya.omniAutomation.entity.ProcessFlowStepEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(uses = {ApiInformationMapper.class, ProcessFlowStepRelationMapper.class})
public interface ProcessFlowStepMapper {

    ProcessFlowStepMapper INSTANCE = Mappers.getMapper(ProcessFlowStepMapper.class);

    @Mappings({
            @Mapping(source = "apiInformationEntity", target = "apiInformation"),
            @Mapping(source = "processFlowStepRelationEntities", target = "processFlowStepRelationList")
    })
    ProcessFlowStepDto toDto(ProcessFlowStepEntity entity);

    List<ProcessFlowStepDto> toDtoList(List<ProcessFlowStepEntity> entityList);

    @InheritInverseConfiguration
    @Mapping(target = "gnlApiInformationId", expression = "java(dto.getGnlApiInformationId() != null ? dto.getGnlApiInformationId() : (dto.getApiInformation() == null ? null : dto.getApiInformation().getId()))")
    ProcessFlowStepEntity toEntity(ProcessFlowStepDto dto);

    @AfterMapping
    default void gatherParameters(@MappingTarget ProcessFlowStepDto targetDto) {
        if (targetDto.getProcessFlowStepRelationList() == null) {
            return;
        }
        List<ProcessFlowStepParmDto> parameters = targetDto.getProcessFlowStepRelationList().stream()
                .map(ProcessFlowStepRelationDto::getProcessFlowStepParameters)
                .collect(Collectors.toList());
        targetDto.setProcessFlowStepParmList(parameters);
    }
}
