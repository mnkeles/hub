package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proc_flow_step_rel_parm")
public class ProcessFlowStepRelationParmEntity {

    @Id
    @GeneratedValue
    @Column(name = "proc_flow_step_rel_parm_id")
    private long processFlowStepRelationParmId;

    @Column(name = "shrt_code")
    private String shortCode;

    @Column(name = "val_expr")
    private String valueExpr;

    @Column(name = "proc_flow_step_rel_id")
    private long processFlowStepRelationId;

    @ManyToOne()
    @JoinColumn(name = "proc_flow_step_rel_id", insertable = false,updatable = false)
    private ProcessFlowStepRelationEntity processFlowStepRelationEntity;
}
