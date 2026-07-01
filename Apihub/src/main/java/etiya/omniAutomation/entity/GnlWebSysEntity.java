package etiya.omniAutomation.entity;

import lombok.*;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "gnl_web_sys")
public class GnlWebSysEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gnl_web_sys_seq")
    @SequenceGenerator(name = "gnl_web_sys_seq", sequenceName = "gnl_web_sys_gnl_web_sys_id_seq", allocationSize = 1)
    @Column(name = "gnl_web_sys_id")
    private Long gnlWebSysId;

    @Column(name = "name")
    private String name;

    @Column(name = "short_code")
    private String shortCode;

    @Column(name = "url")
    private String url;

    @Column(name = "is_actv")
    @Where(clause = "true")
    private boolean isActv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    public ProjectEntity projectEntity;

    @Column(name = "project_id")
    public Long projectId;

    @Column(name = "is_token_url")
    private boolean isTokenUrl;

    @Column(name = "base_url_short_code")
    private String baseUrlShortCode;
}
