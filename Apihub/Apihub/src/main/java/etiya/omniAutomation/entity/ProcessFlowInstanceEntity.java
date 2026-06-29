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
@Table(name = "proc_flow_insc")
public class ProcessFlowInstanceEntity {

    @Id
    @GeneratedValue
    @Column(name = "proc_flow_insc_id")
    private long id;

    @Column(name = "pl_in")
    private String plIn;

    @Column(name = "pl_out")
    private String plOut;

    @Column(name = "proc_flow_id")
    private long processFlowId;

    @Column(name = "gnl_api_information_id")
    private long apiInformationId;

    @Column(name = "status_code")
    private int statusCode;

    @Column(name = "txn_id")
    private UUID txnId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate;

    @ManyToOne()
    @JoinColumn(name = "proc_flow_id", insertable = false,updatable = false)
    private ProcessFlowEntity processFlowEntity;

    @ManyToOne()
    @JoinColumn(name = "gnl_api_information_id", insertable = false,updatable = false)
    private ApiInformationEntity apiInformationEntity;

    public ProcessFlowInstanceEntity(String plIn, String plOut, long procFlowId, long apiInformationId, int statusCode, UUID txnId, Date cdate) {
        this.plIn = plIn;
        this.plOut = plOut;
        this.processFlowId = procFlowId;
        this.apiInformationId = apiInformationId;
        this.statusCode = statusCode;
        this.txnId = txnId;
        this.cdate = cdate;
    }
}
