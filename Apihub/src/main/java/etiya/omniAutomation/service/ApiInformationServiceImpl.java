package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ApiInformationDto;
import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.mappers.ApiInformationMapper;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.*;
import etiya.omniAutomation.repository.ApiInformationRepository;
import etiya.omniAutomation.entity.ApiInformationEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiInformationServiceImpl {

    private final ApiInformationRepository apiInformationRepository;
    @PersistenceContext
    private final EntityManager entityManager;
    private final ProjectService projectService;

    public List<ApiInformationDto> getAll(GeneralPageRequest pageRequest) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ApiInformationEntity> query = criteriaBuilder.createQuery(ApiInformationEntity.class);
        Root<ApiInformationEntity> root = query.from(ApiInformationEntity.class);
        query.select(root);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("id")));

        pageRequest.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROJECT_ID -> {
                    ProjectDto project = this.projectService.getProject(filter.getValue());
                    if (project != null) {
                        predicates.add(criteriaBuilder.equal(root.get("projectId"), project.getProjectId()));
                    }
                }
            }
        });
        Order order = criteriaBuilder.asc(root.get("id"));

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(order);
        List<ApiInformationEntity> resultList = entityManager.createQuery(query)
                .setFirstResult(pageRequest.getOffset())
                .setMaxResults(pageRequest.getLimit())
                .getResultList();
        ApiInformationMapper mapper = ApiInformationMapper.INSTANCE;
        return resultList.stream()
                .map(mapper::toDto)
                .toList();
    }

    public long count(GeneralPageRequest generalPageRequest) {
        if (generalPageRequest.getFilterList() == null || generalPageRequest.getFilterList().isEmpty()) {
            return this.apiInformationRepository.count();
        }
        String value = generalPageRequest.getFilterList().getFirst().getValue();
        ProjectDto project = this.projectService.getProject(value);
        if (project == null) {
            return 0;
        }
        return this.apiInformationRepository.countByProjectId(project.getProjectId());
    }

    @Transactional
    public Result save(ApiInformationDto apiInformationDto) {
        ApiInformationMapper mapper = ApiInformationMapper.INSTANCE;
        ApiInformationEntity entity = mapper.toEntity(apiInformationDto);
        this.apiInformationRepository.save(entity);
        return new SuccessResult();
    }

    @Transactional
    public Result deleteApiInformation(Long apiInformationId) {
        this.apiInformationRepository.deleteById(apiInformationId);
        return new SuccessResult();
    }

    public List<ApiInformationDto> findAllByProjectShortCode(String projectShortCode) {
        ProjectDto project = this.projectService.getProject(projectShortCode);
        if (project == null) {
            return new ArrayList<>();
        }
        List<ApiInformationEntity> entities = this.apiInformationRepository.findByProjectIdAndIsActive(project.getProjectId(), 1);

        ApiInformationMapper mapper = ApiInformationMapper.INSTANCE;
        return mapper.toDtoList(entities);
    }
}
