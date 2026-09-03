package lk.sliit.identityissuanceservice.User_Mgmt.Service;

import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final CitizenAccountRepository citizenAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            CitizenAccountRepository citizenAccountRepository) {
        this.userAccountRepository = userAccountRepository;
        this.citizenAccountRepository = citizenAccountRepository;
    }

    @Transactional
    public UserAccount registerCitizen(String email, String phone, String password) {
        PreparedCitizenRegistration registration = prepareCitizenRegistration(email, phone, password);
        return saveCitizen(registration, true);
    }

    public PreparedCitizenRegistration prepareCitizenRegistration(String email, String phone, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = normalizePhone(phone);

        assertRegistrationAvailable(normalizedEmail, normalizedPhone);
        return new PreparedCitizenRegistration(
                normalizedEmail,
                normalizedPhone,
                passwordEncoder.encode(password)
        );
    }

    @Transactional
    public UserAccount registerVerifiedCitizen(PreparedCitizenRegistration registration) {
        assertRegistrationAvailable(registration.email(), registration.phone());
        return saveCitizen(registration, true);
    }

    private void assertRegistrationAvailable(String normalizedEmail, String normalizedPhone) {
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account already uses this email address.");
        }
        if (userAccountRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("An account already uses this phone number.");
        }
    }

    private UserAccount saveCitizen(PreparedCitizenRegistration registration, boolean emailVerified) {
        UserAccount account = new UserAccount();
        account.setEmail(registration.email());
        account.setPhone(registration.phone());
        account.setPasswordHash(registration.passwordHash());
        account.setEmailVerified(emailVerified ? "Y" : "N");

        UserAccount savedAccount = userAccountRepository.save(account);
        citizenAccountRepository.save(new CitizenAccount(savedAccount.getUserId()));
        return savedAccount;
    }

    public Optional<UserAccount> authenticateCitizen(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userAccountRepository
                .findByEmailIgnoreCaseAndAccountStatusAndEmailVerified(normalizedEmail, "ACTIVE", "Y")
                .filter(account -> citizenAccountRepository.existsById(account.getUserId()))
                .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()));
    }

    private String normalizePhone(String phone) {
        String value = phone.trim();
        if (value.startsWith("0")) {
            return "+94" + value.substring(1);
        }
        return value;
    }
}
