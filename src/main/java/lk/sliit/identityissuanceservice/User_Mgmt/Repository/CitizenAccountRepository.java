package lk.sliit.identityissuanceservice.User_Mgmt.Repository;

import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CitizenAccountRepository extends JpaRepository<CitizenAccount, Integer> {
    boolean existsByGovernmentRecordIdAndUserIdNot(Integer governmentRecordId, Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CitizenAccount c where c.userId = :id")
    Optional<CitizenAccount> findByIdForIdentityLink(Integer id);

    @Query(value = "SELECT COUNT(*) FROM dbo.SupportRequest "
            + "WHERE citizen_id = :id AND request_type = 'CONTACT' "
            + "AND subject = 'Existing document verification'", nativeQuery = true)
    long countIdentityRequests(Integer id);
}
