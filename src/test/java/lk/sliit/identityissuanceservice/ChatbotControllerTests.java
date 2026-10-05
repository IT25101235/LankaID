package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Controller.ChatbotController;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto.ChatReply;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.ChatbotService;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.ChatbotRateLimiter;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.CitizenNavigationAdvice;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.OfficerSessionIdentityAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ChatbotControllerTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private ChatbotService service;
    private NotificationService notifications;
    private CitizenSupportService support;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(ChatbotService.class);
        notifications = mock(NotificationService.class);
        support = mock(CitizenSupportService.class);
        mvc = standaloneSetup(new ChatbotController(service))
                .setControllerAdvice(new CitizenNavigationAdvice(notifications, support), new OfficerSessionIdentityAdvice())
                .build();
        when(service.reply(anyString())).thenReturn(new ChatReply("Public guide", List.of(), "GUIDE"));
    }

    private String body(String message) { return mapper.writeValueAsString(Map.of("message", message)); }

    @Test void anonymousPublicGuideDoesNotCreateAnIdentitySession() throws Exception {
        mvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.text").value("Public guide"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
        verifyNoInteractions(notifications, support);
    }

    @Test void existingIdentityCookiesDoNotTriggerPersonalReadsEvenWithContextPath() throws Exception {
        MockHttpSession session = new MockHttpSession() {
            @Override public Object getAttribute(String name) {
                if (name.startsWith("loggedIn")) fail("Chatbot must not read identity attributes: " + name);
                return super.getAttribute(name);
            }
        };
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserRole", "CITIZEN");
        session.setAttribute("loggedInOfficerEmail", "private@gov.lk");
        mvc.perform(post("/lankaid/api/chatbot/messages").contextPath("/lankaid").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isOk());
        verifyNoInteractions(notifications, support);
    }

    @Test void encodedEndpointAlsoSkipsPersonalPageAdvice() throws Exception {
        MockHttpSession session = new MockHttpSession() {
            @Override public Object getAttribute(String name) {
                if (name.startsWith("loggedIn")) fail("Encoded route must not read identity attributes");
                return super.getAttribute(name);
            }
        };
        mvc.perform(post(URI.create("/api/%63hatbot/messages")).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isOk());
        verifyNoInteractions(notifications, support);
    }

    @Test void invalidPayloadsDoNotReachTheService() throws Exception {
        for (String content : new String[]{"{}", body(" "), body("x".repeat(501)), "not-json"}) {
            mvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON).content(content))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.text").exists());
        }
        verifyNoInteractions(service, notifications, support);
    }

    @Test void crossSiteBrowserRequestsAreRejected() throws Exception {
        mvc.perform(post("/api/chatbot/messages").header("Origin", "https://unrelated.example")
                        .contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/chatbot/messages").header("Sec-Fetch-Site", "cross-site")
                        .contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void sameOriginBrowserRequestsWorkAndRateLimitsHaveSafeReplies() throws Exception {
        mvc.perform(post("/api/chatbot/messages").header("Origin", "http://localhost")
                        .contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isOk());
        when(service.reply(anyString())).thenThrow(new ChatbotRateLimiter.LimitExceeded(3));
        mvc.perform(post("/api/chatbot/messages").contentType(MediaType.APPLICATION_JSON).content(body("NIC guidance")))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "3"))
                .andExpect(jsonPath("$.text").value("The assistant is receiving too many requests. Please try again shortly."));
    }
}
