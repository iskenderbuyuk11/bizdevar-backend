package com.bizdevar.security;

import com.bizdevar.common.ApiException;
import com.bizdevar.seller.SellerPermissions;
import jakarta.servlet.http.HttpServletRequest;

public final class SellerAuth {

    private SellerAuth() {}

    public static SellerContext require(HttpServletRequest request) {
        AuthSupport.requireSeller(request);
        Object ctx = request.getAttribute(SellerContext.ATTR);
        if (ctx instanceof SellerContext sc) return sc;
        AuthPrincipal p = AuthSupport.requireSeller(request);
        return SellerContext.fromPrincipal(p);
    }

    public static SellerContext requirePermission(HttpServletRequest request, String permission) {
        SellerContext ctx = require(request);
        if (!ctx.hasPermission(permission)) {
            throw ApiException.forbidden("Bu bolme ucun icazeniz yoxdur");
        }
        return ctx;
    }

    public static void requireOwner(SellerContext ctx) {
        if (!ctx.owner()) {
            throw ApiException.forbidden("Yalniz magaza sahibi bu emeliyyati ede biler");
        }
    }

    public static void requireStaffManagement(SellerContext ctx) {
        if (!ctx.hasPermission(SellerPermissions.STAFF)) {
            throw ApiException.forbidden("Isci idaresi ucun icazə yoxdur");
        }
    }
}
