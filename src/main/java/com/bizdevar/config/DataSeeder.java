package com.bizdevar.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * İlkin admin email-ləri (şifrəsiz — ilk girişdə OTP ilə təyin edilir).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String[] SEED_ADMINS = {
            "isgenderpasayev@gmail.com",
            "qasimliilqar603@gmail.com",
            "raulquluzade545@gmail.com",
    };

    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        for (String email : SEED_ADMINS) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM admins WHERE email = ?", Integer.class, email);
            if (count == null || count == 0) {
                jdbc.update(
                        "INSERT INTO admins (email, name, password_hash, is_active) VALUES (?, ?, NULL, 1)",
                        email, "");
            }
        }
    }
}
