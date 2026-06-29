package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ProcessFlowDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepParmDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepRelationDto;
import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.entity.*;
import etiya.omniAutomation.mappers.ProcessFlowMapper;
import etiya.omniAutomation.mappers.ProcessFlowStepMapper;
import etiya.omniAutomation.mappers.ProcessFlowStepParmMapper;
import etiya.omniAutomation.repository.*;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessFlowServiceImpl {

    private final ProcessFlowRepository processFlowRepository;
    private final ProcessFlowStepParmRepository processFlowStepParmRepository;
    private final ProcessFlowStepRepository processFlowStepRepository;
    private final ProcessFlowStepRelationRepository processFlowStepRelationRepository;
    private final ProcessFlowStepService processFlowStepService;
    private final ProjectService projectService;
    @PersistenceContext
    private final EntityManager entityManager;

    @Cacheable(value = "processFlowCache", key = "T(java.lang.String).valueOf(#processFlowId)")
    public ProcessFlowDto getByProcessFlowId(long processFlowId) {
        try {
            ProcessFlowEntity processFlowEntity = this.processFlowRepository.getByProcessFlowId(processFlowId);
            if (processFlowEntity == null) {
                throw new Exception(ResultMessage.IS_PROCESS_FLOW_ID_CHECK.toString());
            } else {
                return ProcessFlowMapper.INSTANCE.toDto(processFlowEntity);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public List<ProcessFlowDto> getAll(GeneralPageRequest pageRequest) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProcessFlowEntity> query = criteriaBuilder.createQuery(ProcessFlowEntity.class);
        Root<ProcessFlowEntity> root = query.from(ProcessFlowEntity.class);
        query.select(root);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("processFlowId")));

        pageRequest.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROJECT_ID -> {
                    ProjectDto project = this.projectService.getProject(filter.getValue());
                    predicates.add(criteriaBuilder.equal(root.get("projectId"), project.getProjectId()));
                }
                case PROCESS_FLOW_ID -> predicates.add(criteriaBuilder.equal(root.get("processFlowId"), filter.getNumberValue()));
            }
        });

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(criteriaBuilder.asc(root.get("processFlowId")));
        List<ProcessFlowEntity> resultList = entityManager.createQuery(query)
                .setFirstResult(pageRequest.getOffset())
                .setMaxResults(pageRequest.getLimit())
                .getResultList();
        ProcessFlowMapper mapper = ProcessFlowMapper.INSTANCE;
        return resultList.stream()
                .map(mapper::toDtoWithoutRelations)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "processFlowCache", allEntries = true)
    public Result save(ProcessFlowDto processFlowDto) {
        ProcessFlowMapper mapper = ProcessFlowMapper.INSTANCE;
        ProcessFlowEntity entity = mapper.toEntity(processFlowDto);
        ProcessFlowEntity saved = this.processFlowRepository.save(entity);
        return new SuccessDataResult<>(saved.getProcessFlowId(), "Akış başarıyla kaydedildi");
    }

    @Transactional
    @CacheEvict(value = "processFlowCache", allEntries = true)
    public Result deleteProcessFlow(Long processFlowId) {
        List<ProcessFlowStepEntity> steps = this.processFlowStepRepository
            .findByProcessFlowIdOrderByStepOrderAsc(processFlowId);

        for (ProcessFlowStepEntity step : steps) {
            this.processFlowStepService.delete(step.getProcessFlowStepId());
        }

        this.processFlowRepository.deleteById(processFlowId);
        return new SuccessResult("Akış başarıyla silindi");
    }

    @Transactional
    @CacheEvict(value = "processFlowCache", allEntries = true)
    public Result updateProcessFlowStepParameter(ProcessFlowStepParmDto processFlowStepParmDto) {
        ProcessFlowStepParmEntity entity = ProcessFlowStepParmMapper.INSTANCE.toEntity(processFlowStepParmDto);
        this.processFlowStepParmRepository.save(entity);
        return new SuccessResult();
    }

    @Transactional
    @CacheEvict(value = "processFlowCache", allEntries = true)
    public Result update(ProcessFlowDto processFlowDto) {
        ProcessFlowEntity entity = ProcessFlowMapper.INSTANCE.toEntity(processFlowDto);
        this.processFlowRepository.save(entity);
        return new SuccessResult();
    }

    public long count(GeneralPageRequest generalPageRequest) {
        if (generalPageRequest.getFilterList() == null || generalPageRequest.getFilterList().isEmpty()) {
            return this.processFlowRepository.count();
        }
        String value = generalPageRequest.getFilterList().getFirst().getValue();
        ProjectDto project = this.projectService.getProject(value);
        return this.processFlowRepository.countByProjectId(project.getProjectId());
    }

    @Cacheable(value = "processFlowCache", key = "T(java.lang.String).valueOf(#processFlowId)")
    public ProcessFlowDto findByIdWithRelations(long processFlowId) {
        ProcessFlowEntity processFlowEntity = this.processFlowRepository.getByProcessFlowId(processFlowId);
        ProcessFlowDto dto = ProcessFlowMapper.INSTANCE.toDtoWithoutRelations(processFlowEntity);
        
        // Fetch all steps for this flow
        List<ProcessFlowStepEntity> steps = processFlowStepRepository.findByProcessFlowIdOrderByStepOrderAsc(processFlowId);
        
        // For each step, create a relation DTO with its parameters
        List<ProcessFlowStepRelationDto> stepRelations = new ArrayList<>();
        ProcessFlowStepMapper processFlowStepMapper = ProcessFlowStepMapper.INSTANCE;
        ProcessFlowStepParmMapper processFlowStepParmMapper = ProcessFlowStepParmMapper.INSTANCE;

        for (ProcessFlowStepEntity step : steps) {
            // Fetch all parameter relations for this step
            List<ProcessFlowStepRelationEntity> relations = processFlowStepRelationRepository
                .findAllByProcessFlowStepIdWithParameters(step.getProcessFlowStepId());
            
            // Create a ProcessFlowStepDto with step information
            ProcessFlowStepDto stepDto = processFlowStepMapper.toDto(step);
            
            // Create a single relation DTO for this step with all its parameters
            ProcessFlowStepRelationDto stepRelation = new ProcessFlowStepRelationDto();
            stepRelation.setProcessFlowStepId(step.getProcessFlowStepId());
            stepRelation.setProjectId(processFlowEntity.getProjectId());
            stepRelation.setProcessFlowStep(stepDto);
            
            // Collect all parameters for this step
            List<ProcessFlowStepParmDto> parameters = new ArrayList<>();
            for (ProcessFlowStepRelationEntity relation : relations) {
                if (relation.getProcessFlowStepParm() != null) {
                    ProcessFlowStepParmDto parmDto = processFlowStepParmMapper.toDto(relation.getProcessFlowStepParm());
                    parameters.add(parmDto);
                }
            }
            
            stepRelation.setProcessFlowStepParms(parameters);
            stepRelations.add(stepRelation);
        }
        
        dto.setProcessFlowStepRelations(stepRelations);
        return dto;
    }

    @Cacheable(value = "processFlowCache", key = "'project_' + T(java.lang.String).valueOf(#projectId)")
    public List<ProcessFlowDto> getFlowsByProject(Long projectId) {
        List<ProcessFlowEntity> allByProjectId = this.processFlowRepository.findAllByProjectId(projectId);
        return allByProjectId.stream().map(ProcessFlowMapper.INSTANCE::toDtoWithoutRelations).toList();
    }

    private String generateUniqueShortCode(String baseShortCode, Long projectId) {
        // Eğer baseShortCode zaten _copy ile bitiyorsa, onu kaldır
        String cleanBase = baseShortCode.replaceAll("_copy(_\\d+)?$", "");
        
        // Aynı base ile başlayan tüm akışları bul
        String newShortCode = cleanBase + "_copy";
        int counter = 1;
        
        while (processFlowRepository.existsByShortCodeAndProjectId(newShortCode, projectId)) {
            counter++;
            newShortCode = cleanBase + "_copy_" + counter;
        }
        
        return newShortCode;
    }

    @Transactional
    @CacheEvict(value = "processFlowCache", allEntries = true)
    public Result copyProcessFlow(Long processFlowId) {
        try {
            // 1. Eski akışı bul
            ProcessFlowEntity oldFlow = processFlowRepository.findById(processFlowId)
                .orElseThrow(() -> new RuntimeException("Akış bulunamadı: " + processFlowId));
            
            // 2. Unique shortCode oluştur
            String baseShortCode = oldFlow.getShortCode();
            String newShortCode = generateUniqueShortCode(baseShortCode, oldFlow.getProjectId());
            
            // 3. Yeni akış oluştur
            ProcessFlowEntity newFlow = new ProcessFlowEntity();
            newFlow.setShortCode(newShortCode);
            newFlow.setActive(oldFlow.isActive());
            newFlow.setProjectId(oldFlow.getProjectId());
            newFlow = processFlowRepository.save(newFlow);
            
            // 3. Adımları çek
            List<ProcessFlowStepEntity> oldSteps = processFlowStepRepository
                .findByProcessFlowIdOrderByStepOrderAsc(processFlowId);
            
            // 4. Her adımı kopyala
            for (ProcessFlowStepEntity oldStep : oldSteps) {
                ProcessFlowStepEntity newStep = new ProcessFlowStepEntity();
                newStep.setProcessFlowId(newFlow.getProcessFlowId());
                newStep.setStepShortCode(oldStep.getStepShortCode());
                newStep.setStepOrder(oldStep.getStepOrder());
                newStep.setGnlApiInformationId(oldStep.getGnlApiInformationId());
                newStep.setPlIn(oldStep.getPlIn());
                newStep.setHeaderExtractor(oldStep.getHeaderExtractor());
                newStep.setParameterExtractor(oldStep.getParameterExtractor());
                newStep.setPreHeader(oldStep.getPreHeader());
                newStep = processFlowStepRepository.save(newStep);
                
                // 5. Bu adımın relation'larını ve parametrelerini kopyala
                List<ProcessFlowStepRelationEntity> oldRelations = 
                    processFlowStepRelationRepository.findAllByProcessFlowStepIdWithParameters(oldStep.getProcessFlowStepId());
                
                for (ProcessFlowStepRelationEntity oldRelation : oldRelations) {
                    // Relation'ı kopyala
                    ProcessFlowStepRelationEntity newRelation = new ProcessFlowStepRelationEntity();
                    newRelation.setProcessFlowStepId(newStep.getProcessFlowStepId());
                    newRelation.setProjectId(oldRelation.getProjectId());
                    
                    // Eğer parametresi varsa, önce parametreyi kopyala
                    if (oldRelation.getProcessFlowStepParm() != null) {
                        ProcessFlowStepParmEntity oldParam = oldRelation.getProcessFlowStepParm();
                        ProcessFlowStepParmEntity newParam = new ProcessFlowStepParmEntity();
                        newParam.setShortCode(oldParam.getShortCode());
                        newParam.setValue(oldParam.getValue());
                        newParam.setValExpression(oldParam.getValExpression());
                        newParam.setSql(oldParam.getSql());
                        newParam.setCode(oldParam.getCode());
                        newParam.setUseContext(oldParam.isUseContext());
                        newParam.setParamOrder(oldParam.getParamOrder());
                        newParam = processFlowStepParmRepository.save(newParam);
                        
                        newRelation.setProcessFlowStepParmId(newParam.getProcessFlowStepParmId());
                    }
                    
                    processFlowStepRelationRepository.save(newRelation);
                }
            }
            
            return new SuccessResult("Akış başarıyla kopyalandı. Yeni akış ID: " + newFlow.getProcessFlowId());
            
        } catch (Exception e) {
            return new ErrorResult("Akış kopyalama hatası: " + e.getMessage());
        }
    }

}
