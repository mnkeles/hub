package etiya.omniAutomation.business.dto;

import lombok.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ApiInformationDto extends AbstractDto {

    private Long id;
    private Integer providerSystemId;
    private String name;
    private String shortCode;
    private String srvcName;
    private String headerParameters;
    private Integer statusCode;
    private String plIn;
    private String httpMethod;
    private Date cdate;
    private Date udate;
    private String active;
    private String apiShortCode;
    private String mediaType;
    private String headerVal;
    private ProviderSystemDto providerSystemInfo;
    private Long projectId;
    private final Map<String, String> globalHeaders = new HashMap<>();
    private boolean grpc;
    private String serviceName;
    private String methodName;
    private boolean isTokenApi;
    private boolean externalApi;
    private boolean sqlQuery;
}
