package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

@Repository
public interface GovernmentIdentityRecordRepository extends JpaRepository<GovernmentIdentityRecord, Integer> {
    List<GovernmentIdentityRecord> findAllByRegisteredEmailIgnoreCaseOrRegisteredPhone(String email, String phone);

    List<GovernmentIdentityRecord> findAllByNicNoIgnoreCase(String nicNo);

    List<GovernmentIdentityRecord> findAllByPassportNoIgnoreCase(String passportNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GovernmentIdentityRecord r where r.governmentRecordId = :id")
    Optional<GovernmentIdentityRecord> findByIdForIdentityLink(Integer id);

    Optional<GovernmentIdentityRecord> findByRegisteredEmailIgnoreCaseAndRegisteredPhone(
            String email,
            String phone
    );

    Optional<GovernmentIdentityRecord> findByRegisteredEmailIgnoreCase(String email);

    Optional<GovernmentIdentityRecord> findByRegisteredPhone(String phone);

    boolean existsByNicNo(String nicNo);

    boolean existsByPassportNo(String passportNo);
}
