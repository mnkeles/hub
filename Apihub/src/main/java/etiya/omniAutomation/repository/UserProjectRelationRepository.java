package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProjectEntity;
import etiya.omniAutomation.entity.UserProjectRelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserProjectRelationRepository extends JpaRepository<UserProjectRelEntity, Long> {

    @Query("""  
            select p.projectEntity from UserProjectRelEntity p where p.isActv = true and p.userId = :userId
            """)
    List<ProjectEntity> findUserProjects(Long userId);
}
