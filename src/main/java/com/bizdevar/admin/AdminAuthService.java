package com.bizdevar.admin;

import com.bizdevar.auth.dto.AdminOtpVerifyRequest;
import com.bizdevar.auth.dto.AdminSetPasswordRequest;
import com.bizdevar.auth.dto.LoginRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.mail.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminAuthService {

    private static final int OTP_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminRepository admins;
    private final AdminOtpRepository otps;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    public AdminAuthService(AdminRepository admins, AdminOtpRepository otps,
                            PasswordEncoder encoder, EmailService emailService) {
        this.admins = admins;
        this.otps = otps;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    public Map<String, Object> checkEmail(String emailRaw) {
        String email = normalizeEmail(emailRaw);
        AdminAccount admin = admins.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Bu email admin siyahısında deyil"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("email", email);
        m.put("needs_password", !admin.hasPassword());
        return m;
    }

    /** İlk giriş: yalnız email. Sonrakı girişlər: email + şifrə. */
    public Map<String, Object> requestOtp(String emailRaw, String password) {
        String email = normalizeEmail(emailRaw);
        AdminAccount admin = admins.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Bu email admin siyahısında deyil"));

        if (admin.hasPassword()) {
            if (password == null || password.isBlank()) {
                throw ApiException.badRequest("Şifrə daxil edin");
            }
            if (admin.passwordHash == null || !encoder.matches(password, admin.passwordHash)) {
                throw ApiException.unauthorized("Email və ya şifrə yanlışdır");
            }
        }

        try {
            String code = generateOtp();
            String purpose = admin.hasPassword() ? "login" : "setup";
            otps.invalidateForAdmin(admin.id);
            otps.insert(admin.id, encoder.encode(code), purpose, Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
            emailService.sendAdminOtp(email, code);
        } catch (Exception e) {
            throw ApiException.internal("OTP yaradila bilmedi — admin_otps cedvelini yoxlayin");
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("otp_sent", true);
        m.put("expires_in", OTP_TTL_MINUTES * 60);
        m.put("needs_password_setup", !admin.hasPassword());
        return m;
    }

    public Map<String, Object> verifyOtp(AdminOtpVerifyRequest req) {
        String email = normalizeEmail(req.email);
        String code = trim(req.code);
        if (code.length() < 4) throw ApiException.badRequest("OTP kodu daxil edin");

        AdminAccount admin = admins.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Admin tapılmadı"));

        String purpose = admin.hasPassword() ? "login" : "setup";
        AdminOtpRepository.OtpRow row = otps.findLatestValid(admin.id, purpose)
                .orElseThrow(() -> ApiException.badRequest("OTP kodu etibarsızdır və ya vaxtı keçib"));

        if (!encoder.matches(code, row.codeHash)) {
            throw ApiException.badRequest("OTP kodu yanlışdır");
        }

        if (!admin.hasPassword()) {
            otps.markVerified(row.id);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ok", true);
            m.put("needs_password_setup", true);
            m.put("email", email);
            return m;
        }

        otps.delete(row.id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("admin", admin);
        return m;
    }

    public AdminAccount setPasswordAndLogin(AdminSetPasswordRequest req) {
        String email = normalizeEmail(req.email);
        String code = trim(req.code);
        if (req.password == null || req.password.length() < 8) {
            throw ApiException.badRequest("Şifrə ən azı 8 simvol olmalıdır");
        }
        if (!req.password.equals(req.passwordConfirm)) {
            throw ApiException.badRequest("Şifrələr uyğun gəlmir");
        }

        AdminAccount admin = admins.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Admin tapılmadı"));
        if (admin.hasPassword()) {
            throw ApiException.badRequest("Şifrə artıq təyin edilib");
        }

        AdminOtpRepository.OtpRow row = otps.findLatestValid(admin.id, "setup")
                .orElseThrow(() -> ApiException.badRequest("OTP təsdiqi tapılmadı — yenidən kod istəyin"));

        if (row.verifiedAt == null) {
            if (!encoder.matches(code, row.codeHash)) {
                throw ApiException.badRequest("OTP kodu yanlışdır");
            }
        } else if (!encoder.matches(code, row.codeHash)) {
            throw ApiException.badRequest("OTP kodu yanlışdır");
        }

        String hash = encoder.encode(req.password);
        admins.setPassword(admin.id, hash);
        otps.delete(row.id);
        return admins.findById(admin.id).orElse(admin);
    }

    public AdminAccount addAdminEmail(String emailRaw, String name) {
        String email = normalizeEmail(emailRaw);
        if (admins.existsByEmail(email)) {
            throw ApiException.conflict("Bu email artıq admin siyahısındadır");
        }
        long id = admins.insert(email, name != null ? name.trim() : "");
        return admins.findById(id).orElseThrow(() -> ApiException.internal("Admin yaradıla bilmədi"));
    }

    private String generateOtp() {
        int n = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(n);
    }

    private static String normalizeEmail(String email) {
        if (email == null) throw ApiException.badRequest("Email daxil edin");
        String e = email.trim().toLowerCase();
        if (!e.contains("@") || e.length() < 5) throw ApiException.badRequest("Düzgün email daxil edin");
        return e;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
