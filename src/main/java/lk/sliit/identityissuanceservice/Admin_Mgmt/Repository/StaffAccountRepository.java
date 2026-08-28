package lk.sliit.identityissuanceservice.Admin_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffAccountRepository extends JpaRepository<StaffAccount, Integer> {

    List<StaffAccount> findByEmployeeNoStartingWith(String prefix);

    List<StaffAccount> findByStaffRoleNotAndStaffStatusNotOrderByEmployeeNoAsc(
            String excludedRole,
            String excludedStatus
    );

    boolean existsByUserIdAndStaffRoleAndStaffStatus(
            Integer userId,
            String staffRole,
            String staffStatus
    );
}
