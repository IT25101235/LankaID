package lk.sliit.identityissuanceservice;

import com.sun.net.httpserver.HttpServer;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Sms.PhoneSmsSender;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PhoneSmsSenderTests {
    @Test void postsExpectedJsonAndBearerHeaderToConfiguredEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        server.createContext("/api/sms", exchange -> {
            method.set(exchange.getRequestMethod());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        try {
            PhoneSmsSender sender = new PhoneSmsSender(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/api/sms", "test-key", 5, 10);
            sender.send("+94778833578", "LankaID (OTP) - 648467", 180);
            assertEquals("POST", method.get());
            assertEquals("Bearer test-key", auth.get());
            assertEquals("application/json", contentType.get());
            var json = new ObjectMapper().readTree(body.get());
            assertEquals(4, json.size());
            assertEquals("+94778833578", json.get("to").asText());
            assertEquals("LankaID (OTP) - 648467", json.get("message").asText());
            assertEquals(180, json.get("ttlSeconds").asInt());
            String firstId = json.get("requestId").asText();
            assertDoesNotThrow(() -> UUID.fromString(firstId));
            sender.send("+94778833578", "LankaID (OTP) - 123456", 180);
            assertNotEquals(firstId, new ObjectMapper().readTree(body.get()).get("requestId").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test void authenticationAndServerFailuresGiveSafeErrors() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/unauthorized", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "private provider response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/unavailable", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.createContext("/bad-request", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            for (String path : new String[]{"/unauthorized", "/unavailable", "/missing", "/bad-request"}) {
                PhoneSmsSender sender = new PhoneSmsSender(
                        "http://127.0.0.1:" + server.getAddress().getPort() + path, "test-secret", 5, 10);
                Exception error = assertThrows(IllegalArgumentException.class,
                        () -> sender.send("+94778833578", "LankaID (OTP) - 648467", 180));
                String expected = switch (path) {
                    case "/unauthorized" -> "The SMS server rejected the API key. Check the configured SMS API key.";
                    case "/missing" -> "The SMS endpoint was not found or does not accept POST. Check the SMS URL, including /sms.";
                    case "/bad-request" -> "The SMS server rejected the request format.";
                    default -> "The SMS server returned HTTP 503. Please try again later.";
                };
                assertEquals(expected, error.getMessage());
                assertNull(error.getCause());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test void rejectsMissingConfiguration() {
        PhoneSmsSender sender = new PhoneSmsSender("", "", 1, 1);
        assertThrows(IllegalArgumentException.class, () -> sender.send("+94778833578", "OTP", 180));
    }
}
