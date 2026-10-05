package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.ChatLink;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.ChatReply;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.ChatRequest;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.ChatbotRateLimiter;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.ChatbotService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class ChatbotController {
    private final ChatbotService chatbot;
    public ChatbotController(ChatbotService chatbot) { this.chatbot = chatbot; }

    @PostMapping(value = "/api/chatbot/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatReply> message(@Valid @RequestBody ChatRequest request,
                                             HttpServletRequest http) {
        if (!sameOrigin(http)) {
            return response(403, new ChatReply("Open the assistant from the LankaID landing page.", List.of(), "BOUNDARY"));
        }
        try {
            return response(200, chatbot.reply(request.message()));
        } catch (ChatbotRateLimiter.LimitExceeded exception) {
            return ResponseEntity.status(429).cacheControl(CacheControl.noStore())
                    .header("Retry-After", String.valueOf(exception.retryAfter()))
                    .body(new ChatReply("The assistant is receiving too many requests. Please try again shortly.",
                            List.of(new ChatLink("Citizen support", "/citizen/support")), "GUIDE"));
        }
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ChatReply> invalidQuestion() {
        return response(400, new ChatReply("Enter a question of at most 500 characters.", List.of(), "GUIDE"));
    }

    private static ResponseEntity<ChatReply> response(int status, ChatReply body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }

    private static boolean sameOrigin(HttpServletRequest request) {
        if ("cross-site".equalsIgnoreCase(request.getHeader("Sec-Fetch-Site"))) return false;
        String origin = request.getHeader("Origin");
        if (origin == null) return true; // Also supports direct public-guide API clients.
        try {
            URI uri = URI.create(origin);
            int port = uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
            return request.getScheme().equalsIgnoreCase(uri.getScheme())
                    && request.getServerName().equalsIgnoreCase(uri.getHost())
                    && request.getServerPort() == port && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
