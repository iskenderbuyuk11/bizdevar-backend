-- BizdeVar MySQL schema
-- Spring Boot bunu baslangicda avtomatik isledir (spring.sql.init.mode=always).
-- Butun cedveller "IF NOT EXISTS" ile yaradilir, ona gore tekrar isledikde problem olmur.

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(40)  NOT NULL DEFAULT '',
    password_hash VARCHAR(255) NOT NULL DEFAULT '',
    is_admin      TINYINT      NOT NULL DEFAULT 0,
    google_id     VARCHAR(255) NULL,
    avatar_url    VARCHAR(512) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slug       VARCHAR(120) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(40)  NOT NULL DEFAULT 'active',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vendors (
    id                  BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NULL,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(120) NOT NULL DEFAULT '',
    verification_status VARCHAR(40)  NOT NULL DEFAULT 'pending',
    status              VARCHAR(40)  NOT NULL DEFAULT 'pending',
    store_type          VARCHAR(40)  NOT NULL DEFAULT 'online',
    voen                VARCHAR(40)  NOT NULL DEFAULT '',
    phone               VARCHAR(40)  NOT NULL DEFAULT '',
    rejection_reason    VARCHAR(512) NOT NULL DEFAULT '',
    auto_named          TINYINT      NOT NULL DEFAULT 0,
    revenue             DOUBLE       NOT NULL DEFAULT 0,
    rating              DOUBLE       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vendor_id         BIGINT NULL,
    category_slug     VARCHAR(120) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(255) NOT NULL,
    price             DOUBLE       NOT NULL,
    base_price        DOUBLE       NULL,
    discount_percent  INT          NOT NULL DEFAULT 0,
    popular           INT          NOT NULL DEFAULT 0,
    stock             INT          NOT NULL DEFAULT 0,
    image_url         VARCHAR(512) NULL,
    images_json       TEXT         NULL,
    specs_json        TEXT         NULL,
    description       TEXT         NULL,
    status            VARCHAR(40)  NOT NULL DEFAULT 'pending',
    rejection_reason  VARCHAR(512) NOT NULL DEFAULT '',
    deletion_reason   VARCHAR(512) NOT NULL DEFAULT '',
    pending_json      TEXT         NULL,
    moderation_action VARCHAR(40)  NOT NULL DEFAULT '',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_products_slug (slug),
    KEY idx_products_category (category_slug),
    KEY idx_products_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items (
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    qty        INT    NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS favorites (
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_number  VARCHAR(64)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    vendor_id     BIGINT       NULL,
    status        VARCHAR(40)  NOT NULL DEFAULT 'placed',
    total         DOUBLE       NOT NULL,
    seller        VARCHAR(255) NOT NULL DEFAULT 'BizdeVar Resmi',
    delivery_json TEXT         NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_orders_number (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NULL,
    name       VARCHAR(255) NOT NULL,
    price      DOUBLE NOT NULL,
    qty        INT    NOT NULL DEFAULT 1,
    image_url  VARCHAR(512) NULL,
    KEY idx_order_items_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_reviews (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    stars      INT    NOT NULL,
    text       TEXT   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    UNIQUE KEY uq_product_reviews_user (product_id, user_id),
    KEY idx_product_reviews_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_questions (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    question   TEXT   NOT NULL,
    answer     TEXT   NULL,
    status     VARCHAR(40) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP NULL DEFAULT NULL,
    KEY idx_product_questions_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS coupons (
    id               BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(64) NOT NULL,
    discount_percent INT    NOT NULL DEFAULT 0,
    active           TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uq_coupons_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id          BIGINT NOT NULL PRIMARY KEY,
    first_name       VARCHAR(120) NOT NULL DEFAULT '',
    last_name        VARCHAR(120) NOT NULL DEFAULT '',
    notif_prefs_json TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS addresses (
    id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label   VARCHAR(120) NOT NULL DEFAULT '',
    address VARCHAR(512) NOT NULL,
    lat     DOUBLE NULL,
    lng     DOUBLE NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cms_pages (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slug         VARCHAR(120) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT NULL,
    content_type VARCHAR(40)  NOT NULL DEFAULT 'page',
    status       VARCHAR(40)  NOT NULL DEFAULT 'draft',
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_cms_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS settings (
    setting_key   VARCHAR(64)  NOT NULL PRIMARY KEY,
    setting_value VARCHAR(512) NOT NULL,
    group_name    VARCHAR(64)  NOT NULL DEFAULT 'general',
    status        VARCHAR(40)  NOT NULL DEFAULT 'active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_tickets (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(64)  NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    subject       VARCHAR(255) NOT NULL,
    priority      VARCHAR(40)  NOT NULL DEFAULT 'normal',
    status        VARCHAR(40)  NOT NULL DEFAULT 'new',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_tickets_number (ticket_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    admin_id   BIGINT NULL,
    ip         VARCHAR(64)  NULL,
    resource   VARCHAR(255) NOT NULL DEFAULT '',
    status     VARCHAR(40)  NOT NULL DEFAULT 'ok',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shipping_providers (
    id     BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name   VARCHAR(255) NOT NULL,
    zone   VARCHAR(120) NOT NULL DEFAULT '',
    rate   DOUBLE NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL DEFAULT 'active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trx_id     VARCHAR(64)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    amount     DOUBLE NOT NULL,
    trx_type   VARCHAR(40)  NOT NULL,
    status     VARCHAR(40)  NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_trx_id (trx_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
