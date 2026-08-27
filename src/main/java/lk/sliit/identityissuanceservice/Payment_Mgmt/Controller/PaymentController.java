package lk.sliit.identityissuanceservice.Payment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentController {

    @GetMapping("/citizen/payment")
    public String paymentPage(HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        return "citizen_frontends/payment_gateway";
    }
}
