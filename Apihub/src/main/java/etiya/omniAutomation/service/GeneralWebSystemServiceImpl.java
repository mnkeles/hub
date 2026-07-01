package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ApiInformationDto;
import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.business.dto.ProcessFlowDto;
import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.entity.ApiInformationEntity;
import etiya.omniAutomation.entity.GnlWebSysEntity;
import etiya.omniAutomation.entity.ProcessFlowEntity;
import etiya.omniAutomation.mappers.ApiInformationMapper;
import etiya.omniAutomation.mappers.GenaralWebSystemMapper;
import etiya.omniAutomation.mappers.ProcessFlowMapper;
import etiya.omniAutomation.repository.GeneralWebSystemRepository;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.results.SuccessResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneralWebSystemServiceImpl {
    private final GeneralWebSystemRepository generalWebSystemRepository;
    private final EntityManager entityManager;
    private final ProjectService projectService;

    public List<GeneralWebSystemDto> getAll(GeneralPageRequest pageRequest) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<GnlWebSysEntity> query = criteriaBuilder.createQuery(GnlWebSysEntity.class);
        Root<GnlWebSysEntity> root = query.from(GnlWebSysEntity.class);
        query.select(root);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("gnlWebSysId")));

        pageRequest.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROJECT_ID -> {
                    ProjectDto project = this.projectService.getProject(filter.getValue());
                    predicates.add(criteriaBuilder.equal(root.get("projectId"), project.getProjectId()));
                }
            }
        });
        Order order = criteriaBuilder.asc(root.get("gnlWebSysId"));

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(order);
        List<GnlWebSysEntity> resultList = entityManager.createQuery(query)
                .setFirstResult(pageRequest.getOffset())
                .setMaxResults(pageRequest.getLimit())
                .getResultList();
        GenaralWebSystemMapper mapper = GenaralWebSystemMapper.INSTANCE;
        return resultList.stream()
                .map(mapper::toDto)
                .toList();
    }
    public long count(GeneralPageRequest generalPageRequest) {
        // Eğer filter listesi boş ise, tüm kayıtları say
        if (generalPageRequest.getFilterList() == null || generalPageRequest.getFilterList().isEmpty()) {
            return this.generalWebSystemRepository.count();
        }
        
        String value = generalPageRequest.getFilterList().getFirst().getValue();
        ProjectDto project = this.projectService.getProject(value);
        return this.generalWebSystemRepository.countByProjectId(project.getProjectId());
    }

    @Transactional
    public Result save(GeneralWebSystemDto generalWebSystemDto) {
        GenaralWebSystemMapper mapper = GenaralWebSystemMapper.INSTANCE;
        GnlWebSysEntity entity = mapper.toEntity(generalWebSystemDto);
        this.generalWebSystemRepository.save(entity);
        return new SuccessResult();
    }

    @Transactional
    public Result delete(Long gnlWebSysId) {
        this.generalWebSystemRepository.deleteById(gnlWebSysId);
        return new SuccessResult();
    }
}
