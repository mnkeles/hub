package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.DatabaseConfigDto;
import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.entity.DatabaseConfigEntity;
import etiya.omniAutomation.entity.GnlWebSysEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DatabaseConfigMapper {
    DatabaseConfigMapper INSTANCE = Mappers.getMapper(DatabaseConfigMapper.class);

    DatabaseConfigDto toDto(DatabaseConfigEntity entity);

    DatabaseConfigEntity toEntity(DatabaseConfigDto dto);
}
