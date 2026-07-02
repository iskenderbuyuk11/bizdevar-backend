package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerOtpRequest {
    @JsonProperty("store_code")
    public String storeCode;
    @JsonProperty("member_id")
    public String memberId;
    public String password;
}
