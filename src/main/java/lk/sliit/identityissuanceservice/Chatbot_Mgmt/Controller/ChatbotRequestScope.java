package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/** Uses route metadata so encoded URLs cannot cause the guide to read page identity. */
public final class ChatbotRequestScope {
    private ChatbotRequestScope() { }

    public static boolean isChatbotRequest(HttpServletRequest request) {
        Object mapped = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (mapped instanceof HandlerMethod handler
                && handler.getMethod().getDeclaringClass().getPackageName()
                    .startsWith("lk.sliit.identityissuanceservice.Chatbot_Mgmt.")) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/chatbot/");
    }
}
