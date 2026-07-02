package com.bizdevar.auth;

import com.bizdevar.admin.AdminAccount;
import com.bizdevar.admin.AdminAuthService;
import com.bizdevar.admin.AdminRepository;
import com.bizdevar.auth.dto.AdminEmailRequest;
import com.bizdevar.auth.dto.AdminOtpVerifyRequest;
import com.bizdevar.auth.dto.AdminSetPasswordRequest;
import com.bizdevar.auth.dto.LoginRequest;
import com.bizdevar.security.AuthPrincipal;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.security.CookieFactory;
import com.bizdevar.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuth;
    private final AdminRepository admins;
    private final JwtService jwt;
    private final CookieFactory cookies;
    private final SessionMapper sessionMapper;

    public AdminAuthController(AdminAuthService adminAuth, AdminRepository admins,
                               JwtService jwt, CookieFactory cookies, SessionMapper sessionMapper) {
        this.adminAuth = adminAuth;
        this.admins = admins;
        this.jwt = jwt;
        this.cookies = cookies;
        this.sessionMapper = sessionMapper;
    }

    @PostMapping("/request-otp")
    public Map<String, Object> requestOtp(@RequestBody(required = false) LoginRequest req) {
        if (req == null || req.email == null) {
            throw com.bizdevar.common.ApiException.badRequest("Email daxil edin");
        }
        return adminAuth.requestOtp(req.email, req.password);
    }

    @PostMapping("/check-email")
    public Map<String, Object> checkEmail(@RequestBody(required = false) AdminEmailRequest req) {
        if (req == null || req.email == null) {
            throw com.bizdevar.common.ApiException.badRequest("Email daxil edin");
        }
        return adminAuth.checkEmail(req.email);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody AdminOtpVerifyRequest req) {
        Map<String, Object> result = adminAuth.verifyOtp(req);
        if (Boolean.TRUE.equals(result.get("needs_password_setup"))) {
            return ResponseEntity.ok(result);
        }
        AdminAccount admin = (AdminAccount) result.get("admin");
        return adminSessionResponse(admin);
    }

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody AdminSetPasswordRequest req) {
        AdminAccount admin = adminAuth.setPasswordAndLogin(req);
        return adminSessionResponse(admin);
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true);
    }

    @GetMapping("/session")
    public Map<String, Object> adminSession(HttpServletRequest request) {
        AuthPrincipal p = AuthSupport.optionalPrincipal(request);
        if (p == null || p.role != AuthPrincipal.Role.ADMIN) {
            return sessionMapper.loggedOut();
        }
        return admins.findById(p.id)
                .map(sessionMapper::adminSessionFromAdmin)
                .orElseGet(sessionMapper::loggedOut);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAdmin().toString())
                .body(sessionMapper.loggedOut());
    }

    @GetMapping("/list")
    public Map<String, Object> listAdmins(HttpServletRequest request) {
        AuthSupport.requireAdmin(request);
        List<Map<String, Object>> items = admins.findAll().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id);
            m.put("email", a.email);
            m.put("name", a.name);
            m.put("has_password", a.hasPassword());
            m.put("created_at", a.createdAt != null ? a.createdAt.toString() : null);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    @PostMapping("/invite")
    public Map<String, Object> invite(@RequestBody AdminEmailRequest req, HttpServletRequest request) {
        AuthSupport.requireAdmin(request);
        AdminAccount a = adminAuth.addAdminEmail(req.email, "");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("email", a.email);
        m.put("id", a.id);
        return m;
    }

    private ResponseEntity<Map<String, Object>> adminSessionResponse(AdminAccount admin) {
        String token = jwt.sign(AuthPrincipal.admin(admin.id, admin.email, displayName(admin)));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.createAdmin(token).toString())
                .body(sessionMapper.adminSessionFromAdmin(admin));
    }

    private static String displayName(AdminAccount a) {
        if (a.name != null && !a.name.isBlank()) return a.name;
        int at = a.email.indexOf('@');
        return at > 0 ? a.email.substring(0, at) : a.email;
    }
}
