package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Integer> {

    @Query(value = "SELECT ue.password FROM UserEntity ue WHERE ue.email = :email")
    String getUserPasswordByEmail(String email);

    Optional<UserEntity> findByEmailAndEnabled(String email, int enabled);

    @Query(value = "FROM UserEntity ue WHERE ue.enabled = 1")
    List<UserEntity> getUserIsEnabled();

    UserEntity findByEmail(String email);

    @Query("SELECT ue.id FROM UserEntity ue WHERE ue.email = :email")
    long findUserIdWithEmail(String email);

    UserEntity findByEmailAndPasswordAndEnabled(String email, String password, int enabled);
}
