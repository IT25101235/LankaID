package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AdminAccountService {

    private static final String ADMIN_EMAIL = "admin@gov.lk";
    private static final String ADMIN_PASSWORD = "admin12345";
    private static final String ADMIN_PHONE = "+94110000000";
    private static final String ADMIN_EMPLOYEE_NO = "ADMIN-001";
    private static final String ADMIN_ROLE = "SYSTEM_ADMIN";

    private final UserAccountRepository userAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAccountService(
            UserAccountRepository userAccountRepository,
            StaffAccountRepository staffAccountRepository) {
        this.userAccountRepository = userAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    @Transactional
    public void createDefaultAdminAccount() {
        UserAccount adminUser = userAccountRepository
                .findByEmailIgnoreCase(ADMIN_EMAIL)
                .orElseGet(this::createAdminUser);

        if (!passwordEncoder.matches(ADMIN_PASSWORD, adminUser.getPasswordHash())) {
            adminUser.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        }
        adminUser.setAccountStatus("ACTIVE");
        adminUser.setEmailVerified("Y");
        adminUser.setPhoneVerified("Y");
        userAccountRepository.save(adminUser);

        StaffAccount adminAccount = staffAccountRepository
                .findById(adminUser.getUserId())
                .orElseGet(() -> new StaffAccount(adminUser.getUserId(), ADMIN_EMPLOYEE_NO));

        adminAccount.setEmployeeNo(ADMIN_EMPLOYEE_NO);
        adminAccount.setStaffRole(ADMIN_ROLE);
        adminAccount.setStaffStatus("ACTIVE");
        staffAccountRepository.save(adminAccount);
    }

    public Optional<UserAccount> authenticateAdmin(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userAccountRepository
                .findByEmailIgnoreCaseAndAccountStatus(normalizedEmail, "ACTIVE")
                .filter(user -> staffAccountRepository.existsByUserIdAndStaffRoleAndStaffStatus(
                        user.getUserId(),
                        ADMIN_ROLE,
                        "ACTIVE"
                ))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
    }

    public boolean isSystemAdminEmail(String email) {
        return email != null && ADMIN_EMAIL.equalsIgnoreCase(email.trim());
    }

    private UserAccount createAdminUser() {
        UserAccount adminUser = new UserAccount();
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setPhone(ADMIN_PHONE);
        adminUser.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        return userAccountRepository.save(adminUser);
    }
}
