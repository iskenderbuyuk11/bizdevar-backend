package com.bizdevar.seller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SellerStaff {
    public long id;
    public long sellerId;
    public String email;
    public String name;
    public String role;
    public String status;
    public String passwordHash;
    public Set<String> permissions;
    public String inviteToken;
    public String invitedAt;
    public String joinedAt;
    public String usernameSlug;
    public String faceSubject;
    public boolean faceEnrolled;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("email", email);
        m.put("name", name);
        m.put("role", role);
        m.put("status", status);
        if (permissions != null) m.put("permissions", permissions);
        if (invitedAt != null) m.put("invited_at", invitedAt);
        if (joinedAt != null) m.put("joined_at", joinedAt);
        if (usernameSlug != null && !usernameSlug.isBlank()) m.put("username_slug", usernameSlug);
        return m;
    }
}
