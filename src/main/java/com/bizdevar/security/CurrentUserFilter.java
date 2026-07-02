package com.bizdevar.security;

import com.bizdevar.admin.AdminRepository;
import com.bizdevar.config.AppProperties;
import com.bizdevar.customer.CustomerRepository;
import com.bizdevar.seller.SellerAccountRepository;
import com.bizdevar.seller.SellerStaffRepository;
import com.bizdevar.user.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class CurrentUserFilter extends OncePerRequestFilter {

    public static final String ATTR = "buykon.auth";

    private final JwtService jwt;
    private final CustomerRepository customers;
    private final SellerAccountRepository sellers;
    private final SellerStaffRepository sellerStaff;
    private final AdminRepository admins;
    private final String customerCookie;
    private final String sellerCookie;
    private final String adminCookie;

    public CurrentUserFilter(JwtService jwt, CustomerRepository customers, SellerAccountRepository sellers,
                             SellerStaffRepository sellerStaff, AdminRepository admins, AppProperties props) {
        this.jwt = jwt;
        this.customers = customers;
        this.sellers = sellers;
        this.sellerStaff = sellerStaff;
        this.admins = admins;
        this.customerCookie = props.getCookie().getName();
        this.sellerCookie = props.getCookie().getSellerName();
        String admin = props.getCookie().getAdminName();
        this.adminCookie = (admin == null || admin.isBlank()) ? "buykon_admin_token" : admin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        AuthPrincipal principal = null;

        if (path.startsWith("/api/admin") || path.startsWith("/api/auth/admin")) {
            principal = resolveAdmin(readCookie(request, adminCookie));
        } else if (path.startsWith("/api/seller") || path.equals("/api/auth/seller/session")) {
            principal = resolveSeller(readCookie(request, sellerCookie));
        } else if (path.startsWith("/api/auth/session")) {
            principal = resolveCustomer(readCookie(request, customerCookie));
        } else {
            principal = resolveCustomer(readCookie(request, customerCookie));
        }

        if (principal != null) {
            request.setAttribute(ATTR, principal);
            if (principal.role == AuthPrincipal.Role.SELLER) {
                request.setAttribute(SellerContext.ATTR, SellerContext.fromPrincipal(principal));
            }
            if (path.startsWith("/api/admin") || path.startsWith("/api/auth")) {
                request.setAttribute("bizdevar.currentUser", legacyUser(principal));
            }
        }
        chain.doFilter(request, response);
    }

    private AuthPrincipal resolveCustomer(String token) {
        if (token == null || token.isBlank()) return null;
        JwtService.TokenPayload p = jwt.parse(token);
        if (p == null || p.role() != AuthPrincipal.Role.CUSTOMER) return null;
        return customers.findById(p.id())
                .map(c -> AuthPrincipal.customer(c.id, c.email, c.name))
                .orElse(null);
    }

    private AuthPrincipal resolveSeller(String token) {
        if (token == null || token.isBlank()) return null;
        JwtService.TokenPayload p = jwt.parse(token);
        if (p == null || p.role() != AuthPrincipal.Role.SELLER) return null;
        if (p.staffId() != null) {
            return sellerStaff.findById(p.staffId())
                    .filter(st -> "active".equals(st.status) && st.sellerId == p.id())
                    .map(st -> AuthPrincipal.sellerStaff(
                            p.id(), st.email != null && !st.email.isBlank() ? st.email : "",
                            st.name, st.id, st.role, st.permissions))
                    .orElse(null);
        }
        return sellers.findById(p.id())
                .filter(s -> !"deleted".equals(s.status))
                .map(s -> AuthPrincipal.sellerOwner(s.id, s.email, s.storeName))
                .orElse(null);
    }

    private AuthPrincipal resolveAdmin(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            JwtService.TokenPayload p = jwt.parse(token);
            if (p == null || p.role() != AuthPrincipal.Role.ADMIN) return null;
            return admins.findById(p.id())
                    .map(a -> AuthPrincipal.admin(a.id, a.email,
                            a.name != null && !a.name.isBlank() ? a.name : a.email))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private AppUser legacyUser(AuthPrincipal p) {
        AppUser u = new AppUser();
        u.id = p.id;
        u.email = p.email;
        u.name = p.name;
        u.admin = p.admin;
        u.phone = "";
        u.passwordHash = "";
        return u;
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (name == null || name.isBlank() || request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
