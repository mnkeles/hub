package etiya.omniAutomation.repository;

import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.entity.PerfRsltItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PerformanceResultItemRepository extends JpaRepository<PerfRsltItemEntity, Long> {

    @Modifying
    @Query("""
            update PerfRsltItemEntity prs set prs.performanceThreadGroup = :performanceThreadGroup where prs.perfRsltId = :performanceResultId
            """)
    void updatePerformanceResults(Long performanceResultId, PerformanceThreadGroup performanceThreadGroup);

    PerfRsltItemEntity findByPerfRsltId(Long perfRsltId);
}
