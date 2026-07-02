package com.bizdevar.seller;

import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.common.SlugUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SellerAccountRepository {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;

    public SellerAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SellerAccount> findById(long id) {
        List<SellerAccount> list = jdbc.query(
                "SELECT s.*, v.id AS vendor_id, v.logo_url "
                        + "FROM sellers s "
                        + "LEFT JOIN vendors v ON v.seller_id = s.id "
                        + "WHERE s.id = ?",
                this::mapSeller, id);
        return list.stream().findFirst();
    }

    public Optional<SellerAccount> findByEmail(String email) {
        List<SellerAccount> list = jdbc.query(
                "SELECT s.*, v.id AS vendor_id, v.logo_url "
                        + "FROM sellers s "
                        + "LEFT JOIN vendors v ON v.seller_id = s.id "
                        + "WHERE s.email = ?",
                this::mapSeller, normalizeEmail(email));
        return list.stream().findFirst();
    }

    public Optional<SellerAccount> findByStoreCode(String storeCode) {
        if (storeCode == null || storeCode.isBlank()) return Optional.empty();
        List<SellerAccount> list = jdbc.query(
                "SELECT s.*, v.id AS vendor_id, v.logo_url "
                        + "FROM sellers s "
                        + "LEFT JOIN vendors v ON v.seller_id = s.id "
                        + "WHERE s.store_code = ?",
                this::mapSeller, storeCode.trim());
        return list.stream().findFirst();
    }

    public Optional<SellerAccount> findByStoreSlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        List<SellerAccount> list = jdbc.query(
                "SELECT s.*, v.id AS vendor_id, v.logo_url "
                        + "FROM sellers s "
                        + "LEFT JOIN vendors v ON v.seller_id = s.id "
                        + "WHERE s.store_slug = ? AND s.status IN ('active', 'pending')",
                this::mapSeller, slug.trim().toLowerCase());
        return list.stream().findFirst();
    }

    public boolean storeSlugExists(String slug) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM sellers WHERE store_slug = ?", Integer.class, slug);
        return c != null && c > 0;
    }

    public boolean ownerSlugExists(String slug) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM sellers WHERE owner_slug = ?", Integer.class, slug);
        return c != null && c > 0;
    }

    public String generateUniqueStoreCode() {
        for (int i = 0; i < 80; i++) {
            String code = String.format("%09d", 100_000_000L + (RANDOM.nextInt(900_000_000)));
            Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM sellers WHERE store_code = ?", Integer.class, code);
            if (c == null || c == 0) return code;
        }
        throw ApiException.internal("Magaza kodu yaradila bilmedi");
    }

    public void backfillStoreIdentity(long sellerId, String storeName, String ownerName, String ownerSurname) {
        SellerAccount s = findById(sellerId).orElse(null);
        if (s == null) return;
        if (s.storeCode == null || s.storeCode.isBlank()) {
            String code = generateUniqueStoreCode();
            jdbc.update("UPDATE sellers SET store_code = ? WHERE id = ?", code, sellerId);
        }
        if (s.storeSlug == null || s.storeSlug.isBlank()) {
            String slug = SlugUtil.uniqueSlug(storeName, this::storeSlugExists);
            jdbc.update("UPDATE sellers SET store_slug = ? WHERE id = ?", slug, sellerId);
            jdbc.update("UPDATE vendors SET slug = ? WHERE seller_id = ?", slug, sellerId);
        }
        if (s.ownerSlug == null || s.ownerSlug.isBlank()) {
            String ownerSlug = SlugUtil.uniqueSlug(
                    SlugUtil.personSlug(ownerName, ownerSurname),
                    this::ownerSlugExists);
            jdbc.update("UPDATE sellers SET owner_slug = ? WHERE id = ?", ownerSlug, sellerId);
        }
    }

    public boolean existsByEmail(String email) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM sellers WHERE email = ?",
                Integer.class, normalizeEmail(email));
        return c != null && c > 0;
    }

    public void assertCanRegister(String email) {
        String norm = normalizeEmail(email);
        Integer blocked = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sellers WHERE email = ? AND status IN ('rejected','restricted') "
                        + "AND rejected_at IS NOT NULL AND rejected_at > DATE_SUB(NOW(), INTERVAL 7 DAY)",
                Integer.class, norm);
        if (blocked != null && blocked > 0) {
            throw ApiException.conflict("Legv edildikden 7 gun kecmemis yeniden muraciet ede bilmezsiniz");
        }

        Optional<SellerAccount> existing = findByEmail(email);
        if (existing.isEmpty()) return;

        SellerAccount s = existing.get();
        if ("rejected".equals(s.status) || "restricted".equals(s.status)) {
            jdbc.update("DELETE FROM vendors WHERE seller_id = ?", s.id);
            jdbc.update("DELETE FROM seller_staff WHERE seller_id = ?", s.id);
            jdbc.update("DELETE FROM seller_notifications WHERE seller_id = ?", s.id);
            jdbc.update("DELETE FROM sellers WHERE id = ?", s.id);
            return;
        }
        throw ApiException.conflict("Bu email ile artiq satici muracieti var");
    }

    public SellerAccount register(SellerRegisterRequest req, String passwordHash) {
        assertCanRegister(req.email);

        String storeName = trim(req.storeName);
        boolean autoNamed = false;
        if (storeName.isEmpty()) {
            autoNamed = true;
            storeName = "Magaza-" + System.currentTimeMillis() % 100000;
        }
        assertUniqueStoreName(storeName, null);

        String storeType = normalizeStoreType(req);
        String voen = "voenli".equals(storeType) ? trim(req.voen) : "";
        if ("voenli".equals(storeType) && voen.isEmpty()) {
            throw ApiException.badRequest("VOEN teleb olunur");
        }
        if ("voenli".equals(storeType)) {
            assertUniqueVoen(voen, null);
        }

        final String finalStoreName = storeName;
        final boolean finalAutoNamed = autoNamed;
        final String finalStoreType = storeType;
        final String finalVoen = voen;
        final String storeCode = generateUniqueStoreCode();
        final String storeSlug = SlugUtil.uniqueSlug(storeName, this::storeSlugExists);
        final String ownerSlug = SlugUtil.uniqueSlug(
                SlugUtil.personSlug(trim(req.ownerName), trim(req.ownerSurname)),
                this::ownerSlugExists);

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sellers (email, phone, password_hash, owner_name, owner_surname, store_name, "
                            + "category, store_type, voen, verification_status, status, auto_named, "
                            + "store_code, store_slug, owner_slug) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', 'pending', ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, normalizeEmail(req.email));
            ps.setString(2, trim(req.phone));
            ps.setString(3, passwordHash);
            ps.setString(4, trim(req.ownerName));
            ps.setString(5, trim(req.ownerSurname));
            ps.setString(6, finalStoreName);
            ps.setString(7, trim(req.category));
            ps.setString(8, finalStoreType);
            ps.setString(9, finalVoen);
            ps.setInt(10, finalAutoNamed ? 1 : 0);
            ps.setString(11, storeCode);
            ps.setString(12, storeSlug);
            ps.setString(13, ownerSlug);
            return ps;
        }, kh);

        long sellerId = kh.getKey().longValue();
        long vendorId = createVendorForSeller(sellerId, finalStoreName, trim(req.category), finalStoreType, finalVoen, trim(req.phone), storeSlug);
        insertNotification(sellerId, "Muraciet qebul edildi",
                "Magaza qeydiyyatiniz admin yoxlamasina gonderildi. Tesdiqden sonra panel tam acilacaq.", "info");
        SellerAccount account = findById(sellerId).orElseThrow(() -> ApiException.internal("Satici yaradilmadi"));
        account.vendorId = vendorId;
        return account;
    }

    private long createVendorForSeller(long sellerId, String storeName, String category, String storeType,
                                       String voen, String phone, String slug) {
        jdbc.update("INSERT INTO vendors (seller_id, user_id, name, category, verification_status, status, "
                        + "store_type, voen, phone, auto_named, slug) VALUES (?, NULL, ?, ?, 'pending', 'pending', ?, ?, ?, 0, ?)",
                sellerId, storeName, category, storeType, voen, phone, slug);
        return jdbc.queryForObject("SELECT id FROM vendors WHERE seller_id = ?", Long.class, sellerId);
    }

    public Map<String, Object> publicProfile(String slug) {
        SellerAccount s = findByStoreSlug(slug)
                .orElseThrow(() -> ApiException.notFound("Magaza tapilmadi"));
        long vendorId = vendorIdForSeller(s.id);

        Integer totalProducts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE vendor_id = ? AND status = 'active'", Integer.class, vendorId);
        Integer orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE vendor_id = ?", Integer.class, vendorId);
        Integer deliveredOrders = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE vendor_id = ? AND status = 'delivered'", Integer.class, vendorId);
        Double avgRating = jdbc.queryForObject(
                "SELECT COALESCE(AVG(r.stars),0) FROM product_reviews r "
                        + "JOIN products p ON p.id = r.product_id "
                        + "WHERE p.vendor_id = ? AND r.admin_status = 'approved'",
                Double.class, vendorId);

        int total = orderCount == null ? 0 : orderCount;
        int delivered = deliveredOrders == null ? 0 : deliveredOrders;
        int successRate = total == 0 ? 100 : (int) Math.round((delivered * 100.0) / total);

        List<Map<String, Object>> products = jdbc.query(
                "SELECT id, name, price, image_url, category_slug FROM products "
                        + "WHERE vendor_id = ? AND status = 'active' ORDER BY created_at DESC LIMIT 24",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("name", rs.getString("name"));
                    m.put("price", rs.getDouble("price"));
                    m.put("image_url", rs.getString("image_url"));
                    m.put("category", rs.getString("category_slug"));
                    return m;
                }, vendorId);

        Map<String, Object> store = new LinkedHashMap<>();
        store.put("name", s.storeName);
        store.put("slug", s.storeSlug);
        store.put("category", s.category);
        store.put("rating", avgRating == null ? s.rating : Math.round(avgRating * 10.0) / 10.0);
        store.put("logo_url", s.logoUrl);
        store.put("joined_at", s.createdAt);
        store.put("product_count", totalProducts == null ? 0 : totalProducts);
        store.put("order_count", total);
        store.put("success_rate", successRate);
        store.put("status", s.status);
        if (!"active".equals(s.status)) {
            store.put("pending", true);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("store", store);
        out.put("products", "active".equals(s.status) ? products : List.of());
        return out;
    }

    public SellerAccount requireActiveSeller(long sellerId) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.forbidden("Satici hesabi tapilmadi"));
        if (!"active".equals(s.status)) {
            throw ApiException.forbidden("Magaza tesdiq gozleyir ve ya legv edilib");
        }
        return s;
    }

    public long vendorIdForSeller(long sellerId) {
        Long id = jdbc.queryForObject("SELECT id FROM vendors WHERE seller_id = ?", Long.class, sellerId);
        if (id == null) throw ApiException.forbidden("Magaza tapilmadi");
        return id;
    }

    public Map<String, Object> dashboard(long sellerId) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        long vendorId = vendorIdForSeller(sellerId);

        Integer totalProducts = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ?",
                Integer.class, vendorId);
        Integer activeProducts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE vendor_id = ? AND status = 'active'", Integer.class, vendorId);
        Integer pendingProducts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE vendor_id = ? AND status = 'pending'", Integer.class, vendorId);
        Integer orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE vendor_id = ?",
                Integer.class, vendorId);
        Integer activeOrders = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE vendor_id = ? AND status IN ('placed','processing','shipped')",
                Integer.class, vendorId);
        Integer deliveredOrders = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE vendor_id = ? AND status = 'delivered'", Integer.class, vendorId);
        Double revenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total),0) FROM orders WHERE vendor_id = ? AND status != 'cancelled'",
                Double.class, vendorId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("store", s.toMap());
        out.put("metrics", List.of(
                metric("Sifaris sayi", orderCount),
                metric("Aktiv sifaris", activeOrders),
                metric("Catdirilan", deliveredOrders),
                metric("Umumi gelir", "₼ " + String.format("%.0f", revenue == null ? 0 : revenue)),
                metric("Mehsul sayi", totalProducts),
                metric("Aktiv mehsul", activeProducts),
                metric("Tesdiq gozleyir", pendingProducts)
        ));
        return out;
    }

    public List<Map<String, Object>> listReviews(long sellerId) {
        long vendorId = vendorIdForSeller(sellerId);
        return jdbc.query(
                "SELECT r.id, r.stars, r.text, r.created_at, p.name AS product_name, c.name AS customer_name "
                        + "FROM product_reviews r "
                        + "JOIN products p ON p.id = r.product_id "
                        + "LEFT JOIN customers c ON c.id = r.user_id "
                        + "WHERE p.vendor_id = ? ORDER BY r.created_at DESC LIMIT 100",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("stars", rs.getInt("stars"));
                    m.put("text", rs.getString("text"));
                    m.put("product_name", rs.getString("product_name"));
                    m.put("customer_name", rs.getString("customer_name"));
                    m.put("created_at", rs.getString("created_at"));
                    return m;
                }, vendorId);
    }

    public List<Map<String, Object>> listComplaints(long sellerId) {
        return jdbc.query(
                "SELECT id, product_id, subject, body, status, created_at FROM product_complaints "
                        + "WHERE seller_id = ? ORDER BY created_at DESC LIMIT 100",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("product_id", rs.getObject("product_id"));
                    m.put("subject", rs.getString("subject"));
                    m.put("body", rs.getString("body"));
                    m.put("status", rs.getString("status"));
                    m.put("created_at", rs.getString("created_at"));
                    return m;
                }, sellerId);
    }

    public List<Map<String, Object>> listNotifications(long sellerId) {
        return jdbc.query(
                "SELECT id, title, body, level, is_read, created_at FROM seller_notifications "
                        + "WHERE seller_id = ? ORDER BY created_at DESC LIMIT 50",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("title", rs.getString("title"));
                    m.put("body", rs.getString("body"));
                    m.put("level", rs.getString("level"));
                    m.put("is_read", rs.getInt("is_read") == 1);
                    m.put("created_at", rs.getString("created_at"));
                    return m;
                }, sellerId);
    }

    public void markNotificationRead(long sellerId, long notificationId) {
        jdbc.update("UPDATE seller_notifications SET is_read = 1 WHERE id = ? AND seller_id = ?",
                notificationId, sellerId);
    }

    public List<Map<String, Object>> listStaff(long sellerId) {
        return jdbc.query(
                "SELECT id, email, name, role, status, invited_at, joined_at FROM seller_staff "
                        + "WHERE seller_id = ? ORDER BY invited_at DESC",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("email", rs.getString("email"));
                    m.put("name", rs.getString("name"));
                    m.put("role", rs.getString("role"));
                    m.put("status", rs.getString("status"));
                    m.put("invited_at", rs.getString("invited_at"));
                    m.put("joined_at", rs.getString("joined_at"));
                    return m;
                }, sellerId);
    }

    public long inviteStaff(long sellerId, String email, String name) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank() || !normalized.contains("@")) {
            throw ApiException.badRequest("Duzgun email daxil edin");
        }
        jdbc.update("INSERT INTO seller_staff (seller_id, email, name, role, status) VALUES (?, ?, ?, 'staff', 'invited')",
                sellerId, normalized, name == null ? "" : name.trim());
        insertNotification(sellerId, "Isci devet gonderildi",
                normalized + " unvanina devet hazirlandi (mail gonderisi tezlikle aktiv olacaq).", "info");
        return jdbc.queryForObject("SELECT id FROM seller_staff WHERE seller_id = ? AND email = ? ORDER BY id DESC LIMIT 1",
                Long.class, sellerId, normalized);
    }

    public void insertNotification(long sellerId, String title, String body, String level) {
        jdbc.update("INSERT INTO seller_notifications (seller_id, title, body, level) VALUES (?, ?, ?, ?)",
                sellerId, title, body, level == null ? "info" : level);
    }

    public SellerAccount updateSettings(long sellerId, String storeName, String phone) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        if ("deleted".equals(s.status)) {
            throw ApiException.forbidden("Magaza silinib");
        }

        String name = trim(storeName);
        if (name.length() < 2) {
            throw ApiException.badRequest("Magaza adi en azi 2 simvol olmalidir");
        }
        assertUniqueStoreName(name, sellerId);
        String phoneNorm = trim(phone);
        if (phoneNorm.length() < 7) {
            throw ApiException.badRequest("Duzgun telefon nomresi daxil edin");
        }

        jdbc.update("UPDATE sellers SET store_name = ?, phone = ?, auto_named = 0 WHERE id = ?",
                name, phoneNorm, sellerId);
        jdbc.update("UPDATE vendors SET name = ?, phone = ?, auto_named = 0 WHERE seller_id = ?",
                name, phoneNorm, sellerId);
        insertNotification(sellerId, "Magaza melumatlari yenilendi",
                "Magaza adi ve telefon nomresi ugurla yenilendi.", "info");
        return findById(sellerId).orElseThrow(() -> ApiException.internal("Yenileme alinmadi"));
    }

    public SellerAccount updateLogo(long sellerId, String logoUrl) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        if ("deleted".equals(s.status)) {
            throw ApiException.forbidden("Magaza silinib");
        }
        jdbc.update("UPDATE vendors SET logo_url = ? WHERE seller_id = ?", logoUrl, sellerId);
        insertNotification(sellerId, "Profil sekli yenilendi",
                "Magaza profil sekli ugurla yenilendi.", "info");
        return findById(sellerId).orElseThrow(() -> ApiException.internal("Yenileme alinmadi"));
    }

    public SellerAccount removeLogo(long sellerId) {
        jdbc.update("UPDATE vendors SET logo_url = NULL WHERE seller_id = ?", sellerId);
        return findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
    }

    public SellerAccount freezeStore(long sellerId) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        if (!"active".equals(s.status)) {
            throw ApiException.badRequest("Yalniz aktiv magaza dondurula biler");
        }
        jdbc.update("UPDATE sellers SET status = 'frozen', rejection_reason = '' WHERE id = ?", sellerId);
        jdbc.update("UPDATE vendors SET status = 'frozen' WHERE seller_id = ?", sellerId);
        insertNotification(sellerId, "Magaza donduruldu",
                "Magaziniz donduruldu. Mehsullar saytda gorunmeyecek. Istediyiniz vaxt yeniden aktiv ede bilersiniz.", "warning");
        return findById(sellerId).orElseThrow(() -> ApiException.internal("Emeliyyat alinmadi"));
    }

    public SellerAccount unfreezeStore(long sellerId) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        if (!"frozen".equals(s.status)) {
            throw ApiException.badRequest("Magaza dondurulmeyib ve ya admin terefinden dayandirilib");
        }
        jdbc.update("UPDATE sellers SET status = 'active', rejection_reason = '' WHERE id = ?", sellerId);
        jdbc.update("UPDATE vendors SET status = 'active' WHERE seller_id = ?", sellerId);
        insertNotification(sellerId, "Magaza aktiv edildi",
                "Magaziniz yeniden aktiv edildi. Mehsullariniz saytda gorunecek.", "success");
        return findById(sellerId).orElseThrow(() -> ApiException.internal("Emeliyyat alinmadi"));
    }

    public void deleteStore(long sellerId) {
        SellerAccount s = findById(sellerId).orElseThrow(() -> ApiException.notFound("Satici tapilmadi"));
        if ("deleted".equals(s.status)) {
            throw ApiException.badRequest("Magaza artiq silinib");
        }
        jdbc.update("UPDATE sellers SET status = 'deleted', rejection_reason = 'Magaza sahibi terefinden silindi' WHERE id = ?",
                sellerId);
        jdbc.update("UPDATE vendors SET status = 'deleted' WHERE seller_id = ?", sellerId);
        jdbc.update("UPDATE products SET status = 'deleted' WHERE vendor_id = ?", s.vendorId);
    }

    public void onApproved(long sellerId) {
        jdbc.update("UPDATE sellers SET status='active', verification_status='verified', approved_at=?, rejection_reason='', rejected_at=NULL WHERE id=?",
                Timestamp.from(Instant.now()), sellerId);
        jdbc.update("UPDATE vendors SET status='active', verification_status='verified' WHERE seller_id=?", sellerId);
        insertNotification(sellerId, "Magaza tesdiqlendi", "Tebrikler! Magaziniz aktiv edildi. Mehsul elave ede bilersiniz.", "success");
    }

    public void setOwnerLoginPassword(long sellerId, String hash) {
        jdbc.update("UPDATE sellers SET owner_login_password_hash = ? WHERE id = ?", hash, sellerId);
    }

    public void setOwnerFaceEnrolled(long sellerId, String subject) {
        jdbc.update("UPDATE sellers SET owner_face_enrolled = 1, owner_face_subject = ? WHERE id = ?", subject, sellerId);
    }

    public void assertUniqueStoreName(String storeName, Long excludeId) {
        if (storeName == null || storeName.isBlank()) return;
        String sql = "SELECT COUNT(*) FROM sellers WHERE LOWER(TRIM(store_name)) = LOWER(TRIM(?)) "
                + "AND status NOT IN ('deleted','rejected')";
        Integer c;
        if (excludeId != null) {
            c = jdbc.queryForObject(sql + " AND id <> ?", Integer.class, storeName.trim(), excludeId);
        } else {
            c = jdbc.queryForObject(sql, Integer.class, storeName.trim());
        }
        if (c != null && c > 0) {
            throw ApiException.conflict("Bu magaza adi artiq istifade olunur");
        }
    }

    public void assertUniqueVoen(String voen, Long excludeId) {
        if (voen == null || voen.isBlank()) return;
        String sql = "SELECT COUNT(*) FROM sellers WHERE voen = ? AND voen <> '' AND status NOT IN ('deleted','rejected')";
        Integer c;
        if (excludeId != null) {
            c = jdbc.queryForObject(sql + " AND id <> ?", Integer.class, voen.trim(), excludeId);
        } else {
            c = jdbc.queryForObject(sql, Integer.class, voen.trim());
        }
        if (c != null && c > 0) {
            throw ApiException.conflict("Bu VOEN artiq qeydiyyatdan kecib");
        }
    }

    public void onRejected(long sellerId, String reason) {
        jdbc.update("UPDATE sellers SET status='rejected', verification_status='rejected', rejection_reason=?, rejected_at=? WHERE id=?",
                reason == null ? "" : reason, Timestamp.from(Instant.now()), sellerId);
        jdbc.update("UPDATE vendors SET status='restricted', rejection_reason=? WHERE seller_id=?",
                reason == null ? "" : reason, sellerId);
        insertNotification(sellerId, "Muraciet legv edildi",
                (reason == null || reason.isBlank()) ? "7 gun sonra yeniden muraciet ede bilersiniz." : reason, "warning");
    }

    private SellerAccount mapSeller(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        SellerAccount s = new SellerAccount();
        s.id = rs.getLong("id");
        s.email = rs.getString("email");
        s.phone = rs.getString("phone");
        s.passwordHash = rs.getString("password_hash");
        s.ownerName = rs.getString("owner_name");
        s.ownerSurname = rs.getString("owner_surname");
        s.storeName = rs.getString("store_name");
        s.category = rs.getString("category");
        s.storeType = rs.getString("store_type");
        s.voen = rs.getString("voen");
        s.verificationStatus = rs.getString("verification_status");
        s.status = rs.getString("status");
        s.rejectionReason = rs.getString("rejection_reason");
        s.rejectedAt = rs.getString("rejected_at");
        s.approvedAt = rs.getString("approved_at");
        s.autoNamed = rs.getInt("auto_named") == 1;
        s.revenue = rs.getDouble("revenue");
        s.rating = rs.getDouble("rating");
        try {
            s.vendorId = rs.getLong("vendor_id");
            if (rs.wasNull()) s.vendorId = 0;
        } catch (java.sql.SQLException ignored) {
            s.vendorId = 0;
        }
        try {
            s.logoUrl = rs.getString("logo_url");
        } catch (java.sql.SQLException ignored) {
            s.logoUrl = null;
        }
        try {
            s.storeCode = rs.getString("store_code");
        } catch (java.sql.SQLException ignored) {
            s.storeCode = null;
        }
        try {
            s.storeSlug = rs.getString("store_slug");
        } catch (java.sql.SQLException ignored) {
            s.storeSlug = null;
        }
        try {
            s.ownerSlug = rs.getString("owner_slug");
        } catch (java.sql.SQLException ignored) {
            s.ownerSlug = null;
        }
        try {
            s.createdAt = rs.getString("created_at");
        } catch (java.sql.SQLException ignored) {
            s.createdAt = null;
        }
        try {
            s.ownerLoginPasswordHash = rs.getString("owner_login_password_hash");
        } catch (java.sql.SQLException ignored) {
            s.ownerLoginPasswordHash = null;
        }
        try {
            s.ownerFaceEnrolled = rs.getInt("owner_face_enrolled") == 1;
        } catch (java.sql.SQLException ignored) {
            s.ownerFaceEnrolled = false;
        }
        try {
            s.ownerFaceSubject = rs.getString("owner_face_subject");
        } catch (java.sql.SQLException ignored) {
            s.ownerFaceSubject = null;
        }
        return s;
    }

    private static Map<String, Object> metric(String label, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", String.valueOf(value));
        return m;
    }

    private static String normalizeStoreType(SellerRegisterRequest req) {
        String t = trim(req.storeType).toLowerCase();
        if ("warehouse".equals(t) || "voenli".equals(t) || "anbar".equals(t)) return "voenli";
        if ("online".equals(t) || "voensiz".equals(t)) return "voensiz";
        return trim(req.voen).isEmpty() ? "voensiz" : "voenli";
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
