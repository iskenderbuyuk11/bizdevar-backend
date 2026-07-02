package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerOtpVerifyRequest {
    @JsonProperty("challenge_token")
    public String challengeToken;
    public String code;
}
