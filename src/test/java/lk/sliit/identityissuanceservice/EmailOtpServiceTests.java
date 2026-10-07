package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.OTP_Mgmt.Email.EmailOtpService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailOtpServiceTests {
    @Test
    void sentCodeVerifiesOnlyTheBoundEmailAndPurpose() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailOtpService service = new EmailOtpService(sender, "sender@example.com", 600, 5, 60, 900, Clock.systemUTC());
        MockHttpSession session = new MockHttpSession();

        service.send("Citizen@Example.com", "registration", session);
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        Matcher code = Pattern.compile("\\b(\\d{6})\\b").matcher(message.getValue().getText());
        assertTrue(code.find());

        service.verify(code.group(1), "registration", session);
        assertTrue(service.isVerified("citizen@example.com", "registration", session));
        assertFalse(service.isVerified("another@example.com", "registration", session));
        assertFalse(service.isVerified("citizen@example.com", "password-reset", session));
    }
}
