package lk.sliit.identityissuanceservice.Shared.Sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Sends SMS requests to the Relay SMS Android application.
 */
@Service
public class PhoneSmsSender implements SmsSender {

    private final RestClient client;
    private final String url;
    private final String apiKey;

    public PhoneSmsSender(
            @Value("${app.sms.url:}") String url,
            @Value("${app.sms.api-key:}") String apiKey,
            @Value("${app.sms.connect-timeout-seconds:5}") int connectTimeout,
            @Value("${app.sms.read-timeout-seconds:10}") int readTimeout) {

        this.url = url;
        this.apiKey = apiKey;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(readTimeout));

        this.client = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public void send(String phone, String message, int ttlSeconds) {

        if (url == null || url.isBlank()
                || apiKey == null || apiKey.isBlank()) {

            throw new IllegalArgumentException(
                    "SMS is not configured. Please contact the service centre."
            );
        }

        String requestId = UUID.randomUUID().toString();

        /*
         * Create the exact JSON format required by Relay SMS.
         */
        String json = """
                {
                  "requestId":"%s",
                  "to":"%s",
                  "message":"%s",
                  "ttlSeconds":%d
                }
                """.formatted(
                escapeJson(requestId),
                escapeJson(phone),
                escapeJson(message),
                ttlSeconds
        );

        /*
         * Convert to bytes so we know the exact Content-Length.
         */
        byte[] jsonBody = json.getBytes(StandardCharsets.UTF_8);

        try {

            client.post()
                    .uri(url)
                    .headers(headers -> {
                        headers.setBearerAuth(apiKey);
                        headers.setContentType(MediaType.APPLICATION_JSON);

                        // Relay SMS requires Content-Length.
                        headers.setContentLength(jsonBody.length);
                    })
                    .body(jsonBody)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException exception) {

            int status = exception.getStatusCode().value();

            // Useful during development to see what Relay SMS actually returned.
            System.out.println("SMS HTTP STATUS: " + status);
            System.out.println(
                    "SMS RESPONSE: " + exception.getResponseBodyAsString()
            );

            String errorMessage = switch (status) {

                case 400 ->
                        "The SMS server rejected the request format.";

                case 401, 403 ->
                        "The SMS server rejected the API key. "
                                + "Check the configured SMS API key.";

                case 404, 405 ->
                        "The SMS endpoint was not found or does not accept POST. "
                                + "Check the SMS URL, including /sms.";

                case 422 ->
                        "The SMS server rejected the phone number or message data.";

                case 429 ->
                        "The SMS server is receiving too many requests. "
                                + "Please try again later.";

                default ->
                        "The SMS server returned HTTP "
                                + status
                                + ". Please try again later.";
            };

            throw new IllegalArgumentException(errorMessage);

        } catch (ResourceAccessException exception) {

            throw new IllegalArgumentException(
                    "Could not connect to the SMS server or the request timed out. "
                            + "Check that the phone server is running "
                            + "and its IP and port are correct."
            );

        } catch (RestClientException | IllegalStateException exception) {

            throw new IllegalArgumentException(
                    "Could not send the SMS request. "
                            + "Please check the SMS API configuration."
            );
        }
    }

    /**
     * Escapes characters that have special meaning inside JSON strings.
     */
    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}