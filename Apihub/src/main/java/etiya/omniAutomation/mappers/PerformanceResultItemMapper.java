package etiya.omniAutomation.mappers;

import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.entity.PerfRsltItemEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ProcessFlowStepMapper.class})
public interface PerformanceResultItemMapper {

    PerformanceResultItemMapper INSTANCE = Mappers.getMapper(PerformanceResultItemMapper.class);

}
