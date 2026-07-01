package etiya.omniAutomation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "project")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_id_seq")
    @SequenceGenerator(name = "project_id_seq", sequenceName = "project_project_id_seq", allocationSize = 1)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "name")
    private String name;

    @Column(name = "short_code")
    private String shortCode;

    @Column(name = "description")
    private String description;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "projectEntity")
    private List<GnlWebSysEntity> gnlWebSysEntityList;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "projectEntity")
    private List<DatabaseConfigEntity> databaseConfigEntities;
}
