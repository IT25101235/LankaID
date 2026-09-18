package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.OTP_Mgmt.Email.EmailOtpService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.CitizenRegistrationController;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class CitizenRegistrationControllerTests {
    @Mock UserAccountService accounts;
    @Mock EmailOtpService emailOtp;
    @Mock SmsOtpService smsOtp;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = standaloneSetup(new CitizenRegistrationController(accounts, emailOtp, smsOtp)).build();
    }

    @Test void emailStepNormalizesStoresAndSendsCode() throws Exception {
        when(accounts.validateRegistrationEmail(" Citizen@Example.com ")).thenReturn("citizen@example.com");
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/register/email/send").session(session).param("email", " Citizen@Example.com "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/register"));
        verify(emailOtp).send("citizen@example.com", "citizen-registration-email", session);
    }

    @Test void accountIsCreatedOnlyAfterBothContactMethodsAreVerified() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("registrationEmail", "citizen@example.com");
        session.setAttribute("registrationPhone", "+94771234567");
        when(emailOtp.isVerified("citizen@example.com", "citizen-registration-email", session)).thenReturn(true);
        when(smsOtp.isVerified("+94771234567", "citizen-registration-phone", session)).thenReturn(true);
        PreparedCitizenRegistration prepared = new PreparedCitizenRegistration("citizen@example.com", "+94771234567", "hash");
        when(accounts.prepareCitizenRegistration("citizen@example.com", "+94771234567", "Password123!"))
                .thenReturn(prepared);

        mvc.perform(post("/register").session(session)
                        .param("email", "citizen@example.com").param("phone", "+94771234567")
                        .param("password", "Password123!").param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verify(accounts).registerVerifiedCitizen(prepared);
    }
}
