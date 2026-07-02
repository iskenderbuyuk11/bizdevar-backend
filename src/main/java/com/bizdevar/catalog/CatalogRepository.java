package com.bizdevar.catalog;

import com.bizdevar.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbc;

    public CatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listCategories() {
        return jdbc.query(
                "SELECT c.slug, c.name, c.status, "
                        + "(SELECT COUNT(*) FROM products p WHERE p.category_slug = c.slug AND p.status = 'active') AS cnt "
                        + "FROM categories c ORDER BY c.name",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("slug", rs.getString("slug"));
                    m.put("name", rs.getString("name"));
                    m.put("product_count", rs.getInt("cnt"));
                    m.put("status", rs.getString("status"));
                    return m;
                });
    }

    public List<Map<String, Object>> listProducts(String cat, String q, String slug, boolean activeOnly) {
        StringBuilder sql = new StringBuilder(
                "SELECT " + ProductMapper.SELECT_COLUMNS
                        + " FROM products p LEFT JOIN vendors v ON v.id = p.vendor_id WHERE 1=1");
        List<Object> args = new ArrayList<>();

        if (activeOnly) {
            sql.append(" AND p.status = 'active'");
            sql.append(" AND (v.status = 'active' OR v.id IS NULL)");
        }
        if (cat != null && !cat.isBlank() && !cat.equals("all")) {
            sql.append(" AND p.category_slug = ?");
            args.add(cat);
        }
        if (slug != null && !slug.isBlank()) {
            sql.append(" AND p.slug = ?");
            args.add(slug);
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND LOWER(p.name) LIKE ?");
            args.add("%" + q.toLowerCase() + "%");
        }
        sql.append(" ORDER BY p.popular DESC, p.id DESC");

        return jdbc.query(sql.toString(), ProductMapper.INSTANCE, args.toArray());
    }

    public Map<String, Object> getProduct(long id) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT " + ProductMapper.SELECT_COLUMNS
                        + " FROM products p LEFT JOIN vendors v ON v.id = p.vendor_id "
                        + "WHERE p.id = ? AND p.status = 'active' AND (v.status = 'active' OR v.id IS NULL)",
                ProductMapper.INSTANCE, id);
        if (list.isEmpty()) throw ApiException.notFound("Mehsul tapilmadi");
        return list.get(0);
    }
}
