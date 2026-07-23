package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByPermissionKeyAndEnabled(String permissionKey, int enabled);
}
