package etiya.omniAutomation.service;

import etiya.omniAutomation.results.ErrorResult;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.results.SuccessResult;
import etiya.omniAutomation.repository.ApiLogRepository;
import etiya.omniAutomation.entity.ApiLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiLogServiceImpl {

    private ApiLogRepository apiLogRepository;

    @Autowired
    public ApiLogServiceImpl(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }

    public Result save(ApiLogEntity apiLogEntity) {
        try{
            this.apiLogRepository.save(apiLogEntity);
            return new SuccessResult();
        }catch (Exception e){
            return new ErrorResult(e.getMessage());
        }
    }

    public Result saveAll(List<ApiLogEntity> apiCallLogEntities){
        try{
            this.apiLogRepository.saveAll(apiCallLogEntities);
            return new SuccessResult();
        }catch (Exception e){
            return new ErrorResult(e.getMessage());
        }
    }

    public List<ApiLogEntity> getAll() {
        return this.apiLogRepository.findAll();
    }
}
