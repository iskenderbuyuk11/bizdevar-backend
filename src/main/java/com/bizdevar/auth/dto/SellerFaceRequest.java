package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerFaceRequest {
    @JsonProperty("challenge_token")
    public String challengeToken;
    @JsonProperty("image_base64")
    public String imageBase64;
}
