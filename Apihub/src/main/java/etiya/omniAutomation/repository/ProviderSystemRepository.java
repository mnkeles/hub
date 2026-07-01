package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ProviderSystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderSystemRepository extends JpaRepository<ProviderSystemEntity, Integer> {

    ProviderSystemEntity getProviderSystemById(long id);

    ProviderSystemEntity getProviderSystemByShortCode(String shortCode);
}
