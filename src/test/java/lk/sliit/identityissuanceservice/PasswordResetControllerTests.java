package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.OTP_Mgmt.Email.EmailOtpService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.PasswordResetController;
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
class PasswordResetControllerTests {
    @Mock UserAccountService accounts;
    @Mock EmailOtpService emailOtp;
    private MockMvc mvc;

    @BeforeEach void setUp() { mvc = standaloneSetup(new PasswordResetController(accounts, emailOtp)).build(); }

    @Test void resetEmailMustBelongToAnActiveCitizen() throws Exception {
        when(accounts.requirePasswordResetCitizen("citizen@example.com")).thenReturn("citizen@example.com");
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/forgot-password/email/send").session(session).param("email", "citizen@example.com"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/forgot-password"));
        verify(emailOtp).send("citizen@example.com", "citizen-password-reset", session);
    }

    @Test void verifiedCitizenCanSetANewPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("passwordResetEmail", "citizen@example.com");
        when(emailOtp.isVerified("citizen@example.com", "citizen-password-reset", session)).thenReturn(true);
        mvc.perform(post("/forgot-password/reset").session(session)
                        .param("password", "NewPassword123!").param("confirmPassword", "NewPassword123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verify(accounts).resetCitizenPassword("citizen@example.com", "NewPassword123!");
    }
}
