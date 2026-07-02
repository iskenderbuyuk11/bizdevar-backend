-- Production server: admins cedvelleri (bir defe isledin)
CREATE TABLE IF NOT EXISTS admins (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL DEFAULT '',
    password_hash VARCHAR(255) NULL,
    is_active     TINYINT      NOT NULL DEFAULT 1,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_admins_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO admins (email, name, password_hash, is_active)
VALUES
    ('isgenderpasayev@gmail.com', '', NULL, 1),
    ('qasimliilqar603@gmail.com', '', NULL, 1),
    ('raulquluzade545@gmail.com', '', NULL, 1);
