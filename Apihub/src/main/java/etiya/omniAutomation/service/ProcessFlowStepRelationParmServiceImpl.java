package etiya.omniAutomation.service;

import etiya.omniAutomation.entity.ProcessFlowStepRelationParmEntity;
import etiya.omniAutomation.repository.ProcessFlowStepRelationParmRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProcessFlowStepRelationParmServiceImpl {

    private ProcessFlowStepRelationParmRepository processFlowStepRelationParmRepository;

    public ProcessFlowStepRelationParmServiceImpl(ProcessFlowStepRelationParmRepository processFlowStepRelationParmRepository) {
        this.processFlowStepRelationParmRepository = processFlowStepRelationParmRepository;
    }

    public List<ProcessFlowStepRelationParmEntity> getProcessFlowStepRelationParmByProcessFlowStepId(long processFlowStepId) {
        return null;
    }
}
