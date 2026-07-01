package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proc_flow")
public class ProcessFlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proc_flow_id_seq")
    @SequenceGenerator(name = "proc_flow_id_seq", sequenceName = "proc_flow_id_seq", allocationSize = 1)
    @Column(name = "proc_flow_id")
    private long processFlowId;

    @Column(name = "shrt_code")
    private String shortCode;

    @Column(name = "is_actv")
    private boolean isActive;

    @Column(name = "project_id")
    private long projectId;

    @OneToMany(mappedBy = "processFlowEntity", fetch = FetchType.LAZY)
    @ToString.Exclude()
    @OrderBy("stepOrder ASC ")
    private List<ProcessFlowStepEntity> processFlowStepEntities;

}
