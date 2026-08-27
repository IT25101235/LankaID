package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatbotController {

    @GetMapping("/chatbot")
    public String chatbotPage() {
        return "Chatbot/chatbot";
    }
}
