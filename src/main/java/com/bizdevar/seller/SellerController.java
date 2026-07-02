package com.bizdevar.seller;

import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.auth.dto.SellerSettingsRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.config.AppProperties;
import com.bizdevar.mail.EmailService;
import com.bizdevar.security.SellerAuth;
import com.bizdevar.security.SellerContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerRepository repo;
    private final SellerAccountRepository accounts;
    private final SellerStaffRepository staffRepo;
    private final SellerEngagementRepository engagement;
    private final ProductImageService images;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties props;

    public SellerController(SellerRepository repo, SellerAccountRepository accounts, SellerStaffRepository staffRepo,
                            SellerEngagementRepository engagement, ProductImageService images,
                            PasswordEncoder passwordEncoder, EmailService emailService, AppProperties props) {
        this.repo = repo;
        this.accounts = accounts;
        this.staffRepo = staffRepo;
        this.engagement = engagement;
        this.images = images;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.props = props;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.DASHBOARD);
        return accounts.dashboard(ctx.sellerId);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.require(request);
        SellerAccount s = accounts.findById(ctx.sellerId)
                .orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        return Map.of("store", s.toMap(), "vendor", s.toMap(), "permissions", ctx.permissions);
    }

    @GetMapping("/permissions")
    public Map<String, Object> permissionsCatalog() {
        return Map.of("permissions", SellerPermissions.catalog());
    }

    @PatchMapping("/settings")
    public Map<String, Object> updateSettings(HttpServletRequest request, @RequestBody SellerSettingsRequest req) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.SETTINGS);
        SellerAuth.requireOwner(ctx);
        SellerAccount s = accounts.updateSettings(ctx.sellerId,
                req.storeName == null ? "" : req.storeName,
                req.phone == null ? "" : req.phone);
        return Map.of("status", "ok", "message", "Melumatlar yenilendi", "store", s.toMap());
    }

    @PostMapping("/settings/logo")
    public Map<String, Object> uploadLogo(HttpServletRequest request,
                                            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.SETTINGS);
        String url = images.saveStoreLogo(file);
        SellerAccount s = accounts.updateLogo(ctx.sellerId, url);
        return Map.of("status", "ok", "logo_url", url, "store", s.toMap());
    }

    @DeleteMapping("/settings/logo")
    public Map<String, Object> removeLogo(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.SETTINGS);
        SellerAccount s = accounts.removeLogo(ctx.sellerId);
        return Map.of("status", "ok", "store", s.toMap());
    }

    @PostMapping("/settings/freeze")
    public Map<String, Object> freezeStore(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.SETTINGS);
        SellerAuth.requireOwner(ctx);
        SellerAccount s = accounts.freezeStore(ctx.sellerId);
        return Map.of("status", "ok", "message", "Magaza donduruldu", "store", s.toMap());
    }

    @PostMapping("/settings/unfreeze")
    public Map<String, Object> unfreezeStore(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.SETTINGS);
        SellerAuth.requireOwner(ctx);
        SellerAccount s = accounts.unfreezeStore(ctx.sellerId);
        return Map.of("status", "ok", "message", "Magaza aktiv edildi", "store", s.toMap());
    }

    @PostMapping("/settings/delete")
    public Map<String, Object> deleteStore(HttpServletRequest request, @RequestBody SellerSettingsRequest req) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.SETTINGS);
        SellerAuth.requireOwner(ctx);
        SellerAccount s = accounts.findById(ctx.sellerId)
                .orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        if (req.password == null || req.password.isBlank()) {
            throw ApiException.badRequest("Tesdiq ucun sifre teleb olunur");
        }
        if (s.passwordHash == null || !passwordEncoder.matches(req.password, s.passwordHash)) {
            throw ApiException.unauthorized("Sifre yanlisdir");
        }
        accounts.deleteStore(ctx.sellerId);
        return Map.of("status", "ok", "message", "Magaza silindi");
    }

    @GetMapping("/categories")
    public Map<String, Object> categories(HttpServletRequest request) {
        SellerAuth.requirePermission(request, SellerPermissions.PRODUCTS);
        return Map.of("categories", repo.listCategories());
    }

    @GetMapping("/products")
    public Map<String, Object> products(HttpServletRequest request) {
        long vendorId = vendorId(request, false, SellerPermissions.PRODUCTS);
        return Map.of("products", repo.listProducts(vendorId));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> productCreate(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        long vendorId = vendorId(request, true, SellerPermissions.PRODUCTS);
        long id = repo.createProduct(vendorId, req);
        return Map.of("product_id", id, "message", "Mehsul admin tesdiqine gonderildi");
    }

    @PostMapping("/upload-images")
    public Map<String, Object> uploadImages(HttpServletRequest request,
                                            @RequestParam("files") org.springframework.web.multipart.MultipartFile[] files) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.PRODUCTS);
        accounts.requireActiveSeller(ctx.sellerId);
        return Map.of("urls", images.saveImages(files));
    }

    @PatchMapping("/products/{id}")
    public Map<String, Object> productUpdate(HttpServletRequest request, @PathVariable long id,
                                             @RequestBody Map<String, Object> req) {
        long vendorId = vendorId(request, true, SellerPermissions.PRODUCTS);
        repo.updateProduct(vendorId, id, req);
        return Map.of("status", "ok", "message", "Deyisiklik admin tesdiqine gonderildi");
    }

    @PostMapping("/products/{id}/delete-request")
    public Map<String, Object> productDeleteRequest(HttpServletRequest request, @PathVariable long id,
                                                     @RequestBody(required = false) Map<String, Object> req) {
        long vendorId = vendorId(request, true, SellerPermissions.PRODUCTS);
        String reason = req != null && req.get("reason") != null ? String.valueOf(req.get("reason")) : "";
        repo.requestDelete(vendorId, id, reason);
        return Map.of("status", "ok", "message", "Silinme sorgusu admin tesdiqine gonderildi");
    }

    @GetMapping("/reviews")
    public Map<String, Object> reviews(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.FEEDBACK);
        long vendorId = accounts.vendorIdForSeller(ctx.sellerId);
        return Map.of("reviews", engagement.listReviews(ctx.sellerId, vendorId));
    }

    @PostMapping("/reviews/{id}/reply")
    public Map<String, Object> replyReview(HttpServletRequest request, @PathVariable long id,
                                           @RequestBody Map<String, Object> body) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.FEEDBACK);
        long vendorId = accounts.vendorIdForSeller(ctx.sellerId);
        String reply = body.get("reply") == null ? "" : String.valueOf(body.get("reply"));
        engagement.replyReview(vendorId, id, reply);
        return Map.of("status", "ok", "message", "Cavab yazildi");
    }

    @GetMapping("/questions")
    public Map<String, Object> questions(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.QUESTIONS);
        long vendorId = accounts.vendorIdForSeller(ctx.sellerId);
        return Map.of("questions", engagement.listQuestions(vendorId));
    }

    @PostMapping("/questions/{id}/answer")
    public Map<String, Object> answerQuestion(HttpServletRequest request, @PathVariable long id,
                                              @RequestBody Map<String, Object> body) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.QUESTIONS);
        long vendorId = accounts.vendorIdForSeller(ctx.sellerId);
        String answer = body.get("answer") == null ? "" : String.valueOf(body.get("answer"));
        engagement.answerQuestion(vendorId, id, answer);
        return Map.of("status", "ok", "message", "Cavab yazildi");
    }

    @PostMapping("/questions/{id}/publish")
    public Map<String, Object> publishQuestion(HttpServletRequest request, @PathVariable long id) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.QUESTIONS);
        long vendorId = accounts.vendorIdForSeller(ctx.sellerId);
        engagement.publishQuestion(vendorId, id);
        return Map.of("status", "ok", "message", "Sual hamiya acildi");
    }

    @GetMapping("/complaints")
    public Map<String, Object> complaints(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.FEEDBACK);
        return Map.of("complaints", accounts.listComplaints(ctx.sellerId));
    }

    @GetMapping("/notifications")
    public Map<String, Object> notifications(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.NOTIFICATIONS);
        return Map.of("notifications", accounts.listNotifications(ctx.sellerId));
    }

    @PostMapping("/notifications/{id}/read")
    public Map<String, Object> readNotification(HttpServletRequest request, @PathVariable long id) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.NOTIFICATIONS);
        accounts.markNotificationRead(ctx.sellerId, id);
        return Map.of("status", "ok");
    }

    @GetMapping("/staff")
    public Map<String, Object> staff(HttpServletRequest request) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.STAFF);
        accounts.requireActiveSeller(ctx.sellerId);
        return Map.of("staff", staffRepo.listStaff(ctx.sellerId), "permissions", SellerPermissions.catalog());
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> inviteStaff(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.STAFF);
        SellerAuth.requireOwner(ctx);
        accounts.requireActiveSeller(ctx.sellerId);
        String email = req.get("email") == null ? "" : String.valueOf(req.get("email"));
        String name = req.get("name") == null ? "" : String.valueOf(req.get("name"));
        String role = req.get("role") == null ? "staff" : String.valueOf(req.get("role"));
        Set<String> perms = parsePermissions(req.get("permissions"));
        long staffId = staffRepo.invite(ctx.sellerId, email, name, role, perms);
        SellerAccount store = accounts.findById(ctx.sellerId).orElseThrow();
        String token = staffRepo.inviteTokenForStaff(staffId);
        String base = props.getFrontendUrl() == null ? "https://buykon.com" : props.getFrontendUrl().replaceAll("/+$", "");
        String inviteUrl = base + "/sellerpanel/accept-invite.html?token=" + token;
        emailService.sendStaffInvite(email, store.storeName, inviteUrl);
        accounts.insertNotification(ctx.sellerId, "Isci deveti gonderildi", email + " unvanina devet gonderildi.", "info");
        return Map.of("staff_id", staffId, "message", "Devet email ile gonderildi");
    }

    @PatchMapping("/staff/{id}")
    public Map<String, Object> updateStaff(HttpServletRequest request, @PathVariable long id,
                                           @RequestBody Map<String, Object> req) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.STAFF);
        SellerAuth.requireOwner(ctx);
        String role = req.get("role") == null ? "staff" : String.valueOf(req.get("role"));
        staffRepo.updateStaff(ctx.sellerId, id, role, parsePermissions(req.get("permissions")));
        return Map.of("status", "ok");
    }

    @DeleteMapping("/staff/{id}")
    public Map<String, Object> removeStaff(HttpServletRequest request, @PathVariable long id) {
        SellerContext ctx = SellerAuth.requirePermission(request, SellerPermissions.STAFF);
        SellerAuth.requireOwner(ctx);
        staffRepo.removeStaff(ctx.sellerId, id);
        return Map.of("status", "ok");
    }

    private long vendorId(HttpServletRequest request, boolean requireActive, String permission) {
        SellerContext ctx = SellerAuth.requirePermission(request, permission);
        if (requireActive) accounts.requireActiveSeller(ctx.sellerId);
        return accounts.vendorIdForSeller(ctx.sellerId);
    }

    @SuppressWarnings("unchecked")
    private Set<String> parsePermissions(Object raw) {
        if (!(raw instanceof List<?> list)) return new LinkedHashSet<>();
        Set<String> out = new LinkedHashSet<>();
        for (Object o : list) out.add(String.valueOf(o));
        return out;
    }
}
