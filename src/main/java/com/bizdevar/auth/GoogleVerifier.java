package com.bizdevar.auth;

import com.bizdevar.common.ApiException;
import com.bizdevar.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Google ID token-i Google-un tokeninfo endpoint-i ile yoxlayir (elave kitabxana teleb etmir).
 * Qaytarir: email, ad, sekil ve google sub id.
 */
@Component
public class GoogleVerifier {

    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public GoogleVerifier(AppProperties props) {
        this.props = props;
    }

    public record GoogleUser(String sub, String email, String name, String picture) {}

    public GoogleUser verify(String idToken) {
        String clientId = props.getGoogle().getClientId();
        if (clientId == null || clientId.isBlank()) {
            throw ApiException.internal(
                    "Google girisi konfiqurasiya olunmayib. application.yml-de bizdevar.google.client-id deyerini teyin et.");
        }
        if (idToken == null || idToken.isBlank()) {
            throw ApiException.badRequest("Google token tapilmadi");
        }
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw ApiException.unauthorized("Google token etibarsizdir");
            }
            JsonNode node = mapper.readTree(res.body());
            String aud = node.path("aud").asText("");
            if (!clientId.equals(aud)) {
                throw ApiException.unauthorized("Google token bu tetbiq ucun deyil");
            }
            String email = node.path("email").asText("");
            boolean emailVerified = node.path("email_verified").asBoolean(false)
                    || "true".equals(node.path("email_verified").asText(""));
            if (email.isBlank() || !emailVerified) {
                throw ApiException.unauthorized("Google email tesdiqlenmeyib");
            }
            return new GoogleUser(
                    node.path("sub").asText(""),
                    email,
                    node.path("name").asText(""),
                    node.path("picture").asText(""));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.unauthorized("Google ile giris alinmadi");
        }
    }
}
