package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto;

import java.util.List;

public record ChatReply(String text, List<ChatLink> links, String source) {
    public ChatReply {
        links = List.copyOf(links);
    }
}
