package com.bizdevar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bizdevar")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private Google google = new Google();
    private Mail mail = new Mail();
    private CompreFace compreFace = new CompreFace();
    private String frontendUrl = "https://buykon.com";

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Cookie getCookie() { return cookie; }
    public void setCookie(Cookie cookie) { this.cookie = cookie; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public CompreFace getCompreFace() { return compreFace; }
    public void setCompreFace(CompreFace compreFace) { this.compreFace = compreFace; }
    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }

    public static class Jwt {
        private String secret;
        private int ttlDays = 7;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getTtlDays() { return ttlDays; }
        public void setTtlDays(int ttlDays) { this.ttlDays = ttlDays; }
    }

    public static class Cookie {
        private String name = "buykon_token";
        private String sellerName = "buykon_seller_token";
        private String adminName = "buykon_admin_token";
        private boolean secure = false;
        private String sameSite = "Lax";
        private String domain = "";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSellerName() { return sellerName; }
        public void setSellerName(String sellerName) { this.sellerName = sellerName; }
        public String getAdminName() { return adminName; }
        public void setAdminName(String adminName) { this.adminName = adminName; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
    }

    public static class Cors {
        private List<String> origins = List.of();
        public List<String> getOrigins() { return origins; }
        public void setOrigins(List<String> origins) { this.origins = origins; }
    }

    public static class Google {
        private String clientId = "";
        private String clientSecret = "";
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    public static class Mail {
        private String from = "noreply@buykon.com";
        private boolean devLogOtp = true;
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public boolean isDevLogOtp() { return devLogOtp; }
        public void setDevLogOtp(boolean devLogOtp) { this.devLogOtp = devLogOtp; }
    }

    public static class CompreFace {
        private boolean enabled = false;
        private String url = "http://localhost:8000";
        private String apiKey = "";
        private double similarityThreshold = 0.85;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    }
}
