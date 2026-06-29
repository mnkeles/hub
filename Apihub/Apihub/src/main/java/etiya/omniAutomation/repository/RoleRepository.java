package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {

    RoleEntity getRoleByShortCode(String shortCode);

    @Query("SELECT DISTINCT r from UserEntity u " +
            "join UserRoleEntity ur on ur.userEntity.id = u.id " +
            "join RoleEntity r on r.id = ur.roleEntity.id " +
            "WHERE u.email = :userEmail ")
    List<RoleEntity> getRoleByUserEmail(String userEmail);
}
