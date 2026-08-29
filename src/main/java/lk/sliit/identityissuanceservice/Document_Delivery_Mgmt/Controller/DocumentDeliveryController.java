package lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Dto.CsrCollectionDetails;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Service.DocumentDeliveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DocumentDeliveryController {

    private final DocumentDeliveryService deliveryService;

    public DocumentDeliveryController(DocumentDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/officer/csr/deliveries")
    public String collectionWorkbench(
            @RequestParam(required = false) String reference,
            HttpSession session,
            Model model
    ) {
        if (!canAccess(session)) return "redirect:/login";

        model.addAttribute("readyCollections", deliveryService.getReadyCollections());
        addOfficerDetails(model, session);
        if (reference != null && !reference.isBlank()) {
            try {
                model.addAttribute("selectedCollection", deliveryService.findCollection(reference));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("collectionError", exception.getMessage());
            }
        }
        return "officers_frontends/document_collection";
    }

    @PostMapping("/officer/csr/deliveries/handover")
    public String handOverDocument(
            @RequestParam String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!canAccess(session)) return "redirect:/login";
        try {
            CsrCollectionDetails details = deliveryService.handOver(
                    reference,
                    (Integer) session.getAttribute("loggedInOfficerId")
            );
            redirectAttributes.addFlashAttribute(
                    "collectionMessage",
                    details.serviceType() + " document handed over to " + details.citizenName() + "."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("collectionError", exception.getMessage());
        }
        return "redirect:/officer/csr/deliveries?reference=" + reference.trim();
    }

    @PostMapping("/citizen/deliveries/confirm")
    public String confirmHomeDelivery(
            @RequestParam Integer applicationId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        try {
            deliveryService.confirmHomeDelivery(citizenId, applicationId);
            redirectAttributes.addFlashAttribute(
                    "applicationMessage",
                    "Thank you. Your document delivery has been confirmed."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
        }
        return "redirect:/citizen/applications";
    }

    private boolean canAccess(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) return false;
        Object role = session.getAttribute("loggedInOfficerRole");
        return OfficerAccountService.CSR.equals(role) || OfficerAccountService.DIRECTOR.equals(role);
    }

    private void addOfficerDetails(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return userId instanceof Integer id && "CITIZEN".equals(role) ? id : null;
    }
}
