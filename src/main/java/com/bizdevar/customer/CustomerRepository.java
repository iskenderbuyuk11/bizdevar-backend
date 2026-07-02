package com.bizdevar.customer;

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
public class CustomerRepository {

    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Customer> MAPPER = (rs, n) -> {
        Customer c = new Customer();
        c.id = rs.getLong("id");
        c.name = rs.getString("name");
        c.email = rs.getString("email");
        c.phone = rs.getString("phone");
        c.passwordHash = rs.getString("password_hash");
        c.googleId = rs.getString("google_id");
        c.avatarUrl = rs.getString("avatar_url");
        return c;
    };

    public Optional<Customer> findById(long id) {
        List<Customer> list = jdbc.query(
                "SELECT id, name, email, phone, password_hash, google_id, avatar_url FROM customers WHERE id = ?",
                MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<Customer> findByEmail(String email) {
        List<Customer> list = jdbc.query(
                "SELECT id, name, email, phone, password_hash, google_id, avatar_url FROM customers WHERE email = ?",
                MAPPER, normalizeEmail(email));
        return list.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM customers WHERE email = ?",
                Integer.class, normalizeEmail(email));
        return c != null && c > 0;
    }

    public long insert(String name, String email, String phone, String passwordHash, String googleId, String avatarUrl) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO customers (name, email, phone, password_hash, google_id, avatar_url) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name.trim());
            ps.setString(2, normalizeEmail(email));
            ps.setString(3, phone == null ? "" : phone.trim());
            ps.setString(4, passwordHash == null ? "" : passwordHash);
            ps.setString(5, googleId);
            ps.setString(6, avatarUrl);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? 0 : key.longValue();
    }

    public void setGoogleId(long customerId, String googleId, String avatarUrl) {
        jdbc.update("UPDATE customers SET google_id = ?, avatar_url = COALESCE(?, avatar_url) WHERE id = ?",
                googleId, avatarUrl, customerId);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
