package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProjectEntity;
import etiya.omniAutomation.entity.UserProjectRelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserProjectRelationRepository extends JpaRepository<UserProjectRelEntity, Long> {

    @Query("""  
            select p.projectEntity from UserProjectRelEntity p where p.isActv = true and p.userId = :userId
            """)
    List<ProjectEntity> findUserProjects(Long userId);

    @Query("SELECT DISTINCT r.projectId FROM UserProjectRelEntity r  WHERE r.isActv = true AND r.userId = :userId")
    List<Long> findProjectIdsByEmail(@Param("userId") Long userId);
}
