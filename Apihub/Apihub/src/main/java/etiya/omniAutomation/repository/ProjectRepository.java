package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    ProjectEntity findByShortCode(String shortCode);

    List<ProjectEntity> findAllByProjectId(Long projectId);
}
