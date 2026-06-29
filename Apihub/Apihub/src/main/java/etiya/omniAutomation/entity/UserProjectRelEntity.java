package etiya.omniAutomation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_project_rel")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProjectRelEntity {

    @Id
    @Column(name = "user_project_rel_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_project_rel_id_seq")
    @SequenceGenerator(name = "user_project_rel_id_seq", sequenceName = "user_project_rel_user_project_rel_id_seq", allocationSize = 1)
    private long userProjectRelId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "is_actv")
    private boolean isActv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", insertable = false, updatable = false)
    public UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    public ProjectEntity projectEntity;
}
