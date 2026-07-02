package com.bizdevar.seller;

import com.bizdevar.common.SlugUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SellerSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SellerSchemaInitializer.class);

    private final JdbcTemplate jdbc;

    public SellerSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        try {
            ensureColumn("vendors", "logo_url", "VARCHAR(512) NULL");
            ensureColumn("vendors", "slug", "VARCHAR(120) NULL");
            ensureColumn("sellers", "store_code", "VARCHAR(9) NULL");
            ensureColumn("sellers", "store_slug", "VARCHAR(120) NULL");
            ensureColumn("sellers", "owner_slug", "VARCHAR(120) NULL");
            ensureColumn("seller_staff", "password_hash", "VARCHAR(255) NULL");
            ensureColumn("seller_staff", "permissions_json", "TEXT NULL");
            ensureColumn("seller_staff", "invite_token", "VARCHAR(64) NULL");
            ensureColumn("seller_staff", "invite_expires_at", "DATETIME NULL");
            ensureColumn("seller_staff", "username_slug", "VARCHAR(120) NULL");
            ensureColumn("sellers", "owner_login_password_hash", "VARCHAR(255) NULL");
            ensureColumn("sellers", "owner_face_enrolled", "TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumn("sellers", "owner_face_subject", "VARCHAR(120) NULL");
            ensureColumn("seller_staff", "face_enrolled", "TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumn("seller_staff", "face_subject", "VARCHAR(120) NULL");
            ensureColumn("product_reviews", "admin_status", "VARCHAR(40) NOT NULL DEFAULT 'pending'");
            ensureColumn("product_reviews", "seller_reply", "TEXT NULL");
            ensureColumn("product_reviews", "seller_reply_at", "TIMESTAMP NULL DEFAULT NULL");
            ensureColumn("product_questions", "published_at", "TIMESTAMP NULL DEFAULT NULL");
            jdbc.update("UPDATE product_reviews SET admin_status = 'approved' WHERE admin_status IS NULL OR admin_status = ''");
            jdbc.update("UPDATE product_reviews SET admin_status = 'approved' WHERE admin_status = 'pending' AND created_at < NOW()");
            backfillStoreIdentities();
            log.info("Seller schema migrations hazirdir");
        } catch (Exception e) {
            log.warn("Seller schema migration: {}", e.getMessage());
        }
    }

    private void backfillStoreIdentities() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, store_name, owner_name, owner_surname FROM sellers "
                        + "WHERE store_code IS NULL OR store_code = '' OR store_slug IS NULL OR store_slug = '' "
                        + "OR owner_slug IS NULL OR owner_slug = ''");
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            String storeName = row.get("store_name") == null ? "" : String.valueOf(row.get("store_name"));
            String ownerName = row.get("owner_name") == null ? "" : String.valueOf(row.get("owner_name"));
            String ownerSurname = row.get("owner_surname") == null ? "" : String.valueOf(row.get("owner_surname"));
            backfillOne(id, storeName, ownerName, ownerSurname);
        }
    }

    private void backfillOne(long sellerId, String storeName, String ownerName, String ownerSurname) {
        Integer codeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sellers WHERE id = ? AND (store_code IS NULL OR store_code = '')",
                Integer.class, sellerId);
        if (codeCount != null && codeCount > 0) {
            String code = generateStoreCode();
            jdbc.update("UPDATE sellers SET store_code = ? WHERE id = ?", code, sellerId);
        }
        Integer slugCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sellers WHERE id = ? AND (store_slug IS NULL OR store_slug = '')",
                Integer.class, sellerId);
        if (slugCount != null && slugCount > 0) {
            String slug = uniqueStoreSlug(storeName);
            jdbc.update("UPDATE sellers SET store_slug = ? WHERE id = ?", slug, sellerId);
            jdbc.update("UPDATE vendors SET slug = ? WHERE seller_id = ?", slug, sellerId);
        }
        Integer ownerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sellers WHERE id = ? AND (owner_slug IS NULL OR owner_slug = '')",
                Integer.class, sellerId);
        if (ownerCount != null && ownerCount > 0) {
            String ownerSlug = uniqueOwnerSlug(ownerName, ownerSurname);
            jdbc.update("UPDATE sellers SET owner_slug = ? WHERE id = ?", ownerSlug, sellerId);
        }
    }

    private String generateStoreCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 80; i++) {
            String code = String.format("%09d", 100_000_000L + random.nextInt(900_000_000));
            Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM sellers WHERE store_code = ?", Integer.class, code);
            if (c == null || c == 0) return code;
        }
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L);
    }

    private String uniqueStoreSlug(String storeName) {
        String base = SlugUtil.slugify(storeName);
        if (base.isEmpty()) base = "magaza";
        if (!slugExists("sellers", "store_slug", base)) return base;
        for (int i = 2; i < 1000; i++) {
            String candidate = base + i;
            if (!slugExists("sellers", "store_slug", candidate)) return candidate;
        }
        return base + System.currentTimeMillis() % 10000;
    }

    private String uniqueOwnerSlug(String ownerName, String ownerSurname) {
        String base = SlugUtil.personSlug(ownerName, ownerSurname);
        if (base.isEmpty()) base = "sahib";
        if (!slugExists("sellers", "owner_slug", base) && !slugExists("seller_staff", "username_slug", base)) return base;
        for (int i = 2; i < 1000; i++) {
            String candidate = base + i;
            if (!slugExists("sellers", "owner_slug", candidate) && !slugExists("seller_staff", "username_slug", candidate)) {
                return candidate;
            }
        }
        return base + System.currentTimeMillis() % 10000;
    }

    private boolean slugExists(String table, String column, String slug) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, slug);
        return c != null && c > 0;
    }

    private void ensureColumn(String table, String column, String definition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count > 0) return;
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
}
