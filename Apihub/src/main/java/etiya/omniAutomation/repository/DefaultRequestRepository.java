package etiya.omniAutomation.repository;

import etiya.omniAutomation.entity.DefaultRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DefaultRequestRepository extends JpaRepository<DefaultRequestEntity,Long> {

    @Query("select d from DefaultRequestEntity d where d.project = :project and d.systemShortCode = :systemShortCode " +
            "and d.processFlow = :processFlow")
    DefaultRequestEntity findDefaultRequest(String project,String systemShortCode,String processFlow);
}
