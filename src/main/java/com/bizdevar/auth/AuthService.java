package com.bizdevar.auth;

import com.bizdevar.auth.dto.LoginRequest;
import com.bizdevar.auth.dto.RegisterRequest;
import com.bizdevar.auth.dto.SellerLoginRequest;
import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.customer.Customer;
import com.bizdevar.customer.CustomerRepository;
import com.bizdevar.security.AuthPrincipal;
import com.bizdevar.seller.SellerAccount;
import com.bizdevar.seller.SellerAccountRepository;
import com.bizdevar.seller.SellerStaff;
import com.bizdevar.seller.SellerStaffRepository;
import com.bizdevar.user.AppUser;
import com.bizdevar.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final CustomerRepository customers;
    private final SellerAccountRepository sellerAccounts;
    private final SellerStaffRepository sellerStaff;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final GoogleVerifier google;

    public AuthService(CustomerRepository customers, SellerAccountRepository sellerAccounts,
                       SellerStaffRepository sellerStaff, UserRepository users,
                       PasswordEncoder encoder, GoogleVerifier google) {
        this.customers = customers;
        this.sellerAccounts = sellerAccounts;
        this.sellerStaff = sellerStaff;
        this.users = users;
        this.encoder = encoder;
        this.google = google;
    }

    public Customer registerCustomer(RegisterRequest req) {
        String name = trim(req.name);
        String email = trim(req.email);
        String phone = trim(req.phone);

        if (name.length() < 2) throw ApiException.badRequest("Ad ve soyad en azi 2 simvol olmalidir");
        if (!validEmail(email)) throw ApiException.badRequest("Duzgun email daxil edin");
        if (req.password == null || req.password.length() < 6) throw ApiException.badRequest("Sifre en azi 6 simvol olmalidir");
        if (!req.password.equals(req.passwordConfirm)) throw ApiException.badRequest("Sifreler uygun gelmir");
        if (customers.existsByEmail(email)) throw ApiException.conflict("Bu email artiq qeydiyyatdan kecib");

        String hash = encoder.encode(req.password);
        long id = customers.insert(name, email, phone, hash, null, null);
        return customers.findById(id).orElseThrow(() -> ApiException.internal("Qeydiyyat zamani xeta bas verdi"));
    }

    public Customer loginCustomer(LoginRequest req) {
        String email = trim(req.email);
        if (!validEmail(email) || req.password == null || req.password.isBlank()) {
            throw ApiException.badRequest("Email ve sifre teleb olunur");
        }
        Customer c = customers.findByEmail(email).orElse(null);
        if (c == null || c.passwordHash == null || c.passwordHash.isBlank()
                || !encoder.matches(req.password, c.passwordHash)) {
            throw ApiException.unauthorized("Email ve ya sifre yanlisdir");
        }
        return c;
    }

    public AppUser loginAdmin(LoginRequest req) {
        String email = trim(req.email);
        AppUser u = users.findByEmail(email).orElse(null);
        if (u == null || !u.admin || u.passwordHash == null || !encoder.matches(req.password, u.passwordHash)) {
            throw ApiException.unauthorized("Admin girisi ugursuz");
        }
        return u;
    }

    public Customer googleLoginCustomer(String idToken) {
        GoogleVerifier.GoogleUser g = google.verify(idToken);
        Customer existing = customers.findByEmail(g.email()).orElse(null);
        if (existing != null) {
            if (existing.googleId == null || existing.googleId.isBlank()) {
                customers.setGoogleId(existing.id, g.sub(), emptyToNull(g.picture()));
            }
            return customers.findById(existing.id).orElse(existing);
        }
        String name = (g.name() == null || g.name().isBlank()) ? g.email().split("@")[0] : g.name();
        long id = customers.insert(name, g.email(), "", "", g.sub(), emptyToNull(g.picture()));
        return customers.findById(id).orElseThrow(() -> ApiException.internal("Google ile qeydiyyat alinmadi"));
    }

    public SellerAccount sellerRegister(SellerRegisterRequest req) {
        req.email = trim(req.email);
        req.phone = trim(req.phone);
        if (req.password == null || !req.password.equals(req.passwordConfirm)) {
            throw ApiException.badRequest("Sifreler uygun gelmir");
        }
        if (req.password.length() < 6) throw ApiException.badRequest("Sifre en azi 6 simvol olmalidir");
        if (!validEmail(req.email)) throw ApiException.badRequest("Duzgun email daxil edin");
        String hash = encoder.encode(req.password);
        return sellerAccounts.register(req, hash);
    }

    public SellerAccount sellerLogin(SellerLoginRequest req) {
        AuthPrincipal principal = sellerLoginPrincipal(req);
        if (principal.staffId != null) {
            return sellerAccounts.findById(principal.id)
                    .orElseThrow(() -> ApiException.unauthorized("Magaza tapilmadi"));
        }
        return sellerAccounts.findById(principal.id)
                .orElseThrow(() -> ApiException.unauthorized("Magaza tapilmadi"));
    }

    public AuthPrincipal sellerLoginPrincipal(SellerLoginRequest req) {
        String email = trim(req.email);
        if (!validEmail(email) || req.password == null || req.password.isBlank()) {
            throw ApiException.badRequest("Email, secim ve sifre teleb olunur");
        }
        SellerAccount s = sellerAccounts.findByEmail(email).orElse(null);
        if (s == null) throw ApiException.unauthorized("Magaza tapilmadi");
        if ("deleted".equals(s.status)) throw ApiException.forbidden("Magaza silinib");

        String memberId = req.memberId == null || req.memberId.isBlank() ? "owner" : req.memberId.trim();
        if ("owner".equals(memberId)) {
            if (s.passwordHash == null || !encoder.matches(req.password, s.passwordHash)) {
                throw ApiException.unauthorized("Sifre yanlisdir");
            }
            return AuthPrincipal.sellerOwner(s.id, s.email, s.storeName);
        }

        long staffId;
        try {
            staffId = Long.parseLong(memberId);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Secim duzgun deyil");
        }
        SellerStaff staff = sellerStaff.findById(staffId).orElse(null);
        if (staff == null || staff.sellerId != s.id || !"active".equals(staff.status)) {
            throw ApiException.unauthorized("Isci tapilmadi");
        }
        if (!sellerStaff.verifyStaffPassword(staffId, req.password)) {
            throw ApiException.unauthorized("Sifre yanlisdir");
        }
        return AuthPrincipal.sellerStaff(s.id, staff.email, staff.name, staff.id, staff.role, staff.permissions);
    }

    public Map<String, Object> sellerMembers(String emailRaw) {
        String email = trim(emailRaw);
        if (!validEmail(email)) throw ApiException.badRequest("Duzgun email daxil edin");
        SellerAccount s = sellerAccounts.findByEmail(email).orElse(null);
        if (s == null) throw ApiException.notFound("Magaza tapilmadi");
        if ("deleted".equals(s.status)) throw ApiException.forbidden("Magaza silinib");
        String ownerName = (trim(s.ownerName) + " " + trim(s.ownerSurname)).trim();
        if (ownerName.isBlank()) ownerName = s.storeName;
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("store_name", s.storeName);
        out.put("members", sellerStaff.listMembersForLogin(s.id, ownerName));
        return out;
    }

    public void acceptStaffInvite(String token, String password, String name) {
        sellerStaff.acceptInvite(token, password, name);
    }

    /** @deprecated use sellerLogin(SellerLoginRequest) */
    public SellerAccount sellerLogin(LoginRequest req) {
        SellerLoginRequest r = new SellerLoginRequest();
        r.email = req.email;
        r.password = req.password;
        r.memberId = "owner";
        return sellerLogin(r);
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static boolean validEmail(String email) {
        if (email == null || !email.contains("@")) return false;
        String[] parts = email.split("@");
        return parts.length == 2 && !parts[0].isEmpty() && parts[1].contains(".");
    }
}
