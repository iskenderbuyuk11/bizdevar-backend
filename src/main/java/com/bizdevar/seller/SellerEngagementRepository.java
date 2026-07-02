package com.bizdevar.seller;

import com.bizdevar.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SellerEngagementRepository {

    private final JdbcTemplate jdbc;

    public SellerEngagementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listQuestions(long vendorId) {
        return jdbc.query(
                "SELECT q.id, q.question, q.answer, q.status, q.created_at, q.answered_at, q.published_at, "
                        + "p.id AS product_id, p.name AS product_name, c.name AS customer_name "
                        + "FROM product_questions q "
                        + "JOIN products p ON p.id = q.product_id "
                        + "LEFT JOIN customers c ON c.id = q.user_id "
                        + "WHERE p.vendor_id = ? ORDER BY q.created_at DESC LIMIT 200",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("question", rs.getString("question"));
                    m.put("answer", rs.getString("answer"));
                    m.put("status", rs.getString("status"));
                    m.put("product_id", rs.getLong("product_id"));
                    m.put("product_name", rs.getString("product_name"));
                    m.put("customer_name", rs.getString("customer_name"));
                    m.put("created_at", rs.getString("created_at"));
                    m.put("answered_at", rs.getString("answered_at"));
                    m.put("published_at", rs.getString("published_at"));
                    return m;
                }, vendorId);
    }

    public void answerQuestion(long vendorId, long questionId, String answer) {
        String safe = answer == null ? "" : answer.trim();
        if (safe.isBlank()) throw ApiException.badRequest("Cavab bos ola bilmez");
        Integer ok = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_questions q JOIN products p ON p.id = q.product_id "
                        + "WHERE q.id = ? AND p.vendor_id = ?",
                Integer.class, questionId, vendorId);
        if (ok == null || ok == 0) throw ApiException.notFound("Sual tapilmadi");
        jdbc.update(
                "UPDATE product_questions SET answer = ?, status = 'answered', answered_at = ? WHERE id = ?",
                safe, Timestamp.from(Instant.now()), questionId);
    }

    public void publishQuestion(long vendorId, long questionId) {
        Integer ok = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_questions q JOIN products p ON p.id = q.product_id "
                        + "WHERE q.id = ? AND p.vendor_id = ? AND q.answer IS NOT NULL AND q.answer != ''",
                Integer.class, questionId, vendorId);
        if (ok == null || ok == 0) throw ApiException.badRequest("Evvelce cavab yazilmalidir");
        jdbc.update(
                "UPDATE product_questions SET status = 'published', published_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), questionId);
    }

    public List<Map<String, Object>> listReviews(long sellerId, long vendorId) {
        return jdbc.query(
                "SELECT r.id, r.stars, r.text, r.admin_status, r.seller_reply, r.seller_reply_at, r.created_at, "
                        + "p.name AS product_name, c.name AS customer_name "
                        + "FROM product_reviews r "
                        + "JOIN products p ON p.id = r.product_id "
                        + "LEFT JOIN customers c ON c.id = r.user_id "
                        + "WHERE p.vendor_id = ? ORDER BY r.created_at DESC LIMIT 200",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("stars", rs.getInt("stars"));
                    m.put("text", rs.getString("text"));
                    m.put("admin_status", rs.getString("admin_status"));
                    m.put("seller_reply", rs.getString("seller_reply"));
                    m.put("seller_reply_at", rs.getString("seller_reply_at"));
                    m.put("product_name", rs.getString("product_name"));
                    m.put("customer_name", rs.getString("customer_name"));
                    m.put("created_at", rs.getString("created_at"));
                    return m;
                }, vendorId);
    }

    public void replyReview(long vendorId, long reviewId, String reply) {
        String safe = reply == null ? "" : reply.trim();
        if (safe.isBlank()) throw ApiException.badRequest("Cavab bos ola bilmez");
        Integer ok = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_reviews r JOIN products p ON p.id = r.product_id "
                        + "WHERE r.id = ? AND p.vendor_id = ? AND r.admin_status = 'approved'",
                Integer.class, reviewId, vendorId);
        if (ok == null || ok == 0) throw ApiException.badRequest("Rey tesdiqlenmeyib ve ya tapilmadi");
        jdbc.update(
                "UPDATE product_reviews SET seller_reply = ?, seller_reply_at = ? WHERE id = ?",
                safe, Timestamp.from(Instant.now()), reviewId);
    }

    public List<Map<String, Object>> listPendingReviewsForAdmin() {
        return jdbc.query(
                "SELECT r.id, r.stars, r.text, r.created_at, p.name AS product_name, v.name AS vendor_name "
                        + "FROM product_reviews r "
                        + "JOIN products p ON p.id = r.product_id "
                        + "LEFT JOIN vendors v ON v.id = p.vendor_id "
                        + "WHERE r.admin_status = 'pending' ORDER BY r.created_at ASC LIMIT 200",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("stars", rs.getInt("stars"));
                    m.put("text", rs.getString("text"));
                    m.put("product_name", rs.getString("product_name"));
                    m.put("vendor_name", rs.getString("vendor_name"));
                    m.put("created_at", rs.getString("created_at"));
                    return m;
                });
    }

    public void moderateReview(long reviewId, String status) {
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw ApiException.badRequest("Status approved ve ya rejected olmalidir");
        }
        int n = jdbc.update("UPDATE product_reviews SET admin_status = ? WHERE id = ?", status, reviewId);
        if (n == 0) throw ApiException.notFound("Rey tapilmadi");
    }
}
