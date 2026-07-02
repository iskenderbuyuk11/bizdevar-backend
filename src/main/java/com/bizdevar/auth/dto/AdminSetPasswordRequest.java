package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminSetPasswordRequest {
    public String email;
    public String code;
    public String password;
    @JsonProperty("password_confirm")
    public String passwordConfirm;
}
