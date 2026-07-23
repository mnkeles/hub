package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.PerformanceScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface PerformanceScheduleRepository extends JpaRepository<PerformanceScheduleEntity, Long> {

    List<PerformanceScheduleEntity> findByProjectIdAndProcessFlowIdOrderByCreatedAtDesc(Long projectId, Long processFlowId);

    List<PerformanceScheduleEntity> findByEnabledTrueAndNextRunAtLessThanEqual(Date now);
}
