package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Controller.PaymentController;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.CardPaymentForm;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.PaymentSummary;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerValidationTests {
    @Test
    void invalidCardPreservesOtherFieldsAndDoesNotRecordPayment() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.getPaymentSummary(1, 42)).thenReturn(summary(false));
        MockMvc mvc = standaloneSetup(new PaymentController(service)).build();
        var result = mvc.perform(post("/citizen/payment").session(citizenSession())
                        .param("applicationId", "42").param("deliveryMethod", "CENTRE_COLLECTION")
                        .param("centreId", "2").param("cardholderName", "Test Citizen")
                        .param("cardNumber", "4111 1111 1111 1111")
                        .param("expiry", "13/29").param("cvv", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/payment_gateway"))
                .andExpect(model().attributeExists("cardErrors", "paymentError"))
                .andExpect(model().attribute("selectedCentreId", 2))
                .andReturn();
        CardPaymentForm form = (CardPaymentForm) result.getModelAndView().getModel().get("cardForm");
        assertEquals("Test Citizen", form.getCardholderName());
        assertEquals("4111 1111 1111 1111", form.getCardNumber());
        verify(service, never()).payForApplication(any(), any(), any(), any());
    }

    @Test
    void paidApplicationCanChooseFreeCollectionWithoutCardDetails() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.getPaymentSummary(1, 42)).thenReturn(summary(true));
        MockMvc mvc = standaloneSetup(new PaymentController(service)).build();
        mvc.perform(post("/citizen/payment").session(citizenSession())
                        .param("applicationId", "42").param("deliveryMethod", "CENTRE_COLLECTION")
                        .param("centreId", "2"))
                .andExpect(status().is3xxRedirection());
        verify(service).payForApplication(1, 42, "CENTRE_COLLECTION", 2);
    }

    private PaymentSummary summary(boolean paid) {
        CitizenApplication application = new CitizenApplication();
        application.setCurrentStatus("APPROVED");
        Payment payment = new Payment();
        payment.setPaymentStatus("PAID");
        Centre centre = new Centre();
        centre.setCentreId(2);
        centre.setCentreStatus("ACTIVE");
        return new PaymentSummary(application, new BigDecimal("2000"), new BigDecimal("500"),
                paid ? payment : null, null, List.of(centre), "12 Main Street");
    }

    private MockHttpSession citizenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 1);
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }
}
