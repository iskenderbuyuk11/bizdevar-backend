package com.bizdevar.seller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SellerLoginOtpRepository {

    private final JdbcTemplate jdbc;

    public SellerLoginOtpRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        ensureTable();
    }

    public void ensureTable() {
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS seller_login_otps ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "seller_id BIGINT NOT NULL, "
                        + "member_id VARCHAR(32) NOT NULL, "
                        + "email VARCHAR(255) NOT NULL, "
                        + "code_hash VARCHAR(255) NOT NULL, "
                        + "challenge_token VARCHAR(64) NOT NULL, "
                        + "expires_at DATETIME NOT NULL, "
                        + "verified_at DATETIME NULL, "
                        + "INDEX idx_seller_login_otps_token (challenge_token), "
                        + "INDEX idx_seller_login_otps_seller (seller_id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public void invalidateForSeller(long sellerId, String memberId) {
        jdbc.update("DELETE FROM seller_login_otps WHERE seller_id = ? AND member_id = ?", sellerId, memberId);
    }

    public String insert(long sellerId, String memberId, String email, String codeHash, Instant expiresAt) {
        String token = UUID.randomUUID().toString().replace("-", "");
        jdbc.update(
                "INSERT INTO seller_login_otps (seller_id, member_id, email, code_hash, challenge_token, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                sellerId, memberId, email, codeHash, token, Timestamp.from(expiresAt));
        return token;
    }

    public Optional<OtpRow> findByToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        List<OtpRow> rows = jdbc.query(
                "SELECT id, seller_id, member_id, email, code_hash, challenge_token, expires_at, verified_at "
                        + "FROM seller_login_otps WHERE challenge_token = ? AND expires_at > NOW() ORDER BY id DESC LIMIT 1",
                (rs, i) -> {
                    OtpRow r = new OtpRow();
                    r.id = rs.getLong("id");
                    r.sellerId = rs.getLong("seller_id");
                    r.memberId = rs.getString("member_id");
                    r.email = rs.getString("email");
                    r.codeHash = rs.getString("code_hash");
                    r.challengeToken = rs.getString("challenge_token");
                    r.expiresAt = rs.getTimestamp("expires_at").toInstant();
                    Timestamp v = rs.getTimestamp("verified_at");
                    r.verifiedAt = v != null ? v.toInstant() : null;
                    return r;
                }, token.trim());
        return rows.stream().findFirst();
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM seller_login_otps WHERE id = ?", id);
    }

    public static class OtpRow {
        public long id;
        public long sellerId;
        public String memberId;
        public String email;
        public String codeHash;
        public String challengeToken;
        public Instant expiresAt;
        public Instant verifiedAt;
    }
}
