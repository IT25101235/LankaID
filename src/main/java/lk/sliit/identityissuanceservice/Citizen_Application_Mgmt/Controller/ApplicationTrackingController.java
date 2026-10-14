package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenCorrectionRequestService;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Builds the page where a citizen tracks submitted and draft applications.
public class ApplicationTrackingController {

    // Services used to combine application, correction, and delivery information.
    private final CitizenApplicationService applicationService;
    private final DocumentDeliveryService deliveryService;
    private final CitizenCorrectionRequestService correctionRequestService;

    /** Receives the services used to assemble the citizen's application-tracking page. */
    public ApplicationTrackingController(
            CitizenApplicationService applicationService,
            DocumentDeliveryService deliveryService,
            CitizenCorrectionRequestService correctionRequestService
    ) {
        this.applicationService = applicationService;
        this.deliveryService = deliveryService;
        this.correctionRequestService = correctionRequestService;
    }

    /** Loads all applications, correction messages, and delivery details for the signed-in citizen. */
    @GetMapping("/citizen/applications")
    public String applicationTracking(HttpSession session, Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        model.addAttribute("applications", applicationService.getCitizenApplications(citizenId));
        model.addAttribute("correctionMessages", correctionRequestService.getCurrentCorrectionMessages(citizenId));
        model.addAttribute("deliveryByApplicationId", deliveryService.getCitizenDeliveryMap(citizenId));
        return "citizen_frontends/application_tracking";
    }

    /** Returns the signed-in citizen's ID, or null when the session does not belong to a citizen. */
    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        if (!(userId instanceof Integer) || !"CITIZEN".equals(role)) {
            return null;
        }
        return (Integer) userId;
    }
}
