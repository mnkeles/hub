package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProcessFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProcessFlowRepository extends JpaRepository<ProcessFlowEntity, Long> {

    ProcessFlowEntity getByProcessFlowId(long processFlowId);

    ProcessFlowEntity getByShortCodeAndProjectId(String shortCode, Long projectId);

    @Query("""
            select p from ProcessFlowEntity p order by p.processFlowId
            """)
    List<ProcessFlowEntity> findAllEntities();

    @Query("select count(p.processFlowId) from ProcessFlowEntity p where p.projectId = :projectId")
    long countByProjectId(Long projectId);

    List<ProcessFlowEntity> findAllByProjectId(Long projectId);

    @Query("select p.processFlowId from ProcessFlowEntity p where p.projectId = :projectId")
    List<Long> findProcessFlowIdsByProjectId(Long projectId);

    boolean existsByShortCodeAndProjectId(String shortCode, Long projectId);
}