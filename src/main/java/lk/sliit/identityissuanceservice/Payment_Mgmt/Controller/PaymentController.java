package lk.sliit.identityissuanceservice.Payment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/citizen/payment")
    public String paymentPage(
            @RequestParam(required = false) Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        if (applicationId == null) {
            model.addAttribute("paymentUnavailable", true);
            return "citizen_frontends/payment_gateway";
        }

        try {
            model.addAttribute("paymentSummary", paymentService.getPaymentSummary(citizenId, applicationId));
            return "citizen_frontends/payment_gateway";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/applications";
        }
    }

    @PostMapping("/citizen/payment")
    public String completePayment(
            @RequestParam Integer applicationId,
            @RequestParam String deliveryMethod,
            @RequestParam(required = false) Integer centreId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        try {
            paymentService.payForApplication(citizenId, applicationId, deliveryMethod, centreId);
            String status = paymentService.getPaymentSummary(citizenId, applicationId)
                    .application().getCurrentStatus();
            if ("APPROVED".equals(status)) {
                redirectAttributes.addFlashAttribute(
                        "appointmentMessage",
                        "Payment and delivery choice confirmed. Select a biometric appointment within the next 14 days."
                );
                return "redirect:/citizen/appointments/biometric?applicationId=" + applicationId;
            }
            redirectAttributes.addFlashAttribute(
                    "applicationMessage",
                    "Your document delivery choice was saved successfully."
            );
            return "redirect:/citizen/applications";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("paymentError", exception.getMessage());
            return "redirect:/citizen/payment?applicationId=" + applicationId;
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute(
                    "paymentError",
                    "The payment could not be saved. Please try again."
            );
            return "redirect:/citizen/payment?applicationId=" + applicationId;
        }
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return userId instanceof Integer id && "CITIZEN".equals(role) ? id : null;
    }
}
