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
public class SellerFaceChallengeRepository {

    private final JdbcTemplate jdbc;

    public SellerFaceChallengeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS seller_face_challenges ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "seller_id BIGINT NOT NULL, "
                        + "member_id VARCHAR(32) NOT NULL, "
                        + "purpose VARCHAR(20) NOT NULL, "
                        + "challenge_token VARCHAR(64) NOT NULL, "
                        + "expires_at DATETIME NOT NULL, "
                        + "INDEX idx_sfc_token (challenge_token)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public void invalidate(long sellerId, String memberId) {
        jdbc.update("DELETE FROM seller_face_challenges WHERE seller_id = ? AND member_id = ?",
                sellerId, memberId);
    }

    public String insert(long sellerId, String memberId, String purpose, Instant expiresAt) {
        invalidate(sellerId, memberId);
        String token = UUID.randomUUID().toString().replace("-", "");
        jdbc.update(
                "INSERT INTO seller_face_challenges (seller_id, member_id, purpose, challenge_token, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                sellerId, memberId, purpose, token, Timestamp.from(expiresAt));
        return token;
    }

    public Optional<ChallengeRow> findByToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        List<ChallengeRow> rows = jdbc.query(
                "SELECT id, seller_id, member_id, purpose, challenge_token, expires_at "
                        + "FROM seller_face_challenges WHERE challenge_token = ? AND expires_at > NOW() LIMIT 1",
                (rs, i) -> {
                    ChallengeRow r = new ChallengeRow();
                    r.id = rs.getLong("id");
                    r.sellerId = rs.getLong("seller_id");
                    r.memberId = rs.getString("member_id");
                    r.purpose = rs.getString("purpose");
                    r.challengeToken = rs.getString("challenge_token");
                    r.expiresAt = rs.getTimestamp("expires_at").toInstant();
                    return r;
                }, token.trim());
        return rows.stream().findFirst();
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM seller_face_challenges WHERE id = ?", id);
    }

    public static class ChallengeRow {
        public long id;
        public long sellerId;
        public String memberId;
        public String purpose;
        public String challengeToken;
        public Instant expiresAt;
    }
}
