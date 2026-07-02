package com.bizdevar.admin;

import java.time.Instant;

public class AdminAccount {
    public long id;
    public String email;
    public String name;
    public String passwordHash;
    public boolean active;
    public Instant createdAt;

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
