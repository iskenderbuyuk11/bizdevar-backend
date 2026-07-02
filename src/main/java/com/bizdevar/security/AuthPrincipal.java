package com.bizdevar.security;

import com.bizdevar.seller.SellerPermissions;

import java.util.Set;

public class AuthPrincipal {

    public enum Role { CUSTOMER, SELLER, ADMIN }

    public final Role role;
    public final long id;
    public final String email;
    public final String name;
    public final boolean admin;
    public final Long staffId;
    public final String staffRole;
    public final Set<String> permissions;

    private AuthPrincipal(Role role, long id, String email, String name, boolean admin,
                          Long staffId, String staffRole, Set<String> permissions) {
        this.role = role;
        this.id = id;
        this.email = email;
        this.name = name;
        this.admin = admin;
        this.staffId = staffId;
        this.staffRole = staffRole;
        this.permissions = permissions;
    }

    public static AuthPrincipal customer(long id, String email, String name) {
        return new AuthPrincipal(Role.CUSTOMER, id, email, name, false, null, null, Set.of());
    }

    public static AuthPrincipal sellerOwner(long id, String email, String storeName) {
        return new AuthPrincipal(Role.SELLER, id, email, storeName, false, null, "owner", SellerPermissions.all());
    }

    public static AuthPrincipal sellerStaff(long sellerId, String email, String staffName, long staffId,
                                            String staffRole, Set<String> permissions) {
        return new AuthPrincipal(Role.SELLER, sellerId, email, staffName, false, staffId, staffRole, permissions);
    }

    /** @deprecated use sellerOwner */
    public static AuthPrincipal seller(long id, String email, String storeName) {
        return sellerOwner(id, email, storeName);
    }

    public static AuthPrincipal admin(long id, String email, String name) {
        return new AuthPrincipal(Role.ADMIN, id, email, name, true, null, null, Set.of());
    }
}
