package com.bizdevar.compreface;

import com.bizdevar.common.ApiException;
import com.bizdevar.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class CompreFaceService {

    private static final Logger log = LoggerFactory.getLogger(CompreFaceService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AppProperties props;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public CompreFaceService(AppProperties props) {
        this.props = props;
    }

    public boolean enabled() {
        AppProperties.CompreFace cf = props.getCompreFace();
        return cf.isEnabled() && cf.getApiKey() != null && !cf.getApiKey().isBlank();
    }

    public void ensureSubject(String subject) {
        if (!enabled()) return;
        try {
            String body = "{\"subject\":\"" + escapeJson(subject) + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/api/v1/recognition/subjects"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", props.getCompreFace().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(20))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400 && res.statusCode() != 409) {
                log.warn("CompreFace subject create {}: {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.error("CompreFace subject xetasi: {}", e.getMessage());
            throw ApiException.internal("Uz tanima sistemi hazir deyil");
        }
    }

    public void addFace(String subject, byte[] imageBytes) {
        if (!enabled()) return;
        ensureSubject(subject);
        try {
            String boundary = "----Buykon" + UUID.randomUUID();
            byte[] body = buildMultipart(boundary, imageBytes, "face.jpg", "image/jpeg");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/api/v1/recognition/faces?subject=" + urlEncode(subject)))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("x-api-key", props.getCompreFace().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                log.warn("CompreFace addFace {}: {}", res.statusCode(), res.body());
                throw ApiException.badRequest("Uz qeydiyyati alinmadi. Daha aydin sekille yeniden cəhd edin");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("CompreFace addFace xetasi: {}", e.getMessage());
            throw ApiException.internal("Uz qeydiyyati alinmadi");
        }
    }

    public String recognizeSubject(byte[] imageBytes) {
        if (!enabled()) {
            throw ApiException.internal("CompreFace aktiv deyil");
        }
        try {
            String boundary = "----Buykon" + UUID.randomUUID();
            byte[] body = buildMultipart(boundary, imageBytes, "face.jpg", "image/jpeg");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/api/v1/recognition/recognize?limit=1&prediction_count=1"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("x-api-key", props.getCompreFace().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                log.warn("CompreFace recognize {}: {}", res.statusCode(), res.body());
                throw ApiException.unauthorized("Uz taninmadi");
            }
            JsonNode root = JSON.readTree(res.body());
            JsonNode result = root.path("result").isArray() && root.path("result").size() > 0
                    ? root.path("result").get(0) : null;
            if (result == null) throw ApiException.unauthorized("Uz tapilmadi");
            JsonNode subjects = result.path("subjects");
            if (!subjects.isArray() || subjects.isEmpty()) throw ApiException.unauthorized("Uz taninmadi");
            JsonNode best = subjects.get(0);
            double similarity = best.path("similarity").asDouble(0);
            if (similarity < props.getCompreFace().getSimilarityThreshold()) {
                throw ApiException.unauthorized("Uz uygunlasdirilmadi");
            }
            return best.path("subject").asText("");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("CompreFace recognize xetasi: {}", e.getMessage());
            throw ApiException.internal("Uz tanima zamani xeta");
        }
    }

    public byte[] decodeImage(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw ApiException.badRequest("Sekil teleb olunur");
        }
        String data = base64.trim();
        int comma = data.indexOf(',');
        if (comma >= 0) data = data.substring(comma + 1);
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            if (bytes.length < 100) throw ApiException.badRequest("Sekil duzgun deyil");
            if (bytes.length > 5 * 1024 * 1024) throw ApiException.badRequest("Sekil cox boyukdur (max 5MB)");
            return bytes;
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Sekil formati duzgun deyil");
        }
    }

    public static String faceSubjectForOwner(long sellerId) {
        return "buykon_s" + sellerId + "_owner";
    }

    public static String faceSubjectForStaff(long sellerId, long staffId) {
        return "buykon_s" + sellerId + "_st" + staffId;
    }

    private String baseUrl() {
        String url = props.getCompreFace().getUrl();
        if (url == null || url.isBlank()) return "http://localhost:8000";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static byte[] buildMultipart(String boundary, byte[] fileBytes, String filename, String contentType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        out.writeBytes(header.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(fileBytes);
        out.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
