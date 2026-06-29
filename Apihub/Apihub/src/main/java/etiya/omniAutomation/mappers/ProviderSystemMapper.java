package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.ProviderSystemDto;
import etiya.omniAutomation.entity.ProviderSystemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProviderSystemMapper {
    ProviderSystemMapper INSTANCE = Mappers.getMapper(ProviderSystemMapper.class);

    ProviderSystemDto toDto(ProviderSystemEntity entity);


}
