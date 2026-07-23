package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ServiceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceTokenRepository extends JpaRepository<ServiceTokenEntity, Long> {
    Optional<ServiceTokenEntity> findByTokenHash(String tokenHash);
}
