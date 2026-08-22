package lk.sliit.identityissuanceservice.Payment_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentController {

    @GetMapping("/citizen/payment")
    public String paymentPage() {
        return "citizen_frontends/payment_gateway";
    }
}
