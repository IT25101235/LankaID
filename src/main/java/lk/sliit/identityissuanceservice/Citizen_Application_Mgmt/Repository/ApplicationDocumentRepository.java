package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// Provides database access for uploaded application-document records.
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Integer> {

    // Returns an application's documents in upload order.
    List<ApplicationDocument> findAllByApplicationIdOrderByUploadedAtAsc(Integer applicationId);

    // Checks whether the application already contains the given document type.
    boolean existsByApplicationIdAndDocumentType(Integer applicationId, String documentType);

    // Finds one document type within an application.
    Optional<ApplicationDocument> findByApplicationIdAndDocumentType(
            Integer applicationId,
            String documentType
    );
}
