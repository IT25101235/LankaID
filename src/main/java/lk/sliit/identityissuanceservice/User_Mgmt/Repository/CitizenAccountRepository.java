package lk.sliit.identityissuanceservice.User_Mgmt.Repository;

import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitizenAccountRepository extends JpaRepository<CitizenAccount, Integer> {
}
