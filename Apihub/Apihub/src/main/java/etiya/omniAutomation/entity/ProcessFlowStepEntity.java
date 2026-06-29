package etiya.omniAutomation.entity;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proc_flow_step")
public class ProcessFlowStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proc_flow_step_seq")
    @SequenceGenerator(name = "proc_flow_step_seq", sequenceName = "proc_flow_step_proc_flow_step_id_seq", allocationSize = 1)
    @Column(name = "proc_flow_step_id")
    private Long processFlowStepId;

    @Column(name = "gnl_api_information_id")
    private Long gnlApiInformationId;

    @Column(name = "proc_flow_id")
    private Long processFlowId;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(name = "step_short_code")
    private String stepShortCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proc_flow_id", insertable = false,updatable = false)
    private ProcessFlowEntity processFlowEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gnl_api_information_id", insertable = false,updatable = false)
    private ApiInformationEntity apiInformationEntity;

    @OneToMany(mappedBy = "processFlowStep", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @ToString.Exclude()
    private List<ProcessFlowStepRelationEntity> processFlowStepRelationEntities;

    @Column(name = "pl_in")
    private String plIn;

    @Column(name = "header_extractor")
    private String headerExtractor;

    @Column(name = "parameter_extractor")
    private String parameterExtractor;

    @Column(name = "pre_header")
    private String preHeader;
}
