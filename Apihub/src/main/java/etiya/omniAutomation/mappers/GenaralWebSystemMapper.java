package etiya.omniAutomation.mappers;


import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.entity.GnlWebSysEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GenaralWebSystemMapper {
    GenaralWebSystemMapper INSTANCE = Mappers.getMapper(GenaralWebSystemMapper.class);

    GeneralWebSystemDto toDto(GnlWebSysEntity entity);

    GnlWebSysEntity toEntity(GeneralWebSystemDto dto);

}
