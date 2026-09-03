package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.User_Mgmt.Dto.EmailVerificationChallenge;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailVerificationServiceTests {

    @Test
    void sentCodeValidatesAndIncorrectCodeDoesNot() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = new EmailVerificationService(
                mailSender,
                "sender@example.com",
                10,
                60
        );

        EmailVerificationChallenge challenge = service.sendVerificationCode("citizen@example.com");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b")
                .matcher(messageCaptor.getValue().getText());
        assertTrue(matcher.find());
        String sentCode = matcher.group(1);
        String differentCode = sentCode.equals("000000") ? "000001" : "000000";
        assertTrue(service.isValid(challenge, sentCode));
        assertFalse(service.isValid(challenge, differentCode));
    }
}
