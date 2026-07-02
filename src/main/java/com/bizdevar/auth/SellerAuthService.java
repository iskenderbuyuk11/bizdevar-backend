package com.bizdevar.auth;

import com.bizdevar.auth.dto.SellerFaceRequest;
import com.bizdevar.auth.dto.SellerMemberVerifyRequest;
import com.bizdevar.auth.dto.SellerOtpRequest;
import com.bizdevar.auth.dto.SellerOtpVerifyRequest;
import com.bizdevar.auth.dto.SellerSetMemberPasswordRequest;
import com.bizdevar.auth.dto.SellerStoreVerifyRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.common.NameMaskUtil;
import com.bizdevar.compreface.CompreFaceService;
import com.bizdevar.mail.EmailService;
import com.bizdevar.security.AuthPrincipal;
import com.bizdevar.seller.SellerAccount;
import com.bizdevar.seller.SellerAccountRepository;
import com.bizdevar.seller.SellerFaceChallengeRepository;
import com.bizdevar.seller.SellerLoginOtpRepository;
import com.bizdevar.seller.SellerStaff;
import com.bizdevar.seller.SellerStaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SellerAuthService {

    private static final int CHALLENGE_TTL_MINUTES = 15;
    private static final int OTP_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SellerAccountRepository accounts;
    private final SellerStaffRepository staff;
    private final SellerLoginOtpRepository otps;
    private final SellerFaceChallengeRepository faceChallenges;
    private final CompreFaceService compreFace;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    public SellerAuthService(SellerAccountRepository accounts, SellerStaffRepository staff,
                             SellerLoginOtpRepository otps, SellerFaceChallengeRepository faceChallenges,
                             CompreFaceService compreFace, PasswordEncoder encoder, EmailService emailService) {
        this.accounts = accounts;
        this.staff = staff;
        this.otps = otps;
        this.faceChallenges = faceChallenges;
        this.compreFace = compreFace;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    public Map<String, Object> verifyStore(SellerStoreVerifyRequest req) {
        SellerAccount s = requireStore(req.storeCode);
        if ("deleted".equals(s.status)) throw ApiException.forbidden("Magaza silinib");
        if (!"active".equals(s.status)) {
            throw ApiException.forbidden("Magaza hele tesdiq gozleyir");
        }
        String password = trim(req.storePassword);
        if (password.isBlank()) throw ApiException.badRequest("Magaza sifresi daxil edin");
        if (s.passwordHash == null || !encoder.matches(password, s.passwordHash)) {
            throw ApiException.unauthorized("Magaza kodu ve ya sifre yanlisdir");
        }

        String ownerName = (trim(s.ownerName) + " " + trim(s.ownerSurname)).trim();
        if (ownerName.isBlank()) ownerName = s.storeName;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("store_name", s.storeName);
        out.put("store_slug", s.storeSlug);
        out.put("face_auth", compreFace.enabled());
        out.put("members", staff.listMaskedMembersForLogin(s.id, ownerName, s.ownerSlug, s));
        return out;
    }

    public Map<String, Object> verifyMember(SellerMemberVerifyRequest req) {
        SellerAccount s = requireStore(req.storeCode);
        if (!"active".equals(s.status)) throw ApiException.forbidden("Magaza aktiv deyil");

        String memberId = req.memberId == null || req.memberId.isBlank() ? "owner" : req.memberId.trim();
        String password = trim(req.password);

        if ("owner".equals(memberId)) {
            if (s.ownerLoginPasswordHash == null || s.ownerLoginPasswordHash.isBlank()) {
                throw ApiException.badRequest("Ilk giris ucun sifre teyin edin");
            }
            if (password.isBlank() || !encoder.matches(password, s.ownerLoginPasswordHash)) {
                throw ApiException.unauthorized("Sifre yanlisdir");
            }
            return faceChallengeResponse(s, memberId, s.ownerFaceEnrolled);
        }

        long staffId = parseStaffId(memberId);
        SellerStaff st = staff.findById(staffId).orElseThrow(() -> ApiException.unauthorized("Isci tapilmadi"));
        if (st.sellerId != s.id || !"active".equals(st.status)) {
            throw ApiException.unauthorized("Isci tapilmadi");
        }
        if (st.passwordHash == null || st.passwordHash.isBlank()) {
            throw ApiException.badRequest("Ilk giris ucun sifre teyin edin");
        }
        if (!encoder.matches(password, st.passwordHash)) {
            throw ApiException.unauthorized("Sifre yanlisdir");
        }
        return faceChallengeResponse(s, memberId, st.faceEnrolled);
    }

    public Map<String, Object> setMemberPassword(SellerSetMemberPasswordRequest req) {
        SellerAccount s = requireStore(req.storeCode);
        String memberId = req.memberId == null || req.memberId.isBlank() ? "owner" : req.memberId.trim();
        String pass = trim(req.password);
        String pass2 = trim(req.passwordConfirm);
        if (pass.length() < 6) throw ApiException.badRequest("Sifre en azi 6 simvol olmalidir");
        if (!pass.equals(pass2)) throw ApiException.badRequest("Sifreler uygun gelmir");

        if ("owner".equals(memberId)) {
            if (s.ownerLoginPasswordHash != null && !s.ownerLoginPasswordHash.isBlank()) {
                throw ApiException.conflict("Sifre artiq teyin edilib");
            }
            accounts.setOwnerLoginPassword(s.id, encoder.encode(pass));
            s = accounts.findById(s.id).orElse(s);
            return faceChallengeResponse(s, memberId, s.ownerFaceEnrolled);
        }

        long staffId = parseStaffId(memberId);
        SellerStaff st = staff.findById(staffId).orElseThrow(() -> ApiException.notFound("Isci tapilmadi"));
        if (st.passwordHash != null && !st.passwordHash.isBlank()) {
            throw ApiException.conflict("Sifre artiq teyin edilib");
        }
        staff.setStaffPassword(staffId, encoder.encode(pass));
        st = staff.findById(staffId).orElse(st);
        return faceChallengeResponse(s, memberId, st.faceEnrolled);
    }

    public Map<String, Object> enrollFace(SellerFaceRequest req) {
        SellerFaceChallengeRepository.ChallengeRow row = requireChallenge(req.challengeToken, "enroll");
        SellerAccount s = accounts.findById(row.sellerId).orElseThrow(() -> ApiException.unauthorized("Magaza tapilmadi"));
        byte[] image = compreFace.decodeImage(req.imageBase64);

        String subject = subjectForMember(s, row.memberId);
        if (compreFace.enabled()) {
            compreFace.addFace(subject, image);
        }
        markFaceEnrolled(s, row.memberId, subject);
        faceChallenges.delete(row.id);
        return completeLogin(s, row.memberId);
    }

    public Map<String, Object> verifyFace(SellerFaceRequest req) {
        SellerFaceChallengeRepository.ChallengeRow row = requireChallenge(req.challengeToken, "verify");
        SellerAccount s = accounts.findById(row.sellerId).orElseThrow(() -> ApiException.unauthorized("Magaza tapilmadi"));
        String expectedSubject = subjectForMember(s, row.memberId);

        if (compreFace.enabled()) {
            byte[] image = compreFace.decodeImage(req.imageBase64);
            String recognized = compreFace.recognizeSubject(image);
            if (!expectedSubject.equals(recognized)) {
                throw ApiException.unauthorized("Uz taninmadi");
            }
        }

        faceChallenges.delete(row.id);
        return completeLogin(s, row.memberId);
    }

    /** @deprecated OTP fallback — use verifyMember + face flow */
    public Map<String, Object> requestOtp(SellerOtpRequest req) {
        return verifyMember(toMemberVerify(req));
    }

    /** @deprecated */
    public Map<String, Object> verifyOtp(SellerOtpVerifyRequest req) {
        throw ApiException.badRequest("OTP girisi deaktivdir. Uz tanima istifade edin");
    }

    private Map<String, Object> faceChallengeResponse(SellerAccount s, String memberId, boolean faceEnrolled) {
        if (!compreFace.enabled()) {
            return completeLogin(s, memberId);
        }
        String purpose = faceEnrolled ? "verify" : "enroll";
        String token = faceChallenges.insert(s.id, memberId, purpose,
                Instant.now().plus(CHALLENGE_TTL_MINUTES, ChronoUnit.MINUTES));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("challenge_token", token);
        out.put("needs_face_enroll", !faceEnrolled);
        out.put("needs_face_verify", faceEnrolled);
        return out;
    }

    private Map<String, Object> completeLogin(SellerAccount s, String memberId) {
        AuthPrincipal principal;
        String usernameSlug;
        if ("owner".equals(memberId)) {
            principal = AuthPrincipal.sellerOwner(s.id, s.email, s.storeName);
            usernameSlug = s.ownerSlug;
        } else {
            long staffId = Long.parseLong(memberId);
            SellerStaff st = staff.findById(staffId).orElseThrow(() -> ApiException.unauthorized("Isci tapilmadi"));
            principal = AuthPrincipal.sellerStaff(s.id, st.email, st.name, st.id, st.role, st.permissions);
            usernameSlug = st.usernameSlug != null && !st.usernameSlug.isBlank() ? st.usernameSlug : "isci" + staffId;
        }
        if (usernameSlug == null || usernameSlug.isBlank()) usernameSlug = "panel";

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("logged_in", true);
        out.put("redirect_path", "/" + s.storeSlug + "/sellerpanel/" + usernameSlug);
        out.put("principal", principal);
        out.put("store", s);
        return out;
    }

    private SellerFaceChallengeRepository.ChallengeRow requireChallenge(String tokenRaw, String expectedPurpose) {
        String token = trim(tokenRaw);
        if (token.isBlank()) throw ApiException.badRequest("Sessiya tapilmadi");
        SellerFaceChallengeRepository.ChallengeRow row = faceChallenges.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("Sessiya vaxti kecib"));
        if (!expectedPurpose.equals(row.purpose)) {
            throw ApiException.badRequest("Sessiya duzgun deyil");
        }
        return row;
    }

    private String subjectForMember(SellerAccount s, String memberId) {
        if ("owner".equals(memberId)) {
            return s.ownerFaceSubject != null && !s.ownerFaceSubject.isBlank()
                    ? s.ownerFaceSubject : CompreFaceService.faceSubjectForOwner(s.id);
        }
        long staffId = Long.parseLong(memberId);
        SellerStaff st = staff.findById(staffId).orElseThrow(() -> ApiException.notFound("Isci tapilmadi"));
        return st.faceSubject != null && !st.faceSubject.isBlank()
                ? st.faceSubject : CompreFaceService.faceSubjectForStaff(s.id, staffId);
    }

    private void markFaceEnrolled(SellerAccount s, String memberId, String subject) {
        if ("owner".equals(memberId)) {
            accounts.setOwnerFaceEnrolled(s.id, subject);
        } else {
            staff.setFaceEnrolled(Long.parseLong(memberId), subject);
        }
    }

    private static SellerMemberVerifyRequest toMemberVerify(SellerOtpRequest req) {
        SellerMemberVerifyRequest r = new SellerMemberVerifyRequest();
        r.storeCode = req.storeCode;
        r.memberId = req.memberId;
        r.password = req.password;
        return r;
    }

    private static long parseStaffId(String memberId) {
        try {
            return Long.parseLong(memberId);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Secim duzgun deyil");
        }
    }

    private SellerAccount requireStore(String storeCodeRaw) {
        String code = trim(storeCodeRaw).replaceAll("\\s+", "");
        if (code.length() != 9 || !code.chars().allMatch(Character::isDigit)) {
            throw ApiException.badRequest("9 reqemli magaza kodu daxil edin");
        }
        return accounts.findByStoreCode(code)
                .orElseThrow(() -> ApiException.unauthorized("Magaza tapilmadi"));
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
