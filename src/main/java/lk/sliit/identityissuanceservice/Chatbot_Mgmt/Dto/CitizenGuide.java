package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto;

import java.util.List;

/** Public, reviewed content only. No applicant or account fields belong here. */
public record CitizenGuide(String topic, String question, String text, List<ChatLink> links) {
    public CitizenGuide {
        links = List.copyOf(links);
    }
}
