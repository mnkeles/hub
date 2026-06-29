package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ProcessFlowStepDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepParmDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepRelationDto;
import etiya.omniAutomation.entity.ProcessFlowEntity;
import etiya.omniAutomation.entity.ProcessFlowStepEntity;
import etiya.omniAutomation.entity.ProcessFlowStepParmEntity;
import etiya.omniAutomation.entity.ProcessFlowStepRelationEntity;
import etiya.omniAutomation.mappers.ProcessFlowStepMapper;
import etiya.omniAutomation.mappers.ProcessFlowStepParmMapper;
import etiya.omniAutomation.mappers.ProcessFlowStepRelationMapper;
import etiya.omniAutomation.repository.ProcessFlowRepository;
import etiya.omniAutomation.repository.ProcessFlowStepParmRepository;
import etiya.omniAutomation.repository.ProcessFlowStepRelationRepository;
import etiya.omniAutomation.repository.ProcessFlowStepRepository;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.results.SuccessDataResult;
import etiya.omniAutomation.results.SuccessResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessFlowStepService {

    private final ProcessFlowStepRepository processFlowStepRepository;
    @PersistenceContext
    private final EntityManager entityManager;
    private final ProcessFlowStepRelationRepository processFlowStepRelationRepository;
    private final ProcessFlowStepParmRepository processFlowStepParmRepository;
    private final ProcessFlowRepository processFlowRepository;

    public ProcessFlowStepEntity inquireProcessStepByStepShortCode(String stepShortCode, List<Long> flowIds) {
        return processFlowStepRepository.findByStepShortCodeAndProcessFlowIdIn(stepShortCode, flowIds);
    }

    public List<ProcessFlowStepDto> getProcessFlowSteps(GeneralPageRequest generalPageRequest) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProcessFlowStepEntity> query = criteriaBuilder.createQuery(ProcessFlowStepEntity.class);
        Root<ProcessFlowStepEntity> root = query.from(ProcessFlowStepEntity.class);
        
        // JOIN FETCH ile relation'ları ve parameter'ları tek sorguda çek
        root.fetch("processFlowStepRelationEntities", JoinType.LEFT)
            .fetch("processFlowStepParm", JoinType.LEFT);
        root.fetch("apiInformationEntity", JoinType.LEFT);
        
        query.select(root).distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("processFlowStepId")));

        generalPageRequest.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROCESS_FLOW_ID -> predicates.add(criteriaBuilder.equal(root.get("processFlowId"), filter.getNumberValue()));
            }
        });

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(criteriaBuilder.asc(root.get("processFlowStepId")), criteriaBuilder.asc(root.get("stepOrder")));
        
        List<ProcessFlowStepEntity> resultList = entityManager.createQuery(query)
                .setFirstResult(generalPageRequest.getOffset())
                .setMaxResults(generalPageRequest.getLimit())
                .getResultList();
        
        ProcessFlowStepMapper mapper = ProcessFlowStepMapper.INSTANCE;
        return resultList.stream()
                .map(mapper::toDto)
                .toList();
    }

    public long count(GeneralPageRequest generalPageRequest) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<ProcessFlowStepEntity> root = query.from(ProcessFlowStepEntity.class);
        query.select(criteriaBuilder.count(root));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("processFlowStepId")));

        generalPageRequest.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROCESS_FLOW_ID -> predicates.add(criteriaBuilder.equal(root.get("processFlowId"), filter.getNumberValue()));
            }
        });
        query.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Transactional
    public Result update(ProcessFlowStepDto processFlowStepDto) {
        ProcessFlowStepMapper mapper = ProcessFlowStepMapper.INSTANCE;
        ProcessFlowStepEntity entity = mapper.toEntity(processFlowStepDto);
        
        // Step'i güncelle
        ProcessFlowStepEntity updatedStep = this.processFlowStepRepository.save(entity);
        
        // ProcessFlow'dan projectId'yi al
        ProcessFlowEntity flow = this.processFlowRepository.findById(updatedStep.getProcessFlowId())
            .orElseThrow(() -> new RuntimeException(
                "Process Flow not found with ID: " + updatedStep.getProcessFlowId()));
        
        // Eski relation'ları ve parameter'ları tek sorguda sil
        this.processFlowStepRelationRepository.deleteRelationsAndParametersNative(
            updatedStep.getProcessFlowStepId()
        );
        
        // Hibernate cache'i agresif şekilde temizle
        entityManager.flush();
        entityManager.clear();
        
        // Yeni parametreleri kaydet (eğer varsa)
        if (processFlowStepDto.getProcessFlowStepParmList() != null && 
            !processFlowStepDto.getProcessFlowStepParmList().isEmpty()) {
            
            for (ProcessFlowStepParmDto parmDto : processFlowStepDto.getProcessFlowStepParmList()) {
                ProcessFlowStepParmEntity parmEntity = new ProcessFlowStepParmEntity();
                parmEntity.setShortCode(parmDto.getShortCode());
                parmEntity.setValue(parmDto.getValue());
                parmEntity.setValExpression(parmDto.getValExpression());
                parmEntity.setParamOrder(parmDto.getParamOrder());
                parmEntity.setUseContext(parmDto.isUseContext());
                parmEntity.setSql(parmDto.getSql());
                parmEntity.setCode(parmDto.getCode());
                
                ProcessFlowStepParmEntity savedParm = this.processFlowStepParmRepository.save(parmEntity);
                
                ProcessFlowStepRelationDto relationDto = new ProcessFlowStepRelationDto();
                relationDto.setProcessFlowStepId(updatedStep.getProcessFlowStepId());
                relationDto.setProjectId(flow.getProjectId());
                relationDto.setProcessFlowStepParmId(savedParm.getProcessFlowStepParmId());
                
                ProcessFlowStepRelationEntity relationEntity = ProcessFlowStepRelationMapper.INSTANCE.toEntity(relationDto);
                this.processFlowStepRelationRepository.save(relationEntity);
            }
        }
        
        return new SuccessDataResult<>(updatedStep.getProcessFlowStepId(), "Step ve parametreler başarıyla güncellendi");
    }

    @Transactional
    public void delete(Long processFlowStepId) {
        this.processFlowStepRelationRepository.deleteRelationsAndParametersNative(processFlowStepId);
        entityManager.flush();
        entityManager.clear();
        this.processFlowStepRepository.deleteById(processFlowStepId);
    }

    @Transactional
    public void deleteProcessFlowStepParameter(Long relationId) {
        this.processFlowStepRelationRepository.deleteById(relationId);
    }

    @Transactional
    public List<ProcessFlowStepRelationDto> getProcessFlowStepRelation(Long processFlowStepId, Long projectId) {
        List<ProcessFlowStepRelationEntity> entities = this.processFlowStepRelationRepository.findAllByProcessFlowStepIdAndProjectId(processFlowStepId, projectId);
        return ProcessFlowStepRelationMapper.INSTANCE.toDtoList(entities);
    }

    @Transactional
    public void createNewProcessStep(ProcessFlowStepRelationDto relationDto) {
        ProcessFlowStepParmDto processFlowStepParameters = relationDto.getProcessFlowStepParameters();
        
        ProcessFlowStepParmEntity parmEntity = new ProcessFlowStepParmEntity();
        parmEntity.setShortCode(processFlowStepParameters.getShortCode());
        parmEntity.setValue(processFlowStepParameters.getValue());
        parmEntity.setValExpression(processFlowStepParameters.getValExpression());
        parmEntity.setParamOrder(processFlowStepParameters.getParamOrder());
        parmEntity.setUseContext(processFlowStepParameters.isUseContext());
        parmEntity.setSql(processFlowStepParameters.getSql());
        parmEntity.setCode(processFlowStepParameters.getCode());
        
        this.processFlowStepParmRepository.save(parmEntity);
        relationDto.setProcessFlowStepParmId(parmEntity.getProcessFlowStepParmId());
        ProcessFlowStepRelationEntity relationEntity = ProcessFlowStepRelationMapper.INSTANCE.toEntity(relationDto);
        this.processFlowStepRelationRepository.save(relationEntity);
        relationDto.setProcessFlowStepRelationId(relationEntity.getProcessFlowStepRelationId());
    }
    @Transactional
    public Result save(ProcessFlowStepDto processFlowStepDto) {
        ProcessFlowStepMapper mapper = ProcessFlowStepMapper.INSTANCE;
        ProcessFlowStepEntity entity = mapper.toEntity(processFlowStepDto);

        ProcessFlowStepEntity savedStep = this.processFlowStepRepository.save(entity);

        if (processFlowStepDto.getProcessFlowStepParmList() != null && 
            !processFlowStepDto.getProcessFlowStepParmList().isEmpty()) {

            ProcessFlowEntity flow = this.processFlowRepository.findById(savedStep.getProcessFlowId())
                .orElseThrow(() -> new RuntimeException(
                    "Process Flow not found with ID: " + savedStep.getProcessFlowId()));

            for (ProcessFlowStepParmDto parmDto : processFlowStepDto.getProcessFlowStepParmList()) {
                ProcessFlowStepRelationDto relationDto = new ProcessFlowStepRelationDto();
                relationDto.setProcessFlowStepId(savedStep.getProcessFlowStepId());
                relationDto.setProjectId(flow.getProjectId());
                relationDto.setProcessFlowStepParameters(parmDto);
                
                this.createNewProcessStep(relationDto);
            }
        }

        return new SuccessDataResult<>(savedStep.getProcessFlowStepId(), "Step ve parametreler başarıyla kaydedildi");
    }

    @Transactional
    public Result updateStepOrders(List<ProcessFlowStepDto> steps) {
        try {
            for (ProcessFlowStepDto step : steps) {
                ProcessFlowStepEntity entity = processFlowStepRepository.findById(step.getProcessFlowStepId())
                    .orElseThrow(() -> new RuntimeException("Step not found: " + step.getProcessFlowStepId()));
                
                entity.setStepOrder(step.getStepOrder());
                processFlowStepRepository.save(entity);
            }
            return new SuccessResult("Sıralama güncellendi");
        } catch (Exception e) {
            throw new RuntimeException("Sıralama güncellenemedi: " + e.getMessage());
        }
    }

}
