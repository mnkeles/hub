package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.PerfRsltEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PerformanceResultRepository extends JpaRepository<PerfRsltEntity, Long> {

    @Query("select count(p.perfRsltId) from PerfRsltEntity p where p.projectId = :projectId")
    long countByProjectId(Long projectId);

    List<PerfRsltEntity> findByProjectIdAndProcessFlowId(Long projectId, Long processFlowId);

    List<PerfRsltEntity> findByProjectIdAndProcessFlowIdOrderByCreatedAtDesc(Long projectId, Long processFlowId);

    Optional<PerfRsltEntity> findFirstByProjectIdAndProcessFlowIdAndBaselineTrue(Long projectId, Long processFlowId);

    List<PerfRsltEntity> findByProjectIdAndProcessFlowIdAndBaselineTrue(Long projectId, Long processFlowId);
}
