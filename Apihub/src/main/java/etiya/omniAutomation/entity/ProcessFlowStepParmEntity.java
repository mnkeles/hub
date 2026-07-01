package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proc_flow_step_parm")
public class ProcessFlowStepParmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proc_flow_step_parm_id_seq")
    @SequenceGenerator(name = "proc_flow_step_parm_id_seq", sequenceName = "proc_flow_step_parm_proc_flow_step_parm_id_seq", allocationSize = 1)
    @Column(name = "proc_flow_step_parm_id")
    private Long processFlowStepParmId;

    @Column(name = "shrt_code")
    private String shortCode;

    @Column(name = "val")
    private String value;

    @Column(name = "val_expr")
    private String valExpression;

    @Column(name = "param_order")
    private Integer paramOrder;

    @Column(name = "use_context")
    private boolean useContext;

    @Column(name = "sql")
    private String sql;

    @Column(name = "code")
    private String code;

}
