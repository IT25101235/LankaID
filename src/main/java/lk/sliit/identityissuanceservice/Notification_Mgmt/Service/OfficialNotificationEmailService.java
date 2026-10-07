package lk.sliit.identityissuanceservice.Notification_Mgmt.Service;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.AdminRecipient;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.EmailDispatchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OfficialNotificationEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public OfficialNotificationEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public EmailDispatchResult sendOfficialEmail(List<AdminRecipient> recipients, String title, String content) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("Official email is not configured. Set app.mail.from before sending.");
        }

        int sent = 0;
        int failed = 0;
        for (AdminRecipient recipient : recipients) {
            try {
                SimpleMailMessage email = new SimpleMailMessage();
                email.setFrom(fromAddress);
                email.setTo(recipient.email());
                email.setSubject("[LankaID Official] " + title.trim());
                email.setText(officialTemplate(title, content));
                mailSender.send(email);
                sent++;
            } catch (MailException exception) {
                failed++;
            }
        }
        return new EmailDispatchResult(sent, failed);
    }

    String officialTemplate(String title, String content) {
        return """
                GOVERNMENT OF SRI LANKA
                LankaID – National Identity Services
                OFFICIAL NOTIFICATION

                Subject: %s

                %s

                This is an automated official communication from LankaID.
                Please do not reply to this email. For assistance, use Contact / Complaint in the citizen portal or contact Government Information Centre 1919.

                © Government of Sri Lanka · LankaID National Identity Services
                """.formatted(title.trim(), content.trim());
    }
}
