package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ApiInformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApiInformationRepository extends JpaRepository<ApiInformationEntity, Long> {

    @Query("select count(p.id) from ApiInformationEntity p where p.projectId = :projectId")
    long countByProjectId(Long projectId);

    List<ApiInformationEntity> findByProjectIdAndIsActive(Long projectId, int isActive);
}
