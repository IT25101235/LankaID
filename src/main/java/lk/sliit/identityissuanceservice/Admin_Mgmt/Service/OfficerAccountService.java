package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class OfficerAccountService {

    public static final String NIC_OFFICER = "NIC_OFFICER";
    public static final String PASSPORT_OFFICER = "PASSPORT_OFFICER";
    public static final String CSR = "CSR";
    public static final String DIRECTOR = "DIRECTOR";
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    private static final String ACTIVE = "ACTIVE";
    private static final String SUSPENDED = "SUSPENDED";
    private static final String REMOVED = "REMOVED";

    private final UserAccountRepository userAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public OfficerAccountService(
            UserAccountRepository userAccountRepository,
            StaffAccountRepository staffAccountRepository) {
        this.userAccountRepository = userAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    @Transactional
    public void createOfficer(OfficerRegistrationDto request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();
        String roleCode = roleCodeFromForm(request.getRole());

        if (roleCode == null) {
            throw new IllegalArgumentException("Please select a valid officer role.");
        }
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("That email address is already registered.");
        }
        if (userAccountRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("That phone number is already registered.");
        }
        String employeeNo = generateEmployeeNo(roleCode);

        UserAccount userAccount = new UserAccount();
        userAccount.setEmail(email);
        userAccount.setPhone(phone);
        userAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userAccount.setAccountStatus(ACTIVE);
        userAccount.setEmailVerified("Y");
        userAccount.setPhoneVerified("Y");
        userAccountRepository.save(userAccount);

        StaffAccount staffAccount = new StaffAccount();
        staffAccount.setUserId(userAccount.getUserId());
        staffAccount.setEmployeeNo(employeeNo);
        staffAccount.setStaffRole(roleCode);
        staffAccount.setStaffStatus(ACTIVE);
        staffAccountRepository.save(staffAccount);
    }

    @Transactional(readOnly = true)
    public List<OfficerDetailsDto> getAllOfficers() {
        return staffAccountRepository
                .findByStaffRoleNotAndStaffStatusNotOrderByEmployeeNoAsc(SYSTEM_ADMIN, REMOVED)
                .stream()
                .map(this::toOfficerDetails)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public void updateOfficer(Integer userId, OfficerUpdateDto request) {
        StaffAccount staffAccount = findManageableOfficer(userId);
        UserAccount userAccount = findUser(userId);

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();
        String roleCode = roleCodeFromForm(request.getRole());

        if (roleCode == null) {
            throw new IllegalArgumentException("Please select a valid officer role.");
        }

        Optional<UserAccount> emailOwner = userAccountRepository.findByEmailIgnoreCase(email);
        if (emailOwner.isPresent() && !emailOwner.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("That email address is already registered.");
        }

        Optional<UserAccount> phoneOwner = userAccountRepository.findByPhone(phone);
        if (phoneOwner.isPresent() && !phoneOwner.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("That phone number is already registered.");
        }

        userAccount.setEmail(email);
        userAccount.setPhone(phone);

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            userAccount.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        if (!roleCode.equals(staffAccount.getStaffRole())) {
            staffAccount.setStaffRole(roleCode);
            staffAccount.setEmployeeNo(generateEmployeeNo(roleCode));
        }

        userAccountRepository.save(userAccount);
        staffAccountRepository.save(staffAccount);
    }

    @Transactional
    public String toggleOfficerStatus(Integer userId) {
        StaffAccount staffAccount = findManageableOfficer(userId);
        UserAccount userAccount = findUser(userId);

        String newStatus = SUSPENDED.equals(staffAccount.getStaffStatus()) ? ACTIVE : SUSPENDED;
        staffAccount.setStaffStatus(newStatus);
        userAccount.setAccountStatus(newStatus);

        staffAccountRepository.save(staffAccount);
        userAccountRepository.save(userAccount);
        return newStatus;
    }

    @Transactional
    public void removeOfficer(Integer userId) {
        StaffAccount staffAccount = findManageableOfficer(userId);
        UserAccount userAccount = findUser(userId);

        staffAccount.setStaffStatus(REMOVED);
        userAccount.setAccountStatus(REMOVED);

        staffAccountRepository.save(staffAccount);
        userAccountRepository.save(userAccount);
    }

    @Transactional(readOnly = true)
    public Optional<OfficerDetailsDto> authenticateOfficer(
            String email,
            String password,
            String selectedRole) {
        String requestedRoleCode = roleCodeFromForm(selectedRole);
        if (requestedRoleCode == null) {
            return Optional.empty();
        }

        return userAccountRepository
                .findByEmailIgnoreCaseAndAccountStatus(email.trim().toLowerCase(Locale.ROOT), ACTIVE)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .flatMap(user -> staffAccountRepository.findById(user.getUserId())
                        .filter(staff -> ACTIVE.equals(staff.getStaffStatus()))
                        .filter(staff -> !SYSTEM_ADMIN.equals(staff.getStaffRole()))
                        .filter(staff -> requestedRoleCode.equals(staff.getStaffRole())
                                || DIRECTOR.equals(staff.getStaffRole()))
                        .flatMap(staff -> toOfficerDetails(staff)));
    }

    public String dashboardForRoleSelection(String selectedRole) {
        return switch (selectedRole) {
            case "nic-officer" -> "/officer/nic/applications";
            case "passport-officer" -> "/officer/passport/applications";
            case "csr" -> "/officer/csr";
            case "director" -> "/officer/director";
            default -> null;
        };
    }

    private Optional<OfficerDetailsDto> toOfficerDetails(StaffAccount staffAccount) {
        return userAccountRepository.findById(staffAccount.getUserId())
                .map(user -> new OfficerDetailsDto(
                        user.getUserId(),
                        staffAccount.getEmployeeNo(),
                        user.getEmail(),
                        user.getPhone(),
                        staffAccount.getStaffRole(),
                        roleLabel(staffAccount.getStaffRole()),
                        roleFilter(staffAccount.getStaffRole()),
                        staffAccount.getStaffStatus()
                ));
    }

    private StaffAccount findManageableOfficer(Integer userId) {
        StaffAccount staffAccount = staffAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Officer account was not found."));

        if (SYSTEM_ADMIN.equals(staffAccount.getStaffRole())) {
            throw new IllegalArgumentException("The system administrator account cannot be changed.");
        }
        if (REMOVED.equals(staffAccount.getStaffStatus())) {
            throw new IllegalArgumentException("This officer account has already been removed.");
        }
        return staffAccount;
    }

    private UserAccount findUser(Integer userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Officer user account was not found."));
    }

    private String roleCodeFromForm(String formRole) {
        if (formRole == null) {
            return null;
        }

        return switch (formRole) {
            case "nic-officer" -> NIC_OFFICER;
            case "passport-officer" -> PASSPORT_OFFICER;
            case "csr" -> CSR;
            case "director" -> DIRECTOR;
            default -> null;
        };
    }

    private String generateEmployeeNo(String roleCode) {
        String prefix = switch (roleCode) {
            case NIC_OFFICER -> "EMP-NIC-";
            case PASSPORT_OFFICER -> "EMP-PASS-";
            case CSR -> "EMP-CSR-";
            case DIRECTOR -> "EMP-DIR-";
            default -> throw new IllegalArgumentException("Invalid officer role.");
        };

        int highestNumber = staffAccountRepository
                .findByEmployeeNoStartingWith(prefix)
                .stream()
                .map(StaffAccount::getEmployeeNo)
                .map(employeeNo -> employeeNo.substring(prefix.length()))
                .filter(number -> number.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return prefix + String.format("%03d", highestNumber + 1);
    }

    private String roleLabel(String roleCode) {
        return switch (roleCode) {
            case NIC_OFFICER -> "NIC Application Officer";
            case PASSPORT_OFFICER -> "Passport Application Officer";
            case CSR -> "Citizen Service Representative";
            case DIRECTOR -> "Director";
            default -> roleCode;
        };
    }

    private String roleFilter(String roleCode) {
        return switch (roleCode) {
            case NIC_OFFICER -> "nic-officer";
            case PASSPORT_OFFICER -> "passport-officer";
            case CSR -> "csr";
            case DIRECTOR -> "director";
            default -> "";
        };
    }
}
