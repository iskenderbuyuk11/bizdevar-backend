package com.bizdevar.security;

import com.bizdevar.common.ApiException;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;

/** Request-den cari istifadeçini almaq ucun yardimcilar (RequireAuth/RequireAdmin qarsiligi). */
public final class AuthSupport {

    private AuthSupport() {}

    public static AppUser optional(HttpServletRequest request) {
        Object u = request.getAttribute(CurrentUserFilter.ATTR);
        return u instanceof AppUser ? (AppUser) u : null;
    }

    public static AppUser require(HttpServletRequest request) {
        AppUser u = optional(request);
        if (u == null) {
            throw ApiException.unauthorized("Giris teleb olunur");
        }
        return u;
    }

    public static AppUser requireAdmin(HttpServletRequest request) {
        AppUser u = optional(request);
        if (u == null || !u.admin) {
            throw ApiException.forbidden("Admin icazesi teleb olunur");
        }
        return u;
    }
}
