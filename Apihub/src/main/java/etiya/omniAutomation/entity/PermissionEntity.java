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
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "permission_key", nullable = false, unique = true)
    private String permissionKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "ui_visible")
    private Boolean uiVisible = true;

    @Column(name = "service_assignable")
    private Boolean serviceAssignable = false;

    @Column(name = "enabled")
    private int enabled = 1;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdate")
    private Date cdate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udate")
    private Date udate = new Date();
}
