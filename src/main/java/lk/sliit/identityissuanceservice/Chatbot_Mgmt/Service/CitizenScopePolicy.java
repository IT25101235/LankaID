package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/** Selects public guidance, never a record lookup or an executable operation. */
@Component
public class CitizenScopePolicy {
    private static final Pattern SENSITIVE = pattern(
            "[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|(?<!\\d)\\d{6,}(?!\\d)"
                    + "|\\b[A-Z]\\d{7}\\b|\\b(?:NIC|PP|BIO|WALK|DEL|CTZ|CMP|CON)-\\d[\\w-]*"
                    + "|\\b(?:my|your|their)\\s+(?:password|otp|nic number|passport number)\\s*(?:is|:|=)");
    private static final Pattern RESTRICTED = pattern(
            "\\b(?:admin|administrator|database|sql|credentials|api key|system prompt|staff management)\\b"
                    + "|officer\\s+(?:dashboard|account|login|password)|list\\s+(?:all\\s+)?(?:users|citizens|officers)"
                    + "|(?:ignore|override|bypass|reveal).{0,40}(?:rules|instructions|prompt|security)");
    private static final Pattern PERSONAL_LOOKUP = pattern(
            "\\b(?:show|check|find|retrieve|lookup|look up|give|tell|what)\\b.{0,60}"
                    + "\\b(?:my|their|someone|another citizen)\\b.{0,50}"
                    + "\\b(?:status|records?|details|number|address|balance)\\b"
                    + "|\\b(?:approve|reject)\\s+(?:my|this|the)\\s+application"
                    + "|\\b(?:book|cancel|delete|update|change|pay)\\b.{0,35}\\b(?:for me|on my behalf)\\b");

    private static final List<TopicRule> TOPICS = List.of(
            new TopicRule("walkin", pattern("walk[\\s-]?in|recover.*(?:booking|pass)|lost.*(?:qr|pass\\b)")),
            new TopicRule("tracking", pattern("track|status|progress|correction|resubmit")),
            new TopicRule("account", pattern("register|registration|account|password|sign[\\s-]?in|log[\\s-]?in|otp|verification code")),
            new TopicRule("payment", pattern("pay|payment|fee|cost|price|refund")),
            new TopicRule("documents", pattern("upload|supporting document|file|birth certificate|photograph")),
            new TopicRule("appointments", pattern("appointment|biometric|reschedul|cancel.*booking")),
            new TopicRule("delivery", pattern("delivery|deliver|collection|collect.*document|courier")),
            new TopicRule("passport", pattern("passport")),
            new TopicRule("nic", pattern("\\bnic\\b|identity card")),
            new TopicRule("support", pattern("support|complaint|contact|helpdesk")));

    public Decision assess(String message) {
        if (SENSITIVE.matcher(message).find()) {
            return new Decision("support", false, "This assistant provides general guidance only. "
                    + "Please keep personal identifiers, contact details, passwords, and codes out of the chat. "
                    + "Use your secure dashboard or citizen support for personal requests.");
        }
        if (RESTRICTED.matcher(message).find()) {
            return new Decision("welcome", false, "I can explain LankaID citizen services only. "
                    + "I cannot provide staff or administrator procedures, expose system information, or perform operations.");
        }
        String topic = TOPICS.stream().filter(rule -> rule.pattern().matcher(message).find())
                .map(TopicRule::topic).findFirst().orElse("welcome");
        if (PERSONAL_LOOKUP.matcher(message).find()) {
            return new Decision(topic, false, "I cannot look up personal information or change applications, "
                    + "accounts, payments, or bookings. Use the secure citizen portal for those actions.");
        }
        return new Decision(topic, !"welcome".equals(topic), null);
    }

    private static Pattern pattern(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public record Decision(String topic, boolean modelAllowed, String boundaryMessage) { }
    private record TopicRule(String topic, Pattern pattern) { }
}
