package etiya.omniAutomation.service;

import etiya.omniAutomation.entity.ProcessFlowInstanceEntity;
import etiya.omniAutomation.repository.ProcessFlowInstanceRepository;
import etiya.omniAutomation.results.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessFlowInstanceServiceImpl {

    private ProcessFlowInstanceRepository processFlowInstanceRepository;

    @Autowired
    public ProcessFlowInstanceServiceImpl(ProcessFlowInstanceRepository processFlowInstanceRepository) {
        this.processFlowInstanceRepository = processFlowInstanceRepository;
    }

    public Result saveAll(List<ProcessFlowInstanceEntity> processFlowInstanceEntities) {
        try{
            this.processFlowInstanceRepository.saveAll(processFlowInstanceEntities);
            return new SuccessResult();
        }catch (Exception e){
            return new ErrorResult(e.getMessage());
        }
    }

    public DataResult<List<ProcessFlowInstanceEntity>> getProcessFlowInstanceByOrderByCdate() {
        List<ProcessFlowInstanceEntity> processFlowInstanceEntities = this.processFlowInstanceRepository.getProcessFlowInstanceByOrderByCdate();
        return new SuccessDataResult(processFlowInstanceEntities);
    }
}
