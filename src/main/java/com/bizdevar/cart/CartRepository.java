package com.bizdevar.cart;

import com.bizdevar.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CartRepository {

    private final JdbcTemplate jdbc;

    public CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> get(long userId) {
        List<Map<String, Object>> items = jdbc.query(
                "SELECT p.id, p.name, p.price, p.base_price, p.discount_percent, p.image_url, c.qty, "
                        + "COALESCE(v.name, 'BizdəVar Rəsmi') AS vendor_name "
                        + "FROM cart_items c JOIN products p ON p.id = c.product_id "
                        + "LEFT JOIN vendors v ON v.id = p.vendor_id WHERE c.user_id = ?",
                (rs, n) -> {
                    Map<String, Object> it = new LinkedHashMap<>();
                    long pid = rs.getLong("id");
                    it.put("product_id", pid);
                    it.put("id", pid);
                    it.put("name", rs.getString("name"));
                    it.put("price", rs.getDouble("price"));
                    Object base = rs.getObject("base_price");
                    if (base != null) {
                        it.put("base_price", ((Number) base).doubleValue());
                    }
                    it.put("discount_percent", rs.getInt("discount_percent"));
                    it.put("qty", rs.getInt("qty"));
                    it.put("image_url", rs.getString("image_url"));
                    it.put("vendor_name", rs.getString("vendor_name"));
                    return it;
                }, userId);

        int total = 0;
        for (Map<String, Object> it : items) {
            total += (int) it.get("qty");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total_qty", total);
        return out;
    }

    public Map<String, Object> add(long userId, long productId, int qty) {
        if (qty < 1) qty = 1;
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id = ? AND status = 'active'", Integer.class, productId);
        if (exists == null || exists == 0) {
            throw ApiException.badRequest("Mehsul tapilmadi");
        }
        jdbc.update("INSERT INTO cart_items (user_id, product_id, qty) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty)", userId, productId, qty);
        return get(userId);
    }

    public Map<String, Object> updateQty(long userId, long productId, int qty) {
        if (qty <= 0) {
            jdbc.update("DELETE FROM cart_items WHERE user_id = ? AND product_id = ?", userId, productId);
        } else {
            jdbc.update("UPDATE cart_items SET qty = ? WHERE user_id = ? AND product_id = ?", qty, userId, productId);
        }
        return get(userId);
    }

    public void clear(long userId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id = ?", userId);
    }

    /** Sifaris ucun sebet setirleri (xam). */
    public List<CartLine> lines(long userId) {
        return jdbc.query(
                "SELECT p.id, p.name, p.price, p.image_url, c.qty "
                        + "FROM cart_items c JOIN products p ON p.id = c.product_id WHERE c.user_id = ?",
                (rs, n) -> new CartLine(
                        rs.getLong("id"), rs.getString("name"), rs.getDouble("price"),
                        rs.getInt("qty"), rs.getString("image_url")),
                userId);
    }

    public record CartLine(long productId, String name, double price, int qty, String imageUrl) {}
}
