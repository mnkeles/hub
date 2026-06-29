package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.GnlWebSysEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GeneralWebSystemRepository extends JpaRepository<GnlWebSysEntity, Long> {

    @Query("select count(p.gnlWebSysId) from GnlWebSysEntity p where p.projectId = :projectId")
    long countByProjectId(Long projectId);

}
