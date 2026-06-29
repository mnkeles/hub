package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.LdapUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LdapUserRepository extends JpaRepository<LdapUserEntity, Long> {

    @Query("FROM LdapUserEntity lu WHERE lu.username = :username AND lu.enabled = :enabled")
    LdapUserEntity findByUsernameAndEnabled(@Param("username") String username, @Param("enabled") int enabled);
}
