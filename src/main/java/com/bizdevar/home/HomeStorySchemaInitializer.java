package com.bizdevar.home;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class HomeStorySchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(HomeStorySchemaInitializer.class);

    private final JdbcTemplate jdbc;

    public HomeStorySchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS home_stories (
                        id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        title       VARCHAR(120) NOT NULL DEFAULT '',
                        image_url   VARCHAR(512) NOT NULL,
                        link_url    VARCHAR(512) NOT NULL DEFAULT '',
                        sort_order  INT NOT NULL DEFAULT 0,
                        is_active   TINYINT NOT NULL DEFAULT 1,
                        created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        KEY idx_home_stories_active (is_active, sort_order)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            log.info("home_stories cedveli hazirdir");
        } catch (Exception e) {
            log.error("home_stories cedveli yaradila bilmedi", e);
        }
    }
}
