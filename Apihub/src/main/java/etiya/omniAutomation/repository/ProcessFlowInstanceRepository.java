package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProcessFlowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProcessFlowInstanceRepository extends JpaRepository<ProcessFlowInstanceEntity, Integer> {

    @Query(value = "FROM ProcessFlowInstanceEntity ORDER BY cdate DESC")
    List<ProcessFlowInstanceEntity> getProcessFlowInstanceByOrderByCdate();
}
