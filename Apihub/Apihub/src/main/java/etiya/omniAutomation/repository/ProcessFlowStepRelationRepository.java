package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProcessFlowStepRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessFlowStepRelationRepository extends JpaRepository<ProcessFlowStepRelationEntity, Long> {

    List<ProcessFlowStepRelationEntity> findAllByProcessFlowStepIdAndProjectId(Long processFlowStepId, Long projectId);
    
    @Query("SELECT DISTINCT r FROM ProcessFlowStepRelationEntity r " +
           "LEFT JOIN FETCH r.processFlowStepParm " +
           "WHERE r.processFlowStepId = :stepId")
    List<ProcessFlowStepRelationEntity> findAllByProcessFlowStepIdWithParameters(@Param("stepId") Long stepId);

    @Modifying
    @Query(value = """
        WITH deleted_rels AS (
            DELETE FROM proc_flow_step_rel
            WHERE proc_flow_step_id = :stepId
            RETURNING proc_flow_step_parm_id
        )
        DELETE FROM proc_flow_step_parm p
        WHERE p.proc_flow_step_parm_id IN (
            SELECT DISTINCT proc_flow_step_parm_id
            FROM deleted_rels
            WHERE proc_flow_step_parm_id IS NOT NULL
        )
        AND NOT EXISTS (
            SELECT 1
            FROM proc_flow_step_rel rel
            WHERE rel.proc_flow_step_parm_id = p.proc_flow_step_parm_id
        )
        """, nativeQuery = true)
    void deleteRelationsAndParametersNative(@Param("stepId") Long stepId);
}
