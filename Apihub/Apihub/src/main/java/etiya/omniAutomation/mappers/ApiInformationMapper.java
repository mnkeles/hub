package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.ApiInformationDto;
import etiya.omniAutomation.entity.ApiInformationEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ProviderSystemMapper.class})
public interface ApiInformationMapper {
    ApiInformationMapper INSTANCE = Mappers.getMapper(ApiInformationMapper.class);

    @Mappings({
            @Mapping(source = "providerSystemEntity", target = "providerSystemInfo"),
            @Mapping(target = "active", expression = "java(java.util.Objects.equals(apiInformationEntity.getIsActive(), 1)  ? \"Aktif\" : \"Pasif\")"),
    })
    ApiInformationDto toDto(ApiInformationEntity apiInformationEntity);

    List<ApiInformationDto> toDtoList(List<ApiInformationEntity> entityList);

    @InheritInverseConfiguration
    @Mapping(target = "isActive", expression = "java(org.apache.commons.lang3.StringUtils.equalsIgnoreCase(\"Aktif\", apiInformationDto.getActive()) ? 1 : 0)")
    @Mapping(target = "headerParameters", source = "headerParameters", conditionExpression = "java(org.apache.commons.lang3.StringUtils.isNotEmpty(apiInformationDto.getHeaderParameters()))")
    ApiInformationEntity toEntity(ApiInformationDto apiInformationDto);
}
