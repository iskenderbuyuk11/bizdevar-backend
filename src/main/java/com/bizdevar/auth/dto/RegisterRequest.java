package com.bizdevar.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterRequest {
    public String name;
    public String email;
    public String phone;
    public String password;
    @JsonProperty("password_confirm")
    public String passwordConfirm;
}
