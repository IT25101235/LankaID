package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Client;

import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.CitizenGuide;

/** Only approved public guides can cross the provider boundary, never raw visitor messages. */
public interface LlmClient {
    boolean isConfigured();
    String explain(CitizenGuide guide);
}
