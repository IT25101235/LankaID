package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller.CitizenApplicationController;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller.CitizenSupportController;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller.WalkInAppointmentController;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Controller.DocumentRenewalReplacementController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.BiometricScheduleController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.UUID;

/** Require deletion forms to come from the same browser session that displayed them. */
@ControllerAdvice(assignableTypes = {CitizenApplicationController.class, DocumentRenewalReplacementController.class,
        CitizenSupportController.class, WalkInAppointmentController.class, BiometricScheduleController.class})
public class DeletionFormToken {
    private static final String KEY = "deletionFormToken";

    @ModelAttribute("deleteToken")
    public String token(HttpSession session) {
        if (session.getAttribute(KEY) == null) session.setAttribute(KEY, UUID.randomUUID().toString());
        return (String) session.getAttribute(KEY);
    }

    public static void check(String token, HttpSession session) {
        if (token == null || !token.equals(session.getAttribute(KEY))) {
            throw new IllegalArgumentException("Reload the page before confirming deletion.");
        }
    }
}
