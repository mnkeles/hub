package etiya.omniAutomation.request;

import etiya.omniAutomation.common.GeneralEnums;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GeneralFilter {

    private GeneralEnums.FilterCriteria criteria;
    private String value;
    private Long numberValue;

}
