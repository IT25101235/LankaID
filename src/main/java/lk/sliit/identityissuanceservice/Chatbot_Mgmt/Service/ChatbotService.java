package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service;

import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Client.LlmClient;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.ChatReply;
import org.springframework.stereotype.Service;

/** Stateless public guidance. Deliberately has no repository, session, or account-service dependency. */
@Service
public class ChatbotService {
    private final CitizenKnowledgeService knowledge;
    private final CitizenScopePolicy scope;
    private final LlmClient llm;
    private final ChatbotRateLimiter limits;

    public ChatbotService(CitizenKnowledgeService knowledge, CitizenScopePolicy scope,
                          LlmClient llm, ChatbotRateLimiter limits) {
        this.knowledge = knowledge;
        this.scope = scope;
        this.llm = llm;
        this.limits = limits;
    }

    public ChatReply reply(String message) {
        if (message == null || message.isBlank() || message.length() > 500) {
            throw new IllegalArgumentException("Enter a question of at most 500 characters.");
        }
        try (var permit = limits.acquire()) {
            var decision = scope.assess(message);
            var guide = knowledge.get(decision.topic());
            if (decision.boundaryMessage() != null) {
                return new ChatReply(decision.boundaryMessage() + "\n\n" + guide.text(), guide.links(), "BOUNDARY");
            }
            if (decision.modelAllowed() && llm.isConfigured()) {
                try {
                    String answer = llm.explain(guide);
                    if (answer != null && !answer.isBlank() && answer.length() <= 4000) {
                        return new ChatReply(answer, guide.links(), "MODEL");
                    }
                } catch (RuntimeException exception) {
                    // A provider outage must not stop the landing-page help guide.
                }
            }
            return new ChatReply(guide.text(), guide.links(), "GUIDE");
        }
    }
}
