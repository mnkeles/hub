package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proc_flow_step_rel")
public class ProcessFlowStepRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proc_flow_step_rel_seq")
    @SequenceGenerator(name = "proc_flow_step_rel_seq", sequenceName = "proc_flow_step_rel_proc_flow_step_rel_id_seq", allocationSize = 1)
    @Column(name = "proc_flow_step_rel_id")
    private Long processFlowStepRelationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proc_flow_step_id", insertable = false, updatable = false)
    private ProcessFlowStepEntity processFlowStep;

    @ManyToOne
    @JoinColumn(name = "proc_flow_step_parm_id", insertable = false, updatable = false)
    private ProcessFlowStepParmEntity processFlowStepParm;

    @Column(name = "proc_flow_step_id")
    private Long processFlowStepId;

    @Column(name = "proc_flow_step_parm_id")
    private Long processFlowStepParmId;

    @Column(name = "project_id")
    @OrderBy
    private Long projectId;

}
