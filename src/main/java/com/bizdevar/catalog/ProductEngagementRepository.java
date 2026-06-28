package com.bizdevar.catalog;

import com.bizdevar.common.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProductEngagementRepository {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final JdbcTemplate jdbc;

    public ProductEngagementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS product_reviews ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "product_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "stars INT NOT NULL,"
                + "text TEXT NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP NULL DEFAULT NULL,"
                + "UNIQUE KEY uq_product_reviews_user (product_id, user_id),"
                + "KEY idx_product_reviews_product (product_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        jdbc.execute("CREATE TABLE IF NOT EXISTS product_questions ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "product_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "question TEXT NOT NULL,"
                + "answer TEXT NULL,"
                + "status VARCHAR(40) NOT NULL DEFAULT 'pending',"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "answered_at TIMESTAMP NULL DEFAULT NULL,"
                + "KEY idx_product_questions_product (product_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    public List<Map<String, Object>> reviews(long productId) {
        return jdbc.query(
                "SELECT r.id, r.stars, r.text, r.created_at, u.name AS user_name "
                        + "FROM product_reviews r JOIN users u ON u.id = r.user_id "
                        + "WHERE r.product_id = ? ORDER BY r.created_at DESC",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("stars", rs.getInt("stars"));
                    m.put("text", rs.getString("text"));
                    m.put("name", maskName(rs.getString("user_name")));
                    m.put("time", fmt(rs.getTimestamp("created_at")));
                    return m;
                },
                productId
        );
    }

    public List<Map<String, Object>> questions(long productId) {
        return jdbc.query(
                "SELECT q.id, q.question, q.answer, q.created_at, u.name AS user_name "
                        + "FROM product_questions q JOIN users u ON u.id = q.user_id "
                        + "WHERE q.product_id = ? ORDER BY q.created_at DESC",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("text", rs.getString("question"));
                    m.put("answer", rs.getString("answer"));
                    m.put("name", maskName(rs.getString("user_name")));
                    m.put("time", fmt(rs.getTimestamp("created_at")));
                    return m;
                },
                productId
        );
    }

    public boolean canReview(long userId, long productId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders o "
                        + "JOIN order_items i ON i.order_id = o.id "
                        + "WHERE o.user_id = ? AND i.product_id = ? AND o.status = 'delivered'",
                Integer.class,
                userId,
                productId
        );
        return count != null && count > 0;
    }

    public Map<String, Object> upsertReview(long userId, long productId, int stars, String text) {
        if (!canReview(userId, productId)) {
            throw ApiException.forbidden("Bu mehsula rey yazmaq ucun sifaris catdirilmis olmalidir");
        }
        int safeStars = Math.max(1, Math.min(5, stars));
        String safeText = text == null ? "" : text.trim();
        if (safeText.isBlank()) throw ApiException.badRequest("Rey metni bos ola bilmez");

        jdbc.update(
                "INSERT INTO product_reviews (product_id, user_id, stars, text) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE stars = VALUES(stars), text = VALUES(text), updated_at = CURRENT_TIMESTAMP",
                productId,
                userId,
                safeStars,
                safeText
        );
        return reviews(productId).stream()
                .filter(r -> r.get("name") != null)
                .findFirst()
                .orElse(Map.of());
    }

    public Map<String, Object> addQuestion(long userId, long productId, String text) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isBlank()) throw ApiException.badRequest("Sual bos ola bilmez");
        jdbc.update(
                "INSERT INTO product_questions (product_id, user_id, question) VALUES (?, ?, ?)",
                productId,
                userId,
                safeText
        );
        return questions(productId).stream().findFirst().orElse(Map.of());
    }

    private static String fmt(Timestamp t) {
        LocalDateTime dt = t != null ? t.toLocalDateTime() : LocalDateTime.now();
        return dt.format(DATE_FMT);
    }

    private static String maskName(String name) {
        String s = name == null ? "İstifadəçi" : name.trim();
        if (s.length() <= 2) return s.charAt(0) + "**";
        return s.charAt(0) + "** " + s.charAt(s.length() - 1) + "**";
    }
}
