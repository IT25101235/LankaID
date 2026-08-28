package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GovernmentIdentityRecordRepository extends JpaRepository<GovernmentIdentityRecord, Integer> {

    Optional<GovernmentIdentityRecord> findByRegisteredEmailIgnoreCaseAndRegisteredPhone(
            String email,
            String phone
    );

    Optional<GovernmentIdentityRecord> findByRegisteredEmailIgnoreCase(String email);

    Optional<GovernmentIdentityRecord> findByRegisteredPhone(String phone);

    boolean existsByNicNo(String nicNo);

    boolean existsByPassportNo(String passportNo);
}
