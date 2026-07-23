package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.ServiceAccountPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceAccountPermissionRepository extends JpaRepository<ServiceAccountPermissionEntity, Long> {

    @Query("SELECT sap FROM ServiceAccountPermissionEntity sap JOIN sap.serviceAccountEntity s WHERE s.serviceAccountId = :serviceAccountId")
    List<ServiceAccountPermissionEntity> findByServiceAccountId(@Param("serviceAccountId") Long serviceAccountId);

    @Query("SELECT DISTINCT sap.permissionEntity.permissionKey FROM ServiceAccountPermissionEntity sap " +
            "WHERE sap.serviceAccountEntity.serviceAccountId = :serviceAccountId " +
            "AND sap.serviceAccountEntity.enabled = 1 " +
            "AND sap.permissionEntity.enabled = 1")
    List<String> findPermissionKeysByServiceAccountId(@Param("serviceAccountId") Long serviceAccountId);

    @Query("SELECT DISTINCT sap.projectEntity.projectId FROM ServiceAccountPermissionEntity sap " +
            "WHERE sap.serviceAccountEntity.serviceAccountId = :serviceAccountId " +
            "AND sap.projectEntity IS NOT NULL " +
            "AND sap.serviceAccountEntity.enabled = 1")
    List<Long> findProjectIdsByServiceAccountId(@Param("serviceAccountId") Long serviceAccountId);

    @Query("SELECT COUNT(sap) > 0 FROM ServiceAccountPermissionEntity sap " +
            "WHERE sap.serviceAccountEntity.serviceAccountId = :serviceAccountId " +
            "AND sap.projectEntity IS NULL " +
            "AND sap.serviceAccountEntity.enabled = 1")
    boolean hasGlobalAccess(@Param("serviceAccountId") Long serviceAccountId);

    @Modifying
    @Query("DELETE FROM ServiceAccountPermissionEntity sap WHERE sap.serviceAccountEntity.serviceAccountId = :serviceAccountId")
    void deleteByServiceAccountId(@Param("serviceAccountId") Long serviceAccountId);
}
