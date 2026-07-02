package com.bizdevar.home;

import com.bizdevar.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class HomeStoryRepository {

    private final JdbcTemplate jdbc;

    public HomeStoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listActive() {
        return jdbc.query(
                "SELECT id, title, image_url, link_url FROM home_stories WHERE is_active = 1 ORDER BY sort_order ASC, id ASC",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("title", rs.getString("title"));
                    m.put("image_url", rs.getString("image_url"));
                    m.put("link_url", rs.getString("link_url"));
                    return m;
                });
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.query(
                "SELECT id, title, image_url, link_url, sort_order, is_active, created_at, updated_at "
                        + "FROM home_stories ORDER BY sort_order ASC, id ASC",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("title", rs.getString("title"));
                    m.put("image_url", rs.getString("image_url"));
                    m.put("link_url", rs.getString("link_url"));
                    m.put("sort_order", rs.getInt("sort_order"));
                    m.put("is_active", rs.getInt("is_active") == 1);
                    m.put("created_at", rs.getString("created_at"));
                    m.put("updated_at", rs.getString("updated_at"));
                    return m;
                });
    }

    public Optional<Map<String, Object>> findById(long id) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT id, title, image_url, link_url, sort_order, is_active FROM home_stories WHERE id = ?",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("title", rs.getString("title"));
                    m.put("image_url", rs.getString("image_url"));
                    m.put("link_url", rs.getString("link_url"));
                    m.put("sort_order", rs.getInt("sort_order"));
                    m.put("is_active", rs.getInt("is_active") == 1);
                    return m;
                }, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long insert(String title, String imageUrl, String linkUrl, int sortOrder, boolean active) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO home_stories (title, image_url, link_url, sort_order, is_active) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            ps.setString(2, imageUrl);
            ps.setString(3, linkUrl);
            ps.setInt(4, sortOrder);
            ps.setInt(5, active ? 1 : 0);
            return ps;
        }, key);
        Number id = key.getKey();
        if (id == null) throw ApiException.internal("Story yaradila bilmedi");
        return id.longValue();
    }

    public void update(long id, String title, String imageUrl, String linkUrl, int sortOrder, boolean active) {
        int n = jdbc.update(
                "UPDATE home_stories SET title = ?, image_url = ?, link_url = ?, sort_order = ?, is_active = ? WHERE id = ?",
                title, imageUrl, linkUrl, sortOrder, active ? 1 : 0, id);
        if (n == 0) throw ApiException.notFound("Story tapilmadi");
    }

    public void delete(long id) {
        int n = jdbc.update("DELETE FROM home_stories WHERE id = ?", id);
        if (n == 0) throw ApiException.notFound("Story tapilmadi");
    }
}
