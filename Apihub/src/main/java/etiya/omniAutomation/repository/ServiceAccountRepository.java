package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ServiceAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccountEntity, Long> {
    Optional<ServiceAccountEntity> findByServiceCodeAndEnabled(String serviceCode, int enabled);
}
