package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerStoreVerifyRequest {
    @JsonProperty("store_code")
    public String storeCode;
    @JsonProperty("store_password")
    public String storePassword;
}
