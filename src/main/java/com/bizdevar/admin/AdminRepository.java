package com.bizdevar.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminRepository {

    private static final RowMapper<AdminAccount> MAPPER = (rs, rowNum) -> {
        AdminAccount a = new AdminAccount();
        a.id = rs.getLong("id");
        a.email = rs.getString("email");
        a.name = rs.getString("name");
        a.passwordHash = rs.getString("password_hash");
        a.active = rs.getInt("is_active") == 1;
        Timestamp ts = rs.getTimestamp("created_at");
        a.createdAt = ts != null ? ts.toInstant() : Instant.now();
        return a;
    };

    private final JdbcTemplate jdbc;

    public AdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AdminAccount> findByEmail(String email) {
        List<AdminAccount> rows = jdbc.query(
                "SELECT id, email, name, password_hash, is_active, created_at FROM admins WHERE email = ? AND is_active = 1",
                MAPPER, email);
        return rows.stream().findFirst();
    }

    public Optional<AdminAccount> findById(long id) {
        List<AdminAccount> rows = jdbc.query(
                "SELECT id, email, name, password_hash, is_active, created_at FROM admins WHERE id = ? AND is_active = 1",
                MAPPER, id);
        return rows.stream().findFirst();
    }

    public List<AdminAccount> findAll() {
        return jdbc.query(
                "SELECT id, email, name, password_hash, is_active, created_at FROM admins WHERE is_active = 1 ORDER BY created_at ASC",
                MAPPER);
    }

    public long insert(String email, String name) {
        var key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO admins (email, name, password_hash, is_active) VALUES (?, ?, NULL, 1)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, email);
            ps.setString(2, name != null ? name : "");
            return ps;
        }, key);
        Number id = key.getKey();
        return id != null ? id.longValue() : 0L;
    }

    public void setPassword(long id, String passwordHash) {
        jdbc.update("UPDATE admins SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public boolean existsByEmail(String email) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM admins WHERE email = ? AND is_active = 1", Integer.class, email);
        return n != null && n > 0;
    }
}
