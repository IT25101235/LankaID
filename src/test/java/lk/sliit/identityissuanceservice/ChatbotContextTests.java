package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Client.LlmClient;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Controller.ChatbotController;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import static org.junit.jupiter.api.Assertions.*;

class ChatbotContextTests {
    @Test void productionModuleWiresWithBootJacksonWithoutAnyDatabaseOrAccountBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(PublicGuideOnly.class)
                .withPropertyValues("app.chatbot.llm.enabled=false")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(ChatbotController.class));
                    assertFalse(context.getBean(LlmClient.class).isConfigured());
                    assertEquals("GUIDE", context.getBean(ChatbotService.class).reply("How do I apply for a NIC?").source());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("lk.sliit.identityissuanceservice.Chatbot_Mgmt")
    static class PublicGuideOnly { }
}
