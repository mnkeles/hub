package etiya.omniAutomation.business.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProviderSystemDto extends AbstractDto{

    private int id;

    private String description;

    private String shortCode;
}
