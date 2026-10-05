package lk.sliit.identityissuanceservice;

import com.sun.net.httpserver.HttpServer;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Client.ChatCompletionsLlmClient;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.CitizenKnowledgeService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ChatbotLlmClientTests {
    @Test void adapterPostsOnlyReviewedContextAndKeepsTheKeyInTheHeader() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> payload = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        server.createContext("/chat", exchange -> {
            payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"choices\":[{\"message\":{\"content\":\"Public NIC guidance\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            ObjectMapper mapper = new ObjectMapper();
            var guide = new CitizenKnowledgeService(mapper).get("nic");
            var client = new ChatCompletionsLlmClient(mapper, true,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat", "test-secret", "test-model", 1, 1, 400);
            assertEquals("Public NIC guidance", client.explain(guide));
            assertEquals("Bearer test-secret", auth.get());
            var json = mapper.readTree(payload.get());
            assertEquals("test-model", json.path("model").asText());
            assertFalse(json.path("stream").asBoolean());
            assertEquals(2, json.path("messages").size());
            assertTrue(json.path("messages").path(1).path("content").asText().contains(guide.question()));
            assertFalse(payload.get().contains("test-secret"));
            assertFalse(payload.get().contains("tools"+"\":"));
        } finally { server.stop(0); }
    }

    @Test void unconfiguredOrInvalidEndpointsDoNotEnableOutboundCalls() {
        for (String url : new String[]{"", "http://external.example/chat", "file:///private", "https://user:pass@example.com/chat"}) {
            var client = new ChatCompletionsLlmClient(new ObjectMapper(), true, url, "test-key", "model", 1, 1, 400);
            assertFalse(client.isConfigured(), url);
        }
        var client = new ChatCompletionsLlmClient(new ObjectMapper(), false,
                "https://provider.example/chat", "test-key", "model", 1, 1, 400);
        assertFalse(client.isConfigured());
    }

    @Test void providerErrorsAndOversizedBodiesExposeNoPrivateDetails() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/error", exchange -> {
            byte[] body = "secret provider response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/large", exchange -> {
            byte[] body = "x".repeat(70_000).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var mapper = new ObjectMapper();
            var guide = new CitizenKnowledgeService(mapper).get("nic");
            for (String path : new String[]{"/error", "/large"}) {
                var client = new ChatCompletionsLlmClient(mapper, true,
                        "http://127.0.0.1:" + server.getAddress().getPort() + path, "test-secret", "model", 1, 1, 400);
                var error = assertThrows(IllegalStateException.class, () -> client.explain(guide));
                assertEquals("Chatbot provider unavailable.", error.getMessage());
                assertNull(error.getCause());
            }
        } finally { server.stop(0); }
    }
}
