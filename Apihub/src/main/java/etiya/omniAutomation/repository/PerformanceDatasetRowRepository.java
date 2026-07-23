package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.PerformanceDatasetRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceDatasetRowRepository extends JpaRepository<PerformanceDatasetRowEntity, Long> {

    List<PerformanceDatasetRowEntity> findByDatasetIdAndActiveTrueOrderByRowIndexAsc(Long datasetId);

    List<PerformanceDatasetRowEntity> findTop20ByDatasetIdAndActiveTrueOrderByRowIndexAsc(Long datasetId);

    long countByDatasetIdAndActiveTrue(Long datasetId);
}
