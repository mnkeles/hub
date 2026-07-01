package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    @Query("SELECT CASE WHEN rt.userId = :userId AND rt.token = :token and rt.expiryDate >= :expiryDate THEN true ELSE false END FROM RefreshTokenEntity rt")
    boolean isTokenExpired(Long userId, String token, Instant expiryDate);

    Optional<RefreshTokenEntity> findByUserId(Long userId);
}
