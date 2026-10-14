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
// Provides lookup and locking queries for the government identity registry.
public interface GovernmentIdentityRecordRepository extends JpaRepository<GovernmentIdentityRecord, Integer> {
    // Finds records matching either the registered email or phone.
    List<GovernmentIdentityRecord> findAllByRegisteredEmailIgnoreCaseOrRegisteredPhone(String email, String phone);

    // Finds all records with the supplied NIC number.
    List<GovernmentIdentityRecord> findAllByNicNoIgnoreCase(String nicNo);

    // Finds all records with the supplied passport number.
    List<GovernmentIdentityRecord> findAllByPassportNoIgnoreCase(String passportNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GovernmentIdentityRecord r where r.governmentRecordId = :id")
    // Locks one registry record while it is being linked to a citizen.
    Optional<GovernmentIdentityRecord> findByIdForIdentityLink(Integer id);

    // Finds a registry record matching both email and phone.
    Optional<GovernmentIdentityRecord> findByRegisteredEmailIgnoreCaseAndRegisteredPhone(
            String email,
            String phone
    );

    // Finds a registry record by email.
    Optional<GovernmentIdentityRecord> findByRegisteredEmailIgnoreCase(String email);

    // Finds a registry record by phone number.
    Optional<GovernmentIdentityRecord> findByRegisteredPhone(String phone);

    // Checks whether an NIC number already exists in the registry.
    boolean existsByNicNo(String nicNo);

    // Checks whether a passport number already exists in the registry.
    boolean existsByPassportNo(String passportNo);
}
