package etiya.omniAutomation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_account_permission")
public class ServiceAccountPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "service_account_id", referencedColumnName = "service_account_id")
    private ServiceAccountEntity serviceAccountEntity;

    @ManyToOne
    @JoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    private PermissionEntity permissionEntity;

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private ProjectEntity projectEntity;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate = new Date();
}
