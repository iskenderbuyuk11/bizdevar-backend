package com.bizdevar.admin;

import com.bizdevar.common.ApiException;
import com.bizdevar.config.AppProperties;
import com.bizdevar.mail.EmailService;
import com.bizdevar.order.OrderRepository;
import com.bizdevar.seller.SellerAccount;
import com.bizdevar.seller.SellerAccountRepository;
import com.bizdevar.seller.SellerEngagementRepository;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminQueries q;
    private final OrderRepository orders;
    private final JdbcTemplate jdbc;
    private final SellerAccountRepository sellerAccounts;
    private final SellerEngagementRepository sellerEngagement;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public AdminController(AdminQueries q, OrderRepository orders, JdbcTemplate jdbc,
                           SellerAccountRepository sellerAccounts, SellerEngagementRepository sellerEngagement,
                           EmailService emailService, AppProperties appProperties) {
        this.q = q;
        this.orders = orders;
        this.jdbc = jdbc;
        this.sellerAccounts = sellerAccounts;
        this.sellerEngagement = sellerEngagement;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    // ---------- Read ----------
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest r) { guard(r); return q.dashboard(); }

    @GetMapping("/search")
    public Map<String, Object> search(HttpServletRequest r, @RequestParam(name = "q", required = false) String query) {
        guard(r);
        return q.search(query);
    }

    @GetMapping("/vendor-applications")
    public Map<String, Object> vendorApplications(HttpServletRequest r) { guard(r); return q.vendorApplications(); }

    @GetMapping("/stores")
    public Map<String, Object> stores(HttpServletRequest r) { guard(r); return q.stores(); }

    @GetMapping("/vendors")
    public Map<String, Object> vendors(HttpServletRequest r) { guard(r); return q.stores(); }

    @GetMapping("/vendors/{id}")
    public Map<String, Object> vendorDetail(HttpServletRequest r, @PathVariable long id) { guard(r); return q.vendorDetail(id); }

    @GetMapping("/products")
    public Map<String, Object> products(HttpServletRequest r) { guard(r); return q.products(); }

    @GetMapping("/products/{id}")
    public Map<String, Object> productDetail(HttpServletRequest r, @PathVariable long id) { guard(r); return q.productDetail(id); }

    @GetMapping("/categories")
    public Map<String, Object> categories(HttpServletRequest r) { guard(r); return q.categories(); }

    @GetMapping("/orders")
    public Map<String, Object> orders(HttpServletRequest r) { guard(r); return q.ordersList(); }

    @GetMapping("/orders/{id}")
    public Map<String, Object> orderDetail(HttpServletRequest r, @PathVariable String id) {
        guard(r);
        return Map.of("order", orders.adminGetByNumber(id));
    }

    @GetMapping("/customers")
    public Map<String, Object> customers(HttpServletRequest r) { guard(r); return q.customers(); }

    @GetMapping("/customers/{id}")
    public Map<String, Object> customerDetail(HttpServletRequest r, @PathVariable long id) {
        guard(r);
        var rows = jdbc.query("SELECT id, name, email, phone, created_at FROM users WHERE id = ? AND is_admin = 0",
                (rs, n) -> Map.<String, Object>of(
                        "id", rs.getLong("id"), "name", rs.getString("name"),
                        "email", rs.getString("email"),
                        "phone", rs.getString("phone") == null ? "" : rs.getString("phone"),
                        "created_at", rs.getString("created_at")), id);
        return Map.of("customer", rows.isEmpty() ? Map.of() : rows.get(0));
    }

    @GetMapping("/payments")
    public Map<String, Object> payments(HttpServletRequest r) { guard(r); return q.payments(); }

    @GetMapping("/shipping")
    public Map<String, Object> shipping(HttpServletRequest r) { guard(r); return q.shipping(); }

    @GetMapping({"/campaigns", "/coupons"})
    public Map<String, Object> couponsGet(HttpServletRequest r) { guard(r); return q.coupons(); }

    @GetMapping("/reviews")
    public Map<String, Object> reviews(HttpServletRequest r) { guard(r); return q.reviews(); }

    @GetMapping("/reports")
    public Map<String, Object> reports(HttpServletRequest r) { guard(r); return q.reports(); }

    @GetMapping("/notifications")
    public Map<String, Object> notifications(HttpServletRequest r) { guard(r); return q.notifications(); }

    @GetMapping("/cms")
    public Map<String, Object> cms(HttpServletRequest r) { guard(r); return q.cms(); }

    @GetMapping("/settings")
    public Map<String, Object> settings(HttpServletRequest r) { guard(r); return q.settings(); }

    @GetMapping("/support/tickets")
    public Map<String, Object> tickets(HttpServletRequest r) { guard(r); return q.supportTickets(); }

    @GetMapping("/audit-logs")
    public Map<String, Object> audit(HttpServletRequest r) { guard(r); return q.auditLogs(); }

    // ---------- Actions ----------
    @PostMapping("/vendors/{id}/approve")
    public Map<String, Object> approveVendor(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        Long sellerId = null;
        try {
            sellerId = jdbc.queryForObject("SELECT seller_id FROM vendors WHERE id=?", Long.class, id);
        } catch (Exception ignored) {}
        if (sellerId != null) {
            sellerAccounts.onApproved(sellerId);
            sellerAccounts.findById(sellerId).ifPresent(s -> sendApprovalEmail(s));
        } else {
            jdbc.update("UPDATE vendors SET verification_status='verified', status='active' WHERE id=?", id);
        }
        audit(a, r, "vendor_approve", "vendor:" + id);
        return ok();
    }

    @PostMapping("/vendors/{id}/reject")
    public Map<String, Object> rejectVendor(HttpServletRequest r, @PathVariable long id,
                                            @RequestBody(required = false) AdminActionRequest body) {
        AppUser a = guard(r);
        String reason = body != null && body.reason != null ? body.reason : "";
        Long sellerId = null;
        try {
            sellerId = jdbc.queryForObject("SELECT seller_id FROM vendors WHERE id=?", Long.class, id);
        } catch (Exception ignored) {}
        if (sellerId != null) {
            sellerAccounts.onRejected(sellerId, reason);
        } else {
            jdbc.update("UPDATE vendors SET status='restricted', rejection_reason=? WHERE id=?", reason, id);
        }
        audit(a, r, "vendor_reject", "vendor:" + id);
        return ok();
    }

    @PostMapping("/vendors/{id}/suspend")
    public Map<String, Object> suspendVendor(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        jdbc.update("UPDATE vendors SET status='restricted' WHERE id=?", id);
        audit(a, r, "vendor_suspend", "vendor:" + id);
        return ok();
    }

    @PostMapping("/products/{id}/approve")
    public Map<String, Object> approveProduct(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        String action = jdbc.queryForObject("SELECT moderation_action FROM products WHERE id=?", String.class, id);
        if ("delete".equals(action)) {
            jdbc.update("UPDATE products SET status='deleted', moderation_action='' WHERE id=?", id);
        } else {
            jdbc.update("UPDATE products SET status='active', moderation_action='', rejection_reason='' WHERE id=?", id);
        }
        audit(a, r, "product_approve", "product:" + id);
        return ok();
    }

    @PostMapping("/products/{id}/reject")
    public Map<String, Object> rejectProduct(HttpServletRequest r, @PathVariable long id,
                                             @RequestBody(required = false) AdminActionRequest body) {
        AppUser a = guard(r);
        String reason = body != null && body.reason != null ? body.reason : "";
        jdbc.update("UPDATE products SET status = CASE WHEN moderation_action='create' THEN 'moderation' ELSE 'active' END, "
                + "moderation_action='', rejection_reason=? WHERE id=?", reason, id);
        audit(a, r, "product_reject", "product:" + id);
        return ok();
    }

    @PostMapping("/products/bulk-approve")
    public Map<String, Object> bulkApprove(HttpServletRequest r) {
        AppUser a = guard(r);
        jdbc.update("UPDATE products SET status='deleted', moderation_action='' WHERE moderation_action='delete'");
        int n = jdbc.update("UPDATE products SET status='active', moderation_action='', rejection_reason='' "
                + "WHERE status='pending' AND moderation_action <> 'delete'");
        audit(a, r, "product_bulk_approve", "count:" + n);
        return Map.of("status", "ok", "approved", n);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createCategory(HttpServletRequest r, @RequestBody AdminActionRequest body) {
        AppUser a = guard(r);
        if (body.name == null || body.name.isBlank()) throw ApiException.badRequest("Ad teleb olunur");
        String slug = (body.slug == null || body.slug.isBlank())
                ? body.name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "")
                : body.slug;
        jdbc.update("INSERT IGNORE INTO categories (slug, name, status) VALUES (?, ?, 'active')", slug, body.name);
        audit(a, r, "category_create", slug);
        return ok();
    }

    @PatchMapping("/orders/{id}/status")
    public Map<String, Object> updateOrderStatus(HttpServletRequest r, @PathVariable String id,
                                                 @RequestBody AdminActionRequest body) {
        AppUser a = guard(r);
        if (body.status == null || body.status.isBlank()) throw ApiException.badRequest("Status teleb olunur");
        orders.updateStatus(id, body.status);
        audit(a, r, "order_status", id + ":" + body.status);
        return ok();
    }

    @PostMapping("/payments/{id}/approve")
    public Map<String, Object> approveTransaction(HttpServletRequest r, @PathVariable String id) {
        AppUser a = guard(r);
        jdbc.update("UPDATE transactions SET status='completed' WHERE trx_id=?", id);
        audit(a, r, "payment_approve", id);
        return ok();
    }

    @PostMapping({"/campaigns", "/coupons"})
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createCoupon(HttpServletRequest r, @RequestBody AdminActionRequest body) {
        AppUser a = guard(r);
        if (body.code == null || body.code.isBlank()) throw ApiException.badRequest("Kod teleb olunur");
        int pct = body.discount_percent == null ? 0 : body.discount_percent;
        try {
            jdbc.update("INSERT INTO coupons (code, discount_percent, active) VALUES (?, ?, 1)",
                    body.code.toUpperCase(), pct);
        } catch (Exception e) {
            throw ApiException.conflict("Kupon artiq movcuddur");
        }
        audit(a, r, "coupon_create", body.code);
        return ok();
    }

    @PostMapping("/cms/{id}/publish")
    public Map<String, Object> publishCms(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        jdbc.update("UPDATE cms_pages SET status='published', updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
        audit(a, r, "cms_publish", "cms:" + id);
        return ok();
    }

    @PatchMapping("/settings/{key}")
    public Map<String, Object> updateSetting(HttpServletRequest r, @PathVariable String key,
                                            @RequestBody AdminActionRequest body) {
        AppUser a = guard(r);
        jdbc.update("UPDATE settings SET setting_value=? WHERE setting_key=?", body.value == null ? "" : body.value, key);
        audit(a, r, "setting_update", key);
        return ok();
    }

    @PatchMapping("/support/tickets/{id}/status")
    public Map<String, Object> updateTicket(HttpServletRequest r, @PathVariable String id,
                                            @RequestBody AdminActionRequest body) {
        AppUser a = guard(r);
        jdbc.update("UPDATE support_tickets SET status=? WHERE ticket_number=?",
                body.status == null ? "new" : body.status, id);
        audit(a, r, "ticket_status", id);
        return ok();
    }

    @GetMapping("/reviews/pending")
    public Map<String, Object> pendingReviews(HttpServletRequest r) {
        guard(r);
        return Map.of("reviews", sellerEngagement.listPendingReviewsForAdmin());
    }

    @PostMapping("/reviews/{id}/approve")
    public Map<String, Object> approveReview(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        sellerEngagement.moderateReview(id, "approved");
        audit(a, r, "review_approve", "review:" + id);
        return ok();
    }

    @PostMapping("/reviews/{id}/reject")
    public Map<String, Object> rejectReview(HttpServletRequest r, @PathVariable long id) {
        AppUser a = guard(r);
        sellerEngagement.moderateReview(id, "rejected");
        audit(a, r, "review_reject", "review:" + id);
        return ok();
    }

    // ---------- helpers ----------
    private AppUser guard(HttpServletRequest r) {
        return AuthSupport.requireAdminLegacy(r);
    }

    private void sendApprovalEmail(SellerAccount s) {
        if (s.storeCode == null || s.storeCode.isBlank()) {
            sellerAccounts.backfillStoreIdentity(s.id, s.storeName, s.ownerName, s.ownerSurname);
            s = sellerAccounts.findById(s.id).orElse(s);
        }
        String ownerName = ((s.ownerName == null ? "" : s.ownerName.trim()) + " "
                + (s.ownerSurname == null ? "" : s.ownerSurname.trim())).trim();
        if (ownerName.isBlank()) ownerName = s.storeName;
        String base = appProperties.getFrontendUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String loginUrl = base + "/sellerpanel/login.html";
        emailService.sendSellerApproved(s.email, s.storeCode, ownerName, loginUrl);
    }

    private Map<String, Object> ok() {
        return Map.of("status", "ok");
    }

    private void audit(AppUser admin, HttpServletRequest r, String event, String resource) {
        try {
            jdbc.update("INSERT INTO audit_logs (event_type, admin_id, ip, resource) VALUES (?, ?, ?, ?)",
                    event, admin == null ? null : admin.id, clientIp(r), resource);
        } catch (Exception ignored) {
        }
    }

    private static String clientIp(HttpServletRequest r) {
        String fwd = r.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return r.getRemoteAddr();
    }
}
