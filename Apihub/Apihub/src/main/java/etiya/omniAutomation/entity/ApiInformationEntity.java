package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "gnl_api_information")
public class ApiInformationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gnl_api_information_seq")
    @SequenceGenerator(name = "gnl_api_information_seq", sequenceName = "gnl_api_information_gnl_api_information_id_seq", allocationSize = 1)
    @Column(name = "gnl_api_information_id")
    private long id;

    @Column(name = "api_provider_system_id")
    private long providerSystemId;

    @Column(name = "srvc_name")
    private String srvcName;

    @Column(name = "name")
    private String name;

    @Column(name = "header_parameters")
    private String headerParameters;

    @Column(name = "status_code")
    private int statusCode;

    @Column(name = "pl_in")
    private String plIn;

    @Column(name = "http_method")
    private String httpMethod;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udate")
    private Date udate = new Date();

    @Column(name = "is_actv")
    private int isActive;

    @Column(name = "api_short_code")
    private String apiShortCode;

    @ManyToOne()
    @JoinColumn(name = "api_provider_system_id", insertable = false,updatable = false)
    private ProviderSystemEntity providerSystemEntity;

    @Column(name = "media_type")
    private String mediaType;

    @Transient
    private String headerVal;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "method_name")
    private String methodName;

    @Column(name = "is_grpc")
    private boolean grpc;

    @Column(name = "is_token_api")
    private boolean isTokenApi;

    @Column(name = "external_api")
    private boolean externalApi;

    @Column(name = "sql_query")
    private boolean sqlQuery;
}
