package etiya.omniAutomation.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue
    @Column(name = "role_id")
    private long id;

    @Column(name = "short_code")
    private String shortCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate = new Date();

    @OneToMany(mappedBy = "roleEntity")
    @ToString.Exclude()
    private List<UserRoleEntity> userRoles;

    public RoleEntity(String shortCode){
        this.shortCode = shortCode;
    }
}
