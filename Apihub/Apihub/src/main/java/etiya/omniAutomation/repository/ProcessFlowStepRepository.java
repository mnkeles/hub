package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProcessFlowStepEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProcessFlowStepRepository extends JpaRepository<ProcessFlowStepEntity, Long> {

    ProcessFlowStepEntity findByStepShortCodeAndProcessFlowIdIn(String stepShortCode, List<Long> flowIds);

    List<ProcessFlowStepEntity> findByProcessFlowIdOrderByStepOrderAsc(long processFlowId);

    @Query("select count(processFlowStepId) from ProcessFlowStepEntity where processFlowId = ?1")
    long countByProcessFlowId(long processFlowStepId);
}
