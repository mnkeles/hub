package etiya.omniAutomation.repository;

import etiya.omniAutomation.business.dto.UserRoleDto;
import etiya.omniAutomation.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Integer> {

    UserRoleEntity getUserRoleById(long id);
}
