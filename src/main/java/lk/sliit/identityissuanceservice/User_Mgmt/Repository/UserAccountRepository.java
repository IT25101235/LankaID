package lk.sliit.identityissuanceservice.User_Mgmt.Repository;

import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByPhone(String phone);

    Optional<UserAccount> findByEmailIgnoreCaseAndAccountStatus(
            String email,
            String accountStatus
    );

    Optional<UserAccount> findByEmailIgnoreCaseAndAccountStatusAndEmailVerified(
            String email,
            String accountStatus,
            String emailVerified
    );

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    List<UserAccount> findAllByAccountStatusOrderByRegisteredAtDesc(
            String accountStatus
    );
}
