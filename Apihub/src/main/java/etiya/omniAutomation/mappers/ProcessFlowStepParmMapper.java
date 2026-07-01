package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.ProcessFlowStepParmDto;
import etiya.omniAutomation.entity.ProcessFlowStepParmEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProcessFlowStepParmMapper {

    ProcessFlowStepParmMapper INSTANCE = Mappers.getMapper(ProcessFlowStepParmMapper.class);

    ProcessFlowStepParmDto toDto(ProcessFlowStepParmEntity entity);

    List<ProcessFlowStepParmDto> toDtoList(List<ProcessFlowStepParmEntity> entityList);

    @Mappings({
            @Mapping(source = "value", target = "value", conditionExpression = "java(org.apache.commons.lang3.StringUtils.isNotEmpty(processFlowStepParmDto.getValue()))"),
            @Mapping(source = "sql", target = "sql", conditionExpression = "java(org.apache.commons.lang3.StringUtils.isNotEmpty(processFlowStepParmDto.getSql()))"),
            @Mapping(source = "valExpression", target = "valExpression", conditionExpression = "java(org.apache.commons.lang3.StringUtils.isNotEmpty(processFlowStepParmDto.getValExpression()))"),
    })
    @InheritInverseConfiguration
    ProcessFlowStepParmEntity toEntity(ProcessFlowStepParmDto processFlowStepParmDto);
}
