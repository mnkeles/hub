package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAccountDto implements Serializable {
    private Long serviceAccountId;
    private String serviceCode;
    private String name;
    private String description;
    private String owner;
    private int enabled;
}
