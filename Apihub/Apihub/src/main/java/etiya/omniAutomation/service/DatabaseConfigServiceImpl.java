package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.DatabaseConfigDto;
import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.entity.DatabaseConfigEntity;
import etiya.omniAutomation.entity.GnlWebSysEntity;
import etiya.omniAutomation.mappers.DatabaseConfigMapper;
import etiya.omniAutomation.mappers.GenaralWebSystemMapper;
import etiya.omniAutomation.repository.DatabaseConfigRepository;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.results.SuccessResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseConfigServiceImpl {
    private final DatabaseConfigRepository databaseConfigRepository;
    private final EntityManager entityManager;
    private final ProjectService projectService;

    public List<DatabaseConfigDto> getAll(GeneralPageRequest pageRequest) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DatabaseConfigEntity> query = criteriaBuilder.createQuery(DatabaseConfigEntity.class);
        Root<DatabaseConfigEntity> root = query.from(DatabaseConfigEntity.class);
        query.select(root);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNotNull(root.get("dbConfigId")));

        pageRequest.getFilterList().forEach(filter -> {
            switch (filter.getCriteria()) {
                case PROJECT_ID -> {
                    ProjectDto project = this.projectService.getProject(filter.getValue());
                    predicates.add(criteriaBuilder.equal(root.get("projectId"), project.getProjectId()));
                }
            }
        });
        Order order = criteriaBuilder.asc(root.get("dbConfigId"));

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(order);
        List<DatabaseConfigEntity> resultList = entityManager.createQuery(query)
                .setFirstResult(pageRequest.getOffset())
                .setMaxResults(pageRequest.getLimit())
                .getResultList();
        DatabaseConfigMapper mapper = DatabaseConfigMapper.INSTANCE;
        return resultList.stream()
                .map(mapper::toDto)
                .toList();
    }
    public long count(GeneralPageRequest generalPageRequest) {
        if (generalPageRequest.getFilterList() == null || generalPageRequest.getFilterList().isEmpty()) {
            return this.databaseConfigRepository.count();
        }
        String value = generalPageRequest.getFilterList().getFirst().getValue();
        ProjectDto project = this.projectService.getProject(value);
        return this.databaseConfigRepository.countByProjectId(project.getProjectId());
    }

    @Transactional
    public Result save(DatabaseConfigDto databaseConfigDto) {
        DatabaseConfigMapper mapper = DatabaseConfigMapper.INSTANCE;
        DatabaseConfigEntity entity = mapper.toEntity(databaseConfigDto);
        this.databaseConfigRepository.save(entity);
        return new SuccessResult();
    }

    @Transactional
    public Result delete(Long dbConfigId) {
        this.databaseConfigRepository.deleteById(dbConfigId);
        return new SuccessResult();
    }
}
