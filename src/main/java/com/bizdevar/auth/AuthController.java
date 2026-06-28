package com.bizdevar.auth;

import com.bizdevar.auth.dto.GoogleLoginRequest;
import com.bizdevar.auth.dto.LoginRequest;
import com.bizdevar.auth.dto.RegisterRequest;
import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.security.CookieFactory;
import com.bizdevar.security.JwtService;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;
    private final CookieFactory cookies;
    private final SessionMapper session;

    public AuthController(AuthService auth, JwtService jwt, CookieFactory cookies, SessionMapper session) {
        this.auth = auth;
        this.jwt = jwt;
        this.cookies = cookies;
        this.session = session;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        return sessionResponse(auth.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        return sessionResponse(auth.login(req));
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> google(@RequestBody GoogleLoginRequest req) {
        return sessionResponse(auth.googleLogin(req.resolveToken()));
    }

    @PostMapping("/seller-register")
    public ResponseEntity<Map<String, Object>> sellerRegister(@RequestBody SellerRegisterRequest req) {
        return sessionResponse(auth.sellerRegister(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        ResponseCookie cookie = cookies.clear();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.loggedOut());
    }

    @GetMapping("/session")
    public Map<String, Object> session(HttpServletRequest request) {
        AppUser u = AuthSupport.optional(request);
        return u == null ? session.loggedOut() : session.session(u);
    }

    private ResponseEntity<Map<String, Object>> sessionResponse(AppUser user) {
        String token = jwt.sign(user);
        ResponseCookie cookie = cookies.create(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.session(user));
    }
}
