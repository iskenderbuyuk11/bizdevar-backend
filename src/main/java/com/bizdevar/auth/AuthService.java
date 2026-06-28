package com.bizdevar.auth;

import com.bizdevar.auth.dto.LoginRequest;
import com.bizdevar.auth.dto.RegisterRequest;
import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.seller.SellerRepository;
import com.bizdevar.user.AppUser;
import com.bizdevar.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final SellerRepository sellers;
    private final PasswordEncoder encoder;
    private final GoogleVerifier google;

    public AuthService(UserRepository users, SellerRepository sellers, PasswordEncoder encoder, GoogleVerifier google) {
        this.users = users;
        this.sellers = sellers;
        this.encoder = encoder;
        this.google = google;
    }

    public AppUser register(RegisterRequest req) {
        String name = trim(req.name);
        String email = trim(req.email);
        String phone = trim(req.phone);

        if (name.length() < 2) throw ApiException.badRequest("Ad ve soyad en azi 2 simvol olmalidir");
        if (!validEmail(email)) throw ApiException.badRequest("Duzgun email daxil edin");
        if (req.password == null || req.password.length() < 6) throw ApiException.badRequest("Sifre en azi 6 simvol olmalidir");
        if (!req.password.equals(req.passwordConfirm)) throw ApiException.badRequest("Sifreler uygun gelmir");
        if (users.existsByEmail(email)) throw ApiException.conflict("Bu email artiq qeydiyyatdan kecib");

        String hash = encoder.encode(req.password);
        long id = users.insert(name, email, phone, hash, false, null, null);
        return users.findById(id).orElseThrow(() -> ApiException.internal("Qeydiyyat zamani xeta bas verdi"));
    }

    public AppUser login(LoginRequest req) {
        String email = trim(req.email);
        if (!validEmail(email) || req.password == null || req.password.isEmpty()) {
            throw ApiException.badRequest("Email ve sifre teleb olunur");
        }
        AppUser u = users.findByEmail(email).orElse(null);
        if (u == null || u.passwordHash == null || u.passwordHash.isBlank()
                || !encoder.matches(req.password, u.passwordHash)) {
            throw ApiException.unauthorized("Email ve ya sifre yanlisdir");
        }
        return u;
    }

    public AppUser googleLogin(String idToken) {
        GoogleVerifier.GoogleUser g = google.verify(idToken);
        AppUser existing = users.findByEmail(g.email()).orElse(null);
        if (existing != null) {
            if (existing.googleId == null || existing.googleId.isBlank()) {
                users.setGoogleId(existing.id, g.sub(), emptyToNull(g.picture()));
            }
            return existing;
        }
        String name = (g.name() == null || g.name().isBlank()) ? g.email().split("@")[0] : g.name();
        long id = users.insert(name, g.email(), "", "", false, g.sub(), emptyToNull(g.picture()));
        return users.findById(id).orElseThrow(() -> ApiException.internal("Google ile qeydiyyat alinmadi"));
    }

    public AppUser sellerRegister(SellerRegisterRequest req) {
        req.email = trim(req.email);
        req.phone = trim(req.phone);
        if (req.password == null || !req.password.equals(req.passwordConfirm)) {
            throw ApiException.badRequest("Sifreler uygun gelmir");
        }
        if (req.password.length() < 6) throw ApiException.badRequest("Sifre en azi 6 simvol olmalidir");
        if (!validEmail(req.email)) throw ApiException.badRequest("Duzgun email daxil edin");

        String ownerName = trim(req.ownerName);
        String ownerSurname = trim(req.ownerSurname);
        String fullName = (ownerName + " " + ownerSurname).trim();
        if (fullName.isEmpty()) {
            String prefix = req.email.contains("@") ? req.email.split("@")[0] : "";
            fullName = prefix.isEmpty() ? "Satici" : "Satici-" + prefix;
        }
        if (users.existsByEmail(req.email)) {
            throw ApiException.conflict("Bu email artiq qeydiyyatdan kecib. Daxil olub magaza yaradin.");
        }
        String hash = encoder.encode(req.password);
        long id = users.insert(fullName, req.email, req.phone, hash, false, null, null);
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.internal("Qeydiyyat xetasi"));
        sellers.createVendor(u.id, req);
        return u;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static boolean validEmail(String email) {
        if (email == null || !email.contains("@")) return false;
        String[] parts = email.split("@");
        return parts.length == 2 && !parts[0].isEmpty() && parts[1].contains(".");
    }
}
