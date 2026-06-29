package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.ProcessFlowDto;
import etiya.omniAutomation.entity.ProcessFlowEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {ProcessFlowStepMapper.class})
public interface ProcessFlowMapper {

    ProcessFlowMapper INSTANCE = Mappers.getMapper(ProcessFlowMapper.class);

    @Mappings({
            @Mapping(source = "processFlowStepEntities", target = "processFlowStepList"),
            @Mapping(target = "isActive", expression = "java(entity.isActive() ? \"Aktif\" : \"Pasif\")"),
    })
    @Named("toDto")
    ProcessFlowDto toDto(ProcessFlowEntity entity);

    @Mappings({
            @Mapping(target = "processFlowStepList", ignore = true),
            @Mapping(target = "isActive", expression = "java(entity.isActive() ? \"Aktif\" : \"Pasif\")"),
    })
    @Named("toDtoWithoutRelations")
    ProcessFlowDto toDtoWithoutRelations(ProcessFlowEntity entity);

    @InheritInverseConfiguration(name = "toDtoWithoutRelations")
    @Mapping(target = "active", expression = "java(org.apache.commons.lang3.StringUtils.equalsIgnoreCase(\"Aktif\", dto.getIsActive()) ? true : false)")
    ProcessFlowEntity toEntity(ProcessFlowDto dto);
}
