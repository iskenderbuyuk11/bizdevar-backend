package com.bizdevar.favorites;

import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {

    private final JdbcTemplate jdbc;

    public FavoritesController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request) {
        AppUser u = AuthSupport.require(request);
        return Map.of("ids", ids(u.id));
    }

    @PostMapping("/{productId}")
    public Map<String, Object> add(HttpServletRequest request, @PathVariable long productId) {
        AppUser u = AuthSupport.require(request);
        jdbc.update("INSERT IGNORE INTO favorites (user_id, product_id) VALUES (?, ?)", u.id, productId);
        return Map.of("ids", ids(u.id));
    }

    @DeleteMapping("/{productId}")
    public Map<String, Object> remove(HttpServletRequest request, @PathVariable long productId) {
        AppUser u = AuthSupport.require(request);
        jdbc.update("DELETE FROM favorites WHERE user_id = ? AND product_id = ?", u.id, productId);
        return Map.of("ids", ids(u.id));
    }

    private List<Long> ids(long userId) {
        return jdbc.queryForList(
                "SELECT product_id FROM favorites WHERE user_id = ? ORDER BY product_id", Long.class, userId);
    }
}
