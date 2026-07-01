package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ApiLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiLogRepository extends JpaRepository<ApiLogEntity, Long> {
}
