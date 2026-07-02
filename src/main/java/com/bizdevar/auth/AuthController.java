package com.bizdevar.auth;

import com.bizdevar.auth.dto.GoogleLoginRequest;
import com.bizdevar.auth.dto.LoginRequest;
import com.bizdevar.auth.dto.RegisterRequest;
import com.bizdevar.auth.dto.SellerAcceptInviteRequest;
import com.bizdevar.auth.dto.SellerFaceRequest;
import com.bizdevar.auth.dto.SellerMemberVerifyRequest;
import com.bizdevar.auth.dto.SellerOtpRequest;
import com.bizdevar.auth.dto.SellerLoginRequest;
import com.bizdevar.auth.dto.SellerSetMemberPasswordRequest;
import com.bizdevar.auth.dto.SellerStoreVerifyRequest;
import com.bizdevar.auth.dto.SellerOtpVerifyRequest;
import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.customer.Customer;
import com.bizdevar.customer.CustomerRepository;
import com.bizdevar.security.AuthPrincipal;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.security.CookieFactory;
import com.bizdevar.security.JwtService;
import com.bizdevar.seller.SellerAccount;
import com.bizdevar.seller.SellerAccountRepository;
import com.bizdevar.user.UserRepository;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final SellerAuthService sellerAuth;
    private final JwtService jwt;
    private final CookieFactory cookies;
    private final SessionMapper session;
    private final CustomerRepository customers;
    private final SellerAccountRepository sellerAccounts;
    private final UserRepository users;

    public AuthController(AuthService auth, SellerAuthService sellerAuth, JwtService jwt, CookieFactory cookies, SessionMapper session,
                          CustomerRepository customers, SellerAccountRepository sellerAccounts,
                          UserRepository users) {
        this.auth = auth;
        this.sellerAuth = sellerAuth;
        this.jwt = jwt;
        this.cookies = cookies;
        this.session = session;
        this.customers = customers;
        this.sellerAccounts = sellerAccounts;
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        Customer c = auth.registerCustomer(req);
        return customerResponse(c);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        Customer c = auth.loginCustomer(req);
        return customerResponse(c);
    }

    @PostMapping("/admin/login")
    @Deprecated
    public ResponseEntity<Map<String, Object>> adminLoginDeprecated() {
        throw com.bizdevar.common.ApiException.badRequest(
                "Kohne admin girisi deaktivdir. /api/auth/admin/request-otp istifade edin");
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> google(@RequestBody GoogleLoginRequest req) {
        Customer c = auth.googleLoginCustomer(req.resolveToken());
        return customerResponse(c);
    }

    @PostMapping("/seller-register")
    public ResponseEntity<Map<String, Object>> sellerRegister(@RequestBody SellerRegisterRequest req) {
        SellerAccount s = auth.sellerRegister(req);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("status", "pending");
        body.put("message", "Magaza qeydiyyatiniz admin tesdiqi ucun gonderildi. Tesdiqden sonra email ile magaza kodunuz gonderilecek.");
        Map<String, Object> seller = new LinkedHashMap<>();
        seller.put("email", s.email);
        seller.put("store_name", s.storeName);
        seller.put("status", s.status);
        body.put("seller", seller);
        return ResponseEntity.status(201).body(body);
    }

    @PostMapping("/seller/verify-store")
    public Map<String, Object> sellerVerifyStore(@RequestBody SellerStoreVerifyRequest req) {
        return sellerAuth.verifyStore(req);
    }

    @PostMapping("/seller/request-otp")
    public ResponseEntity<Map<String, Object>> sellerRequestOtp(@RequestBody SellerOtpRequest req) {
        return sellerStepResponse(sellerAuth.requestOtp(req));
    }

    @PostMapping("/seller/verify-member")
    public ResponseEntity<Map<String, Object>> sellerVerifyMember(@RequestBody SellerMemberVerifyRequest req) {
        return sellerStepResponse(sellerAuth.verifyMember(req));
    }

    @PostMapping("/seller/set-member-password")
    public ResponseEntity<Map<String, Object>> sellerSetMemberPassword(@RequestBody SellerSetMemberPasswordRequest req) {
        return sellerStepResponse(sellerAuth.setMemberPassword(req));
    }

    @PostMapping("/seller/enroll-face")
    public ResponseEntity<Map<String, Object>> sellerEnrollFace(@RequestBody SellerFaceRequest req) {
        return sellerStepResponse(sellerAuth.enrollFace(req));
    }

    @PostMapping("/seller/verify-face")
    public ResponseEntity<Map<String, Object>> sellerVerifyFace(@RequestBody SellerFaceRequest req) {
        return sellerStepResponse(sellerAuth.verifyFace(req));
    }

    @PostMapping("/seller/verify-otp")
    public ResponseEntity<Map<String, Object>> sellerVerifyOtp(@RequestBody SellerOtpVerifyRequest req) {
        return sellerStepResponse(sellerAuth.verifyOtp(req));
    }

    private ResponseEntity<Map<String, Object>> sellerStepResponse(Map<String, Object> result) {
        if (Boolean.TRUE.equals(result.get("logged_in"))) {
            AuthPrincipal p = (AuthPrincipal) result.remove("principal");
            SellerAccount s = (SellerAccount) result.remove("store");
            String redirectPath = result.get("redirect_path") == null ? null : String.valueOf(result.get("redirect_path"));
            ResponseEntity<Map<String, Object>> response = sellerResponse(s, p, false);
            if (redirectPath != null && response.getBody() != null) {
                response.getBody().put("redirect_path", redirectPath);
                response.getBody().putAll(result);
            }
            return response;
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/seller/login")
    public ResponseEntity<Map<String, Object>> sellerLogin(@RequestBody SellerLoginRequest req) {
        AuthPrincipal p = auth.sellerLoginPrincipal(req);
        SellerAccount s = sellerAccounts.findById(p.id)
                .orElseThrow(() -> com.bizdevar.common.ApiException.unauthorized("Magaza tapilmadi"));
        return sellerResponse(s, p, false);
    }

    @PostMapping("/seller/members")
    public Map<String, Object> sellerMembers(@RequestBody Map<String, Object> body) {
        Object email = body == null ? null : body.get("email");
        return auth.sellerMembers(email == null ? "" : String.valueOf(email));
    }

    @PostMapping("/seller/accept-invite")
    public Map<String, Object> acceptStaffInvite(@RequestBody SellerAcceptInviteRequest req) {
        auth.acceptStaffInvite(req.token, req.password, req.name);
        return Map.of("status", "ok", "message", "Devet qebul edildi. Indi giris ede bilersiniz.");
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.clearCustomer().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearSeller().toString())
                .body(session.loggedOut());
    }

    @PostMapping("/seller/logout")
    public ResponseEntity<Map<String, Object>> sellerLogout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.clearSeller().toString())
                .body(session.loggedOut());
    }

    @GetMapping("/session")
    public Map<String, Object> customerSession(HttpServletRequest request) {
        AuthPrincipal p = AuthSupport.optionalPrincipal(request);
        if (p == null || p.role != AuthPrincipal.Role.CUSTOMER) {
            return session.loggedOut();
        }
        return customers.findById(p.id)
                .map(session::customerSession)
                .orElseGet(session::loggedOut);
    }

    @GetMapping("/seller/session")
    public Map<String, Object> sellerSession(HttpServletRequest request) {
        AuthPrincipal p = AuthSupport.optionalPrincipal(request);
        if (p == null || p.role != AuthPrincipal.Role.SELLER) {
            return session.loggedOut();
        }
        return sellerAccounts.findById(p.id)
                .map(s -> session.sellerSession(s, p))
                .orElseGet(session::loggedOut);
    }

    private ResponseEntity<Map<String, Object>> customerResponse(Customer c) {
        String token = jwt.sign(AuthPrincipal.customer(c.id, c.email, c.name));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.createCustomer(token).toString())
                .body(session.customerSession(c));
    }

    private ResponseEntity<Map<String, Object>> sellerResponse(SellerAccount s, AuthPrincipal principal, boolean created) {
        String token = jwt.sign(principal);
        var builder = created ? ResponseEntity.status(201) : ResponseEntity.ok();
        Map<String, Object> body = session.sellerSession(s, principal);
        if (created && s.storeCode != null && !s.storeCode.isBlank()) {
            body.put("store_code", s.storeCode);
            body.put("store_code_warning", "Bu 9 reqemli magaza kodunu hec kimle paylasmayin. Giris zamani bu kod istifade olunur.");
            if (s.storeSlug != null && s.ownerSlug != null) {
                body.put("panel_path", "/" + s.storeSlug + "/sellerpanel/" + s.ownerSlug);
            }
            Object sellerObj = body.get("seller");
            if (sellerObj instanceof Map<?, ?> sellerMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sm = (Map<String, Object>) sellerMap;
                sm.put("store_code", s.storeCode);
            }
        }
        return builder
                .header(HttpHeaders.SET_COOKIE, cookies.createSeller(token).toString())
                .body(body);
    }
}
