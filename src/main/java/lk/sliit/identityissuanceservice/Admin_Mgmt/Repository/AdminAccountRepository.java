package lk.sliit.identityissuanceservice.Admin_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Integer> {

    boolean existsByUserIdAndStaffRoleAndStaffStatus(
            Integer userId,
            String staffRole,
            String staffStatus
    );
}
