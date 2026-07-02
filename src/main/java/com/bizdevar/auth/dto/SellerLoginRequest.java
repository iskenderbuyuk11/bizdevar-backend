package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerLoginRequest {
    public String email;
    public String password;
    @JsonProperty("member_id")
    public String memberId;
}
