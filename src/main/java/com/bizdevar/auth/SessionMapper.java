package com.bizdevar.auth;

import com.bizdevar.admin.AdminAccount;
import com.bizdevar.customer.Customer;
import com.bizdevar.security.AuthPrincipal;
import com.bizdevar.seller.SellerAccount;
import com.bizdevar.user.AppUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SessionMapper {

    public Map<String, Object> customerSession(Customer c) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", c.id);
        user.put("name", c.name);
        user.put("email", c.email);
        if (c.phone != null && !c.phone.isBlank()) user.put("phone", c.phone);
        if (c.avatarUrl != null && !c.avatarUrl.isBlank()) user.put("avatar_url", c.avatarUrl);
        user.put("role", "customer");
        user.put("is_admin", false);
        user.put("is_seller", false);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", true);
        m.put("role", "customer");
        m.put("user", user);
        return m;
    }

    public Map<String, Object> sellerSession(SellerAccount s, AuthPrincipal p) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", s.id);
        user.put("email", s.email);
        user.put("store_name", s.storeName);
        user.put("owner_name", s.ownerName);
        user.put("owner_surname", s.ownerSurname);
        user.put("status", s.status);
        if (s.logoUrl != null && !s.logoUrl.isBlank()) user.put("logo_url", s.logoUrl);
        user.put("verification_status", s.verificationStatus);
        user.put("store_type", s.storeType);
        user.put("role", "seller");
        user.put("is_seller", true);
        if (s.rejectionReason != null && !s.rejectionReason.isBlank()) {
            user.put("rejection_reason", s.rejectionReason);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", true);
        m.put("role", "seller");
        m.put("seller", s.toMap());
        m.put("user", user);
        if (p != null && p.staffId != null) {
            m.put("staff", com.bizdevar.security.SellerContext.fromPrincipal(p).toMap());
            user.put("display_name", p.name);
        }
        if (p != null) {
            m.put("permissions", p.staffId == null ? com.bizdevar.seller.SellerPermissions.all() : p.permissions);
        }
        return m;
    }

    /** @deprecated use sellerSession(SellerAccount, AuthPrincipal) */
    public Map<String, Object> sellerSession(SellerAccount s) {
        return sellerSession(s, AuthPrincipal.sellerOwner(s.id, s.email, s.storeName));
    }

    public Map<String, Object> adminSessionFromAdmin(AdminAccount a) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", a.id);
        user.put("name", a.name != null && !a.name.isBlank() ? a.name : a.email);
        user.put("email", a.email);
        user.put("is_admin", true);
        user.put("role", "admin");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", true);
        m.put("role", "admin");
        m.put("user", user);
        return m;
    }

    public Map<String, Object> adminSession(AppUser u) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", u.id);
        user.put("name", u.name);
        user.put("email", u.email);
        user.put("is_admin", true);
        user.put("role", "admin");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", true);
        m.put("role", "admin");
        m.put("user", user);
        return m;
    }

    public Map<String, Object> loggedOut() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logged_in", false);
        return m;
    }
}
