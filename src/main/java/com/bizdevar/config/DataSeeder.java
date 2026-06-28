package com.bizdevar.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Admin istifadeçisini (admin@bizdevar.com / Admin123) BCrypt hash ile yaradir.
 * data.sql parol hash-i saxlamir, cunki hash kodda generasiya olunur ki, dogru olsun.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@bizdevar.com";
    private static final String ADMIN_PASSWORD = "Admin123";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public DataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, ADMIN_EMAIL);
        if (count != null && count > 0) {
            jdbc.update("UPDATE users SET is_admin = 1 WHERE email = ?", ADMIN_EMAIL);
            return;
        }
        String hash = encoder.encode(ADMIN_PASSWORD);
        jdbc.update(
                "INSERT INTO users (name, email, phone, password_hash, is_admin) VALUES (?, ?, ?, ?, 1)",
                "Admin Rehber", ADMIN_EMAIL, "+994500000000", hash);
    }
}
