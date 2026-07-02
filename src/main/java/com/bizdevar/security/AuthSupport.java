package com.bizdevar.security;

import com.bizdevar.common.ApiException;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthSupport {

    private AuthSupport() {}

    public static AuthPrincipal optionalPrincipal(HttpServletRequest request) {
        Object p = request.getAttribute(CurrentUserFilter.ATTR);
        return p instanceof AuthPrincipal ? (AuthPrincipal) p : null;
    }

    public static AuthPrincipal requirePrincipal(HttpServletRequest request) {
        AuthPrincipal p = optionalPrincipal(request);
        if (p == null) throw ApiException.unauthorized("Giris teleb olunur");
        return p;
    }

    public static AuthPrincipal requireCustomer(HttpServletRequest request) {
        AuthPrincipal p = requirePrincipal(request);
        if (p.role != AuthPrincipal.Role.CUSTOMER) {
            throw ApiException.unauthorized("Musteri girisi teleb olunur");
        }
        return p;
    }

    public static AuthPrincipal requireSeller(HttpServletRequest request) {
        AuthPrincipal p = requirePrincipal(request);
        if (p.role != AuthPrincipal.Role.SELLER) {
            throw ApiException.unauthorized("Satici girisi teleb olunur");
        }
        return p;
    }

    public static AuthPrincipal requireAdmin(HttpServletRequest request) {
        AuthPrincipal p = requirePrincipal(request);
        if (p.role != AuthPrincipal.Role.ADMIN || !p.admin) {
            throw ApiException.forbidden("Admin icazesi teleb olunur");
        }
        return p;
    }

    /** Legacy — admin panel and older code. */
    public static AppUser optional(HttpServletRequest request) {
        Object u = request.getAttribute("bizdevar.currentUser");
        if (u instanceof AppUser) return (AppUser) u;
        AuthPrincipal p = optionalPrincipal(request);
        if (p == null) return null;
        AppUser legacy = new AppUser();
        legacy.id = p.id;
        legacy.email = p.email;
        legacy.name = p.name;
        legacy.admin = p.admin;
        legacy.phone = "";
        legacy.passwordHash = "";
        return legacy;
    }

    public static AppUser require(HttpServletRequest request) {
        AuthPrincipal p = requireCustomer(request);
        AppUser u = new AppUser();
        u.id = p.id;
        u.email = p.email;
        u.name = p.name;
        u.admin = false;
        return u;
    }

    public static AppUser requireAdminLegacy(HttpServletRequest request) {
        AuthPrincipal p = requireAdmin(request);
        AppUser u = new AppUser();
        u.id = p.id;
        u.email = p.email;
        u.name = p.name;
        u.admin = true;
        return u;
    }
}
