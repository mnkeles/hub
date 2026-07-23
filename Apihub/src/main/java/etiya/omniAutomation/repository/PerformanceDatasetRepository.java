package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.PerformanceDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceDatasetRepository extends JpaRepository<PerformanceDatasetEntity, Long> {

    List<PerformanceDatasetEntity> findByProjectIdAndActiveTrueOrderByUpdatedAtDesc(Long projectId);
}
