package com.bizdevar.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Production serverde sql.init.mode=never oldugu ucun admin cedvelleri avtomatik yaradilir.
 */
@Component
public class AdminSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminSchemaInitializer.class);

    private final JdbcTemplate jdbc;

    public AdminSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String[] SEED_ADMINS = {
            "isgenderpasayev@gmail.com",
            "qasimliilqar603@gmail.com",
            "raulquluzade545@gmail.com",
    };

    @PostConstruct
    public void init() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS admins (
                        id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        email         VARCHAR(255) NOT NULL,
                        name          VARCHAR(255) NOT NULL DEFAULT '',
                        password_hash VARCHAR(255) NULL,
                        is_active     TINYINT      NOT NULL DEFAULT 1,
                        created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_admins_email (email)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS admin_otps (
                        id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        admin_id    BIGINT NOT NULL,
                        code_hash   VARCHAR(255) NOT NULL,
                        purpose     VARCHAR(20)  NOT NULL DEFAULT 'login',
                        verified_at DATETIME NULL,
                        expires_at  DATETIME NOT NULL,
                        created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        KEY idx_admin_otps_admin (admin_id),
                        KEY idx_admin_otps_expires (expires_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            for (String email : SEED_ADMINS) {
                jdbc.update(
                        "INSERT IGNORE INTO admins (email, name, password_hash, is_active) VALUES (?, '', NULL, 1)",
                        email);
            }

            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM admins", Integer.class);
            log.info("Admin schema hazirdir (admin sayi: {})", n);
        } catch (Exception e) {
            log.error("KRITIK: Admin cedvelleri yaradila bilmedi — admin-tables.sql isledin.", e);
        }
    }
}
