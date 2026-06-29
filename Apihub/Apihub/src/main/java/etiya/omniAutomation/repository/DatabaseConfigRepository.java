package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.DatabaseConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfigEntity, Long> {

    DatabaseConfigEntity findByShortCodeAndProjectIdAndIsActv(String shortCode,Long projectId, boolean isActive);
    @Query("select count(p.dbConfigId) from DatabaseConfigEntity p where p.projectId = :projectId")
    long countByProjectId(Long projectId);


}
