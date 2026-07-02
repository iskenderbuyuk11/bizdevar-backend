package com.bizdevar.security;

import com.bizdevar.seller.SellerPermissions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SellerContext {

    public static final String ATTR = "buykon.seller.context";

    public final long sellerId;
    public final Long staffId;
    public final String staffRole;
    public final String displayName;
    public final Set<String> permissions;

    public SellerContext(long sellerId, Long staffId, String staffRole, String displayName, Set<String> permissions) {
        this.sellerId = sellerId;
        this.staffId = staffId;
        this.staffRole = staffRole;
        this.displayName = displayName;
        this.permissions = permissions;
    }

    public static SellerContext owner(long sellerId, String storeName) {
        return new SellerContext(sellerId, null, "owner", storeName, SellerPermissions.all());
    }

    public static SellerContext staff(long sellerId, long staffId, String role, String name, Set<String> permissions) {
        return new SellerContext(sellerId, staffId, role, name, permissions);
    }

    public static SellerContext fromPrincipal(AuthPrincipal p) {
        if (p.staffId != null) {
            return new SellerContext(p.id, p.staffId, p.staffRole, p.name, p.permissions);
        }
        return owner(p.id, p.name);
    }

    public boolean owner() {
        return staffId == null || "owner".equals(staffRole);
    }

    public boolean hasPermission(String key) {
        return owner() || SellerPermissions.can(permissions, key);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seller_id", sellerId);
        m.put("display_name", displayName);
        m.put("role", staffRole == null ? "owner" : staffRole);
        if (staffId != null) m.put("staff_id", staffId);
        if (!owner()) m.put("permissions", permissions);
        return m;
    }
}
