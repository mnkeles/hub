package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.UserPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermissionEntity, Long> {

    @Query("SELECT up FROM UserPermissionEntity up WHERE up.userEntity.userId = :userId")
    List<UserPermissionEntity> findByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT up.permissionEntity.permissionKey FROM UserPermissionEntity up " +
            "WHERE up.userEntity.email = :email " +
            "AND up.userEntity.enabled = 1 " +
            "AND up.permissionEntity.enabled = 1")
    List<String> findPermissionKeysByUserEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM UserPermissionEntity up WHERE up.userEntity.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
