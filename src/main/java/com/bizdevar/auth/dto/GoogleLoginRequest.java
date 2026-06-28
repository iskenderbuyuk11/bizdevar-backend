package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleLoginRequest {
    // Google Identity Services "credential" (ID token JWT).
    public String credential;
    @JsonProperty("id_token")
    public String idToken;
    public String token;

    public String resolveToken() {
        if (credential != null && !credential.isBlank()) return credential;
        if (idToken != null && !idToken.isBlank()) return idToken;
        return token;
    }
}
