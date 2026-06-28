package com.bizdevar.auth;

import com.bizdevar.user.AppUser;
import com.bizdevar.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SessionMapper {

    private final UserRepository users;

    public SessionMapper(UserRepository users) {
        this.users = users;
    }

    public Map<String, Object> publicUser(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id);
        m.put("name", u.name);
        m.put("email", u.email);
        if (u.phone != null && !u.phone.isBlank()) m.put("phone", u.phone);
        m.put("is_admin", u.admin);
        if (u.avatarUrl != null && !u.avatarUrl.isBlank()) m.put("avatar_url", u.avatarUrl);

        UserRepository.VendorSummary v = users.vendorForUser(u.id);
        boolean isSeller = v != null;
        m.put("is_seller", isSeller);
        if (isSeller) {
            m.put("vendor_id", v.id());
            m.put("store_name", v.name());
            m.put("vendor_status", v.status());
        }
        return m;
    }

    public Map<String, Object> session(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", true);
        m.put("user", publicUser(u));
        return m;
    }

    public Map<String, Object> loggedOut() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", false);
        return m;
    }
}
