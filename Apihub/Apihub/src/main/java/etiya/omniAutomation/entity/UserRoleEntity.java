package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_role")
public class UserRoleEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    @ManyToOne()
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserEntity userEntity;

    @ManyToOne()
    @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    private RoleEntity roleEntity;

    @ManyToOne()
    @JoinColumn(name = "proc_flow_id", referencedColumnName = "proc_flow_id")
    private ProcessFlowEntity processFlowEntity;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate = new Date();
}
