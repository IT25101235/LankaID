package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Integer> {

    List<ApplicationDocument> findAllByApplicationIdOrderByUploadedAtAsc(Integer applicationId);

    boolean existsByApplicationIdAndDocumentType(Integer applicationId, String documentType);
}
