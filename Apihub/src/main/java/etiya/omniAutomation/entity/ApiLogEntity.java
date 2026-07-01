package etiya.omniAutomation.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "gnl_api_call_log")
public class ApiLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gnl_api_call_log_id")
    private Long id;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "status_code")
    private int statusCode;

    @Column(name = "pl_in")
    private String plIn;

    @Column(name = "pl_out")
    private String plOut;

    @Column(name = "txn_id")
    private UUID txnId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate;

    public ApiLogEntity(String projectName, int statusCode, String apiUrl, String plIn, String plOut) {
        this.statusCode = statusCode;
        this.projectName = projectName;
        this.apiUrl = apiUrl;
        this.plIn = plIn;
        this.plOut = plOut;
        this.cdate = new Date();
        this.txnId = UUID.randomUUID();
    }
}
