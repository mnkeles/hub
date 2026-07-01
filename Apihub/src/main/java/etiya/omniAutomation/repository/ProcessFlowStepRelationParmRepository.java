package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProcessFlowStepRelationParmEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProcessFlowStepRelationParmRepository extends JpaRepository<ProcessFlowStepRelationParmEntity, Integer> { 
}
