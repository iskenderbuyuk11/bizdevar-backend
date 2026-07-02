package com.bizdevar.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AdminOtpRepository {

    private final JdbcTemplate jdbc;

    public AdminOtpRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void invalidateForAdmin(long adminId) {
        jdbc.update("DELETE FROM admin_otps WHERE admin_id = ?", adminId);
    }

    public void insert(long adminId, String codeHash, String purpose, Instant expiresAt) {
        jdbc.update(
                "INSERT INTO admin_otps (admin_id, code_hash, purpose, expires_at) VALUES (?, ?, ?, ?)",
                adminId, codeHash, purpose, Timestamp.from(expiresAt));
    }

    public Optional<OtpRow> findLatestValid(long adminId, String purpose) {
        List<OtpRow> rows = jdbc.query(
                "SELECT id, admin_id, code_hash, purpose, verified_at, expires_at FROM admin_otps "
                        + "WHERE admin_id = ? AND purpose = ? AND expires_at > NOW() ORDER BY id DESC LIMIT 1",
                (rs, i) -> {
                    OtpRow r = new OtpRow();
                    r.id = rs.getLong("id");
                    r.adminId = rs.getLong("admin_id");
                    r.codeHash = rs.getString("code_hash");
                    r.purpose = rs.getString("purpose");
                    Timestamp v = rs.getTimestamp("verified_at");
                    r.verifiedAt = v != null ? v.toInstant() : null;
                    r.expiresAt = rs.getTimestamp("expires_at").toInstant();
                    return r;
                },
                adminId, purpose);
        return rows.stream().findFirst();
    }

    public void markVerified(long id) {
        jdbc.update("UPDATE admin_otps SET verified_at = NOW() WHERE id = ?", id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM admin_otps WHERE id = ?", id);
    }

    public static class OtpRow {
        public long id;
        public long adminId;
        public String codeHash;
        public String purpose;
        public Instant verifiedAt;
        public Instant expiresAt;
    }
}
