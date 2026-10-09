package lk.sliit.identityissuanceservice.Payment_Mgmt;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Controller.PaymentController;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.PaymentSummary;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PaymentCollectionCentreValidationTests {

    @Test
    void staleCollectionCentreSelectionDoesNotReachPaymentService() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.getPaymentSummary(1, 42)).thenReturn(summary(true, false));
        standaloneSetup(new PaymentController(service)).build()
                .perform(post("/citizen/payment").session(citizenSession())
                        .param("applicationId", "42").param("deliveryMethod", "CENTRE_COLLECTION")
                        .param("centreId", "9"))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/payment_gateway"))
                .andExpect(model().attribute("selectedCentreId", 9))
                .andExpect(model().attribute("paymentError",
                        "Select an active service centre for document collection."));
        verify(service, never()).payForApplication(any(), any(), any(), any());
    }

    @Test
    void unpaidDeliveryCannotSelectInactiveCentre() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.getPaymentSummary(1, 42)).thenReturn(summary(false, true));
        standaloneSetup(new PaymentController(service)).build()
                .perform(post("/citizen/payment").session(citizenSession())
                        .param("applicationId", "42").param("deliveryMethod", "CENTRE_COLLECTION")
                        .param("centreId", "9").param("cardholderName", "Test Citizen")
                        .param("cardNumber", "4111111111111111")
                        .param("expiry", "12/99").param("cvv", "123"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("paymentError",
                        "Select an active service centre for document collection."));
        verify(service, never()).payForApplication(any(), any(), any(), any());
    }

    @Test
    void unpaidDeliveryStillRequiresCardDetails() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.getPaymentSummary(1, 42)).thenReturn(summary(false, true));
        standaloneSetup(new PaymentController(service)).build()
                .perform(post("/citizen/payment").session(citizenSession())
                        .param("applicationId", "42").param("deliveryMethod", "CENTRE_COLLECTION")
                        .param("centreId", "9"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cardErrors"));
        verify(service, never()).payForApplication(any(), any(), any(), any());
    }

    @Test
    void completedPaymentDoesNotRejectRepeatedSubmissionAfterCentreDeactivation() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.getPaymentSummary(1, 42)).thenReturn(summary(true, true));
        standaloneSetup(new PaymentController(service)).build()
                .perform(post("/citizen/payment").session(citizenSession())
                        .param("applicationId", "42").param("deliveryMethod", "CENTRE_COLLECTION")
                        .param("centreId", "9"))
                .andExpect(status().is3xxRedirection());
        verify(service).payForApplication(1, 42, "CENTRE_COLLECTION", 9);
    }

    private PaymentSummary summary(boolean paid, boolean deliverySelected) {
        CitizenApplication application = new CitizenApplication();
        application.setCurrentStatus("APPROVED");
        Payment payment = new Payment();
        payment.setPaymentStatus("PAID");
        return new PaymentSummary(
                application, new BigDecimal("2000"), new BigDecimal("500"), paid ? payment : null,
                deliverySelected ? new DocumentDelivery() : null, List.of(), "12 Main Street");
    }

    private MockHttpSession citizenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 1);
        session.setAttribute("loggedInUserRole", "CITIZEN");

        return session;
    }
}
