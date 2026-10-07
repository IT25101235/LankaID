package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service;

import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.CitizenGuide;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class CitizenKnowledgeService {
    private final Map<String, CitizenGuide> guides;

    public CitizenKnowledgeService(ObjectMapper mapper) throws IOException {
        try (var content = new ClassPathResource("Chatbot/citizen-guide.json").getInputStream()) {
            List<CitizenGuide> loaded = mapper.readValue(content, new TypeReference<>() { });
            guides = Map.copyOf(loaded.stream().collect(
                    Collectors.toMap(CitizenGuide::topic, Function.identity())));
        }
        if (!guides.containsKey("welcome")) throw new IllegalStateException("Chatbot welcome guide is missing.");
    }

    public CitizenGuide get(String topic) {
        return guides.getOrDefault(topic, guides.get("welcome"));
    }
}
