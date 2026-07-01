package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.*;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.DefaultRequestEntity;
import etiya.omniAutomation.entity.PerfRsltEntity;
import etiya.omniAutomation.entity.PerfRsltItemEntity;
import etiya.omniAutomation.entity.ProjectEntity;
import etiya.omniAutomation.mappers.PerformanceResultMapper;
import etiya.omniAutomation.repository.DefaultRequestRepository;
import etiya.omniAutomation.repository.PerformanceResultItemRepository;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import etiya.omniAutomation.repository.ProjectRepository;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.request.PerformanceRequest;
import etiya.omniAutomation.request.PerformanceValidationNoteRequest;
import etiya.omniAutomation.results.PerformanceSummaryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceResultRepository performanceResultRepository;
    private final ApiCallServiceImpl apiCallService;
    private final ProcessFlowServiceImpl processFlowService;
    private final ProjectRepository projectRepository;
    private final PerformanceResultItemRepository performanceResultItemRepository;
    private final ProjectService projectService;
    private final PlatformTransactionManager platformTransactionManager;
    @PersistenceContext
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultRequestRepository defaultRequestRepository;
    private final PerformanceRunRegistry performanceRunRegistry;
    private final PerformanceComparisonService performanceComparisonService;
    private final PerformanceExportService performanceExportService;
    private final PerformanceThresholdPresetResolver performanceThresholdPresetResolver;
    private final PerformanceBaselineService performanceBaselineService;
    private final PerformanceValidationChecklistBuilder performanceValidationChecklistBuilder;
    private final PerformanceAiReportRegenerationService performanceAiReportRegenerationService;

    @Transactional
    public PerformanceResultDto executePerformanceTest(PerformanceRequest request) {
        if (Objects.nonNull(request.getProcessFlowId())) {
            return this.executeProcessFlowPerformance(request);
        } else if (Objects.nonNull(request.getProcessFlowStepId())) {
            return this.executeProcessFlowStepPerformance(request);
        } else {
            throw new NotImplementedException("Performance test is not implemented");
        }
    }

    private PerformanceResultDto executeProcessFlowPerformance(PerformanceRequest request) {
        PerfRsltEntity resultEntity = this.createRunningResult(request);
        ProcessFlowDto processFlow = this.processFlowService.findByIdWithRelations(request.getProcessFlowId());
        processFlow.setProjectId(request.getProjectId());
        processFlow.setSystemShortCode(request.getEnvironment());
        ProjectEntity project =  this.projectRepository.findById(request.getProjectId()).get();
        if (project.getShortCode().equals("DARWIN")) {
            DefaultRequestEntity defaultRequest = defaultRequestRepository.findDefaultRequest(project.getShortCode(), request.getEnvironment(), processFlow.getShortCode());
            ParameterRequestDto parameterRequest = new ParameterRequestDto();
            try {
                if (defaultRequest.getParameterContext() != null && !defaultRequest.getParameterContext().trim().isEmpty()) {
                    Map<String, Object> parameterContext = objectMapper.readValue(defaultRequest.getParameterContext(), Map.class);
                    parameterRequest.setParameterContext(parameterContext);
                }
                if (defaultRequest.getGlobalHeaders() != null && !defaultRequest.getGlobalHeaders().trim().isEmpty()) {
                    Map<String, Object> globalHeaders = objectMapper.readValue(defaultRequest.getGlobalHeaders(), Map.class);
                    parameterRequest.setGlobalHeaders(globalHeaders);
                }
                Optional.ofNullable(parameterRequest.getParameterContext())
                        .ifPresent(parameterContext -> parameterContext.forEach((key, value) -> {
                            String stringValue = value != null ? value.toString() : null;
                            processFlow.getParameterContext().put(key, stringValue);
                        }));
                Optional.ofNullable(parameterRequest.getGlobalHeaders())
                        .ifPresent(globalHeaders -> globalHeaders.forEach((key, value) -> {
                            String stringValue = value != null ? value.toString() : null;
                            processFlow.getGlobalHeaders().put(key, stringValue);
                        }));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Default request JSON içeriği parse edilirken hata oluştu", e);
            }
        }

        PerformanceThreadGroup threadGroup = this.createRunningResultItems(processFlow, resultEntity);
        PerformanceResultDto result = PerformanceResultMapper.INSTANCE.toDto(resultEntity);
        result.setThreadGroup(threadGroup);
        result.setDurationSeconds(request.getDurationSeconds());
        result.setLoopCount(request.getLoopCount());
        result.setThinkTimeMs(request.getThinkTimeMs());
        result.setTimeoutMs(request.getTimeoutMs());
        result.setEnvironmentBaseUrl(request.getEnvironmentBaseUrl());
        result.setRunSummary(new PerformanceRunSummary(
                GeneralEnums.PerformanceStatus.RUNNING,
                resultEntity.getCreatedAt(),
                null,
                0,
                request.getThreadCount(),
                request.getRampUpPeriod(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                null
        ));
        CompletableFuture.runAsync(() -> this.apiCallService.executeFlowPerformanceTest(processFlow, result));

        return result;
    }

    private PerformanceResultDto executeProcessFlowStepPerformance(PerformanceRequest request) {
        throw new NotImplementedException("Performance test is not implemented");
    }

    private PerfRsltEntity createRunningResult(PerformanceRequest request) {
        PerfRsltEntity resultEntity = new PerfRsltEntity();
        resultEntity.setThreadCount(request.getThreadCount());
        resultEntity.setRampUpPeriod(request.getRampUpPeriod());
        resultEntity.setDurationSeconds(request.getDurationSeconds());
        resultEntity.setLoopCount(request.getLoopCount());
        resultEntity.setThinkTimeMs(request.getThinkTimeMs());
        resultEntity.setTimeoutMs(request.getTimeoutMs());
        resultEntity.setEnvironmentBaseUrl(request.getEnvironmentBaseUrl());
        resultEntity.setResultSchemaVersion(1);
        resultEntity.setThresholdPreset(Optional.ofNullable(request.getThresholdPreset()).orElse(PerformanceThresholdPreset.NORMAL));
        resultEntity.setThresholdConfig(this.performanceThresholdPresetResolver.resolve(request));
        resultEntity.setBaseline(false);
        resultEntity.setProjectId(request.getProjectId());
        resultEntity.setProcessFlowId(request.getProcessFlowId());
        resultEntity.setPerfStatus(GeneralEnums.PerformanceStatus.RUNNING);
        resultEntity.setCreatedAt(new Date());
        this.performanceResultRepository.save(resultEntity);
        return resultEntity;
    }

    private PerformanceThreadGroup createRunningResultItems(ProcessFlowDto processFlowDto, PerfRsltEntity parent) {
        Integer threadCount = parent.getThreadCount();
        PerfRsltItemEntity itemEntity = new PerfRsltItemEntity();
        itemEntity.setPerfRsltId(parent.getPerfRsltId());

        List<PerformanceThread> threads = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            List<PerformanceResultItemDto> steps = new ArrayList<>(processFlowDto.getProcessFlowStepRelations().size());
            for (ProcessFlowStepRelationDto processFlowStepRelationDto : processFlowDto.getProcessFlowStepRelations()) {
                PerformanceResultItemDto itemDto = new PerformanceResultItemDto();
                itemDto.setProcessFlowStepId(processFlowStepRelationDto.getProcessFlowStepId());
                itemDto.setStepName(processFlowStepRelationDto.getProcessFlowStep().getStepShortCode());
                itemDto.setThreadNumber(i);
                itemDto.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.RUNNING);
                steps.add(itemDto);
            }
            PerformanceThread performanceThread = new PerformanceThread(i, steps);
            threads.add(performanceThread);
        }
        PerformanceThreadGroup performanceThreadGroup = new PerformanceThreadGroup(threads);
        itemEntity.setPerformanceThreadGroup(performanceThreadGroup);

        this.performanceResultItemRepository.save(itemEntity);
        return performanceThreadGroup;
    }

    public List<PerformanceResultDto> getAll(GeneralPageRequest request) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PerfRsltEntity> query = criteriaBuilder.createQuery(PerfRsltEntity.class);
        Root<PerfRsltEntity> root = query.from(PerfRsltEntity.class);
        root.fetch("processFlowEntity");

        query.select(root);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("perfRsltId")));

        request.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROJECT_ID -> {
                    ProjectDto project = this.projectService.getProject(filter.getValue());
                    predicates.add(criteriaBuilder.equal(root.get("projectId"), project.getProjectId()));
                }
                case PROCESS_FLOW_ID -> predicates.add(criteriaBuilder.equal(root.get("processFlowId"), filter.getNumberValue()));
            }
        });
        query.orderBy(criteriaBuilder.desc(root.get("perfRsltId")));

        query.where(predicates.toArray(new Predicate[0]));
        List<PerfRsltEntity> resultList = entityManager.createQuery(query)
                .setFirstResult(request.getOffset())
                .setMaxResults(request.getLimit())
                .getResultList();
        PerformanceResultMapper mapper = PerformanceResultMapper.INSTANCE;
        return resultList.stream()
                .map(mapper::toDto)
                .toList();
    }

    public long count(GeneralPageRequest request) {
        String value = request.getFilterList().getFirst().getValue();
        ProjectDto project = this.projectService.getProject(value);
        return this.performanceResultRepository.countByProjectId(project.getProjectId());
    }

    public List<PerformanceSummaryResult> getHistory(long projectId, long processFlowId) {
        List<PerfRsltEntity> result = this.performanceResultRepository.findByProjectIdAndProcessFlowIdOrderByCreatedAtDesc(projectId, processFlowId);

        return result.stream()
                .map(this::toSummaryResult)
                .toList();
    }

    public PerformanceThreadGroup getDetail(long performanceResultId) {
        PerfRsltItemEntity perfRsltItemEntity = this.performanceResultItemRepository.findByPerfRsltId(performanceResultId);
        return perfRsltItemEntity.getPerformanceThreadGroup();
    }

    public PerformanceExportPayload getAnalysis(long performanceResultId) {
        PerfRsltEntity result = this.performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + performanceResultId));
        return this.performanceExportService.buildPayload(result, loadThreadGroup(performanceResultId));
    }

    public PerformanceLiveSnapshot getLiveSnapshot(long performanceResultId) {
        PerfRsltEntity result = this.performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + performanceResultId));
        int activeFutureCount = this.performanceRunRegistry.find(performanceResultId)
                .map(PerformanceRunRegistry.PerformanceRunControl::activeFutureCount)
                .orElse(0);
        return PerformanceLiveSnapshotBuilder.build(
                performanceResultId,
                result.getPerfStatus(),
                loadThreadGroup(performanceResultId),
                result.getCreatedAt(),
                result.getThreadCount(),
                result.getDurationSeconds(),
                activeFutureCount
        );
    }

    @Transactional
    public PerformanceLiveSnapshot stop(long performanceResultId, boolean force) {
        PerfRsltEntity result = this.performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + performanceResultId));
        if (result.getPerfStatus() == GeneralEnums.PerformanceStatus.RUNNING || result.getPerfStatus() == GeneralEnums.PerformanceStatus.STOPPING) {
            result.setPerfStatus(GeneralEnums.PerformanceStatus.STOPPING);
            this.performanceResultRepository.save(result);
            if (force) {
                this.performanceRunRegistry.requestForceStop(performanceResultId);
            } else {
                this.performanceRunRegistry.requestStop(performanceResultId);
            }
        }
        return this.getLiveSnapshot(performanceResultId);
    }

    public PerformanceComparisonResult compare(long baseResultId, long targetResultId) {
        PerfRsltEntity base = this.performanceResultRepository.findById(baseResultId)
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + baseResultId));
        PerfRsltEntity target = this.performanceResultRepository.findById(targetResultId)
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + targetResultId));
        return this.performanceComparisonService.compare(base, target);
    }

    public PerformanceAiManagementReport regenerateAiReport(Long performanceResultId) {
        return performanceAiReportRegenerationService.regenerate(performanceResultId, loadThreadGroup(performanceResultId));
    }

    @Transactional
    public PerformanceSummaryResult setBaseline(Long performanceResultId) {
        return toSummaryResult(this.performanceBaselineService.setBaseline(performanceResultId));
    }

    @Transactional(readOnly = true)
    public Optional<PerformanceSummaryResult> getBaseline(Long projectId, Long processFlowId) {
        return this.performanceBaselineService.getBaseline(projectId, processFlowId)
                .map(this::toSummaryResult);
    }

    @Transactional
    public PerformanceValidationChecklist updateValidationNote(PerformanceValidationNoteRequest request) {
        if (request == null || request.getPerformanceResultId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "performanceResultId is required.");
        }
        String note = request.getNote();
        if (note != null && note.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation note must be 1000 characters or less.");
        }
        PerfRsltEntity result = this.performanceResultRepository.findById(request.getPerformanceResultId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Performance result not found: " + request.getPerformanceResultId()));
        PerformanceValidationChecklist current = Optional.ofNullable(result.getValidationChecklist())
                .orElseGet(() -> this.performanceValidationChecklistBuilder.build(result, loadThreadGroup(request.getPerformanceResultId())));
        PerformanceValidationChecklist updated = this.performanceValidationChecklistBuilder.withManualNote(current, note, new Date());
        result.setValidationChecklist(updated);
        this.performanceResultRepository.save(result);
        return updated;
    }

    public Object export(long performanceResultId, String format) {
        PerfRsltEntity result = this.performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + performanceResultId));
        PerformanceExportPayload payload = this.performanceExportService.buildPayload(result, loadThreadGroup(performanceResultId));
        String normalizedFormat = format == null ? "json" : format.toLowerCase(Locale.ROOT);
        return switch (normalizedFormat) {
            case "json" -> payload;
            case "csv" -> this.performanceExportService.buildCsv(payload);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        };
    }

    private PerformanceThreadGroup loadThreadGroup(long performanceResultId) {
        PerfRsltItemEntity perfRsltItemEntity = this.performanceResultItemRepository.findByPerfRsltId(performanceResultId);
        if (perfRsltItemEntity == null) {
            return null;
        }
        return perfRsltItemEntity.getPerformanceThreadGroup();
    }

    private PerformanceSummaryResult toSummaryResult(PerfRsltEntity item) {
        return new PerformanceSummaryResult(
                item.getPerfRsltId(),
                item.getPerfStatus(),
                item.getThreadCount(),
                item.getRampUpPeriod(),
                item.getDurationSeconds(),
                item.getLoopCount(),
                item.getThinkTimeMs(),
                item.getTimeoutMs(),
                item.getEnvironmentBaseUrl(),
                item.getResultSchemaVersion(),
                item.getThresholdPreset(),
                item.getThresholdConfig(),
                item.getBaseline(),
                item.getBaselineResultId(),
                item.getBaselineComparison(),
                item.getValidationChecklist(),
                item.getRunSummary(),
                item.getThresholdResult(),
                item.getAnalysisSummary(),
                item.getErrorAnalysis(),
                item.getEnvironmentMetrics(),
                item.getInsightReport(),
                item.getAiManagementReport(),
                item.getSummary(),
                item.getCreatedAt()
        );
    }
}
