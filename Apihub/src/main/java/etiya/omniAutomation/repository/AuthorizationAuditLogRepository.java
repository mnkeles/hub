package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.AuthorizationAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationAuditLogRepository extends JpaRepository<AuthorizationAuditLogEntity, Long> {
}
