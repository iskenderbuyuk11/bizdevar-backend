package com.bizdevar.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AppUser> MAPPER = (rs, n) -> {
        AppUser u = new AppUser();
        u.id = rs.getLong("id");
        u.name = rs.getString("name");
        u.email = rs.getString("email");
        u.phone = rs.getString("phone");
        u.passwordHash = rs.getString("password_hash");
        u.admin = rs.getInt("is_admin") == 1;
        u.googleId = rs.getString("google_id");
        u.avatarUrl = rs.getString("avatar_url");
        u.createdAt = rs.getString("created_at");
        return u;
    };

    public Optional<AppUser> findById(long id) {
        List<AppUser> list = jdbc.query(
                "SELECT id, name, email, phone, password_hash, is_admin, google_id, avatar_url, created_at FROM users WHERE id = ?",
                MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<AppUser> findByEmail(String email) {
        List<AppUser> list = jdbc.query(
                "SELECT id, name, email, phone, password_hash, is_admin, google_id, avatar_url, created_at FROM users WHERE email = ?",
                MAPPER, email.trim().toLowerCase());
        return list.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email.trim().toLowerCase());
        return c != null && c > 0;
    }

    public long insert(String name, String email, String phone, String passwordHash, boolean admin, String googleId, String avatarUrl) {
        KeyHolder kh = new GeneratedKeyHolder();
        String emailNorm = email.trim().toLowerCase();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (name, email, phone, password_hash, is_admin, google_id, avatar_url) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name.trim());
            ps.setString(2, emailNorm);
            ps.setString(3, phone == null ? "" : phone.trim());
            ps.setString(4, passwordHash);
            ps.setInt(5, admin ? 1 : 0);
            ps.setString(6, googleId);
            ps.setString(7, avatarUrl);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? 0 : key.longValue();
    }

    public void setGoogleId(long userId, String googleId, String avatarUrl) {
        jdbc.update("UPDATE users SET google_id = ?, avatar_url = COALESCE(?, avatar_url) WHERE id = ?",
                googleId, avatarUrl, userId);
    }

    public VendorSummary vendorForUser(long userId) {
        List<VendorSummary> list = jdbc.query(
                "SELECT id, name, status FROM vendors WHERE user_id = ?",
                (rs, n) -> new VendorSummary(rs.getLong("id"), rs.getString("name"), rs.getString("status")),
                userId);
        return list.isEmpty() ? null : list.get(0);
    }

    public record VendorSummary(long id, String name, String status) {}
}
