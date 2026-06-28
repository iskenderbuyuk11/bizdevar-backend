package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellerRegisterRequest {
    public String email;
    public String password;
    @JsonProperty("password_confirm")
    public String passwordConfirm;
    public String phone;
    @JsonProperty("owner_name")
    public String ownerName;
    @JsonProperty("owner_surname")
    public String ownerSurname;
    @JsonProperty("store_name")
    public String storeName;
    public String category;
    @JsonProperty("store_type")
    public String storeType;
    public String voen;
}
