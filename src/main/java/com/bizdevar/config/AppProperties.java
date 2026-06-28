package com.bizdevar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bizdevar")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private Google google = new Google();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Cookie getCookie() { return cookie; }
    public void setCookie(Cookie cookie) { this.cookie = cookie; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }

    public static class Jwt {
        private String secret;
        private int ttlDays = 7;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getTtlDays() { return ttlDays; }
        public void setTtlDays(int ttlDays) { this.ttlDays = ttlDays; }
    }

    public static class Cookie {
        private String name = "bizdevar_token";
        private boolean secure = false;
        private String sameSite = "Lax";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }
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
}
