package lk.sliit.identityissuanceservice.User_Mgmt.Service;

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
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = normalizePhone(phone);

        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account already uses this email address.");
        }
        if (userAccountRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("An account already uses this phone number.");
        }

        UserAccount account = new UserAccount();
        account.setEmail(normalizedEmail);
        account.setPhone(normalizedPhone);
        account.setPasswordHash(passwordEncoder.encode(password));

        UserAccount savedAccount = userAccountRepository.save(account);
        citizenAccountRepository.save(new CitizenAccount(savedAccount.getUserId()));
        return savedAccount;
    }

    public Optional<UserAccount> authenticateCitizen(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userAccountRepository
                .findByEmailIgnoreCaseAndAccountStatus(normalizedEmail, "ACTIVE")
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
