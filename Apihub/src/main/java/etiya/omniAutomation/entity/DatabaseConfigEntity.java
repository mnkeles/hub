package etiya.omniAutomation.entity;

import lombok.*;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "database_config")
public class DatabaseConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "database_config_seq")
    @SequenceGenerator(name = "database_config_seq", sequenceName = "database_config_db_config_id_seq", allocationSize = 1)
    @Column(name = "db_config_id")
    private Long dbConfigId;

    @Column(name = "short_code")
    private String shortCode;

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "is_actv")
    private boolean isActv;

    @Column(name = "schema")
    private String schema;

    @Column(name = "driver")
    private String driver;

    @Column(name = "ssh_enabled")
    private Boolean sshEnabled;

    @Column(name = "ssh_host")
    private String sshHost;

    @Column(name = "ssh_port")
    private Integer sshPort;

    @Column(name = "ssh_user")
    private String sshUser;

    @Column(name = "ssh_password")
    private String sshPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    public ProjectEntity projectEntity;

    @Column(name = "project_id")
    public Long projectId;
}
