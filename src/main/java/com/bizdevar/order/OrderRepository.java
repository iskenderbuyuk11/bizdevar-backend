package com.bizdevar.order;

import com.bizdevar.cart.CartRepository;
import com.bizdevar.common.ApiException;
import com.bizdevar.common.Json;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class OrderRepository {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JdbcTemplate jdbc;
    private final CartRepository carts;

    public OrderRepository(JdbcTemplate jdbc, CartRepository carts) {
        this.jdbc = jdbc;
        this.carts = carts;
    }

    public List<Map<String, Object>> listByUser(long userId) {
        List<Map<String, Object>> orders = jdbc.query(
                "SELECT id, order_number, status, total, seller, delivery_json, created_at "
                        + "FROM orders WHERE user_id = ? ORDER BY created_at DESC",
                (rs, n) -> baseOrder(rs.getLong("id"), rs.getString("order_number"), rs.getString("status"),
                        rs.getDouble("total"), rs.getString("seller"), rs.getString("delivery_json"),
                        rs.getTimestamp("created_at"), null),
                userId);
        for (Map<String, Object> o : orders) {
            o.put("items", items((long) o.remove("_oid")));
        }
        return orders;
    }

    public Map<String, Object> getByNumber(long userId, String orderNum) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT id, order_number, status, total, seller, delivery_json, created_at "
                        + "FROM orders WHERE user_id = ? AND order_number = ?",
                (rs, n) -> baseOrder(rs.getLong("id"), rs.getString("order_number"), rs.getString("status"),
                        rs.getDouble("total"), rs.getString("seller"), rs.getString("delivery_json"),
                        rs.getTimestamp("created_at"), null),
                userId, orderNum);
        if (list.isEmpty()) throw ApiException.notFound("Sifaris tapilmadi");
        Map<String, Object> o = list.get(0);
        o.put("items", items((long) o.remove("_oid")));
        return o;
    }

    @Transactional
    public Map<String, Object> create(long userId, Map<String, Object> delivery, int promoPct) {
        List<CartRepository.CartLine> lines = carts.lines(userId);
        if (lines.isEmpty()) throw ApiException.badRequest("Sebet bosdur");

        double subtotal = 0;
        for (CartRepository.CartLine l : lines) subtotal += l.price() * l.qty();
        double discount = subtotal * promoPct / 100.0;
        double total = Math.max(0, subtotal - discount);

        String orderNum = "BV-" + (ThreadLocalRandom.current().nextInt(900000) + 100000);
        String deliveryJson = delivery == null ? "{}" : Json.write(delivery);

        String sellerName = "BizdeVar";
        Long vendorId = null;
        CartRepository.CartLine first = lines.get(0);
        List<Map<String, Object>> v = jdbc.query(
                "SELECT v.id AS vid, v.name AS vname FROM products p JOIN vendors v ON v.id = p.vendor_id WHERE p.id = ?",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("vid"));
                    m.put("name", rs.getString("vname"));
                    return m;
                }, first.productId());
        if (!v.isEmpty()) {
            sellerName = (String) v.get(0).get("name");
            vendorId = (Long) v.get(0).get("id");
        }

        jdbc.update("INSERT INTO orders (order_number, user_id, vendor_id, status, total, seller, delivery_json) "
                        + "VALUES (?, ?, ?, 'placed', ?, ?, ?)",
                orderNum, userId, vendorId, total, sellerName, deliveryJson);
        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_number = ?", Long.class, orderNum);

        for (CartRepository.CartLine l : lines) {
            jdbc.update("INSERT INTO order_items (order_id, product_id, name, price, qty, image_url) VALUES (?, ?, ?, ?, ?, ?)",
                    orderId, l.productId(), l.name(), l.price(), l.qty(), l.imageUrl());
        }
        jdbc.update("DELETE FROM cart_items WHERE user_id = ?", userId);

        return getByNumber(userId, orderNum);
    }

    public int validatePromo(String code) {
        if (code == null || code.isBlank()) throw ApiException.badRequest("invalid");
        List<Map<String, Object>> list = jdbc.query(
                "SELECT discount_percent, active FROM coupons WHERE code = ?",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("pct", rs.getInt("discount_percent"));
                    m.put("active", rs.getInt("active"));
                    return m;
                }, code.trim().toUpperCase());
        if (list.isEmpty() || (int) list.get(0).get("active") != 1) {
            throw ApiException.badRequest("invalid");
        }
        return (int) list.get(0).get("pct");
    }

    // ---- Admin ----
    public List<Map<String, Object>> adminList() {
        List<Map<String, Object>> orders = jdbc.query(
                "SELECT o.id, o.order_number, o.status, o.total, o.seller, o.delivery_json, o.created_at, u.name AS customer "
                        + "FROM orders o JOIN users u ON u.id = o.user_id ORDER BY o.created_at DESC LIMIT 100",
                (rs, n) -> baseOrder(rs.getLong("id"), rs.getString("order_number"), rs.getString("status"),
                        rs.getDouble("total"), rs.getString("seller"), rs.getString("delivery_json"),
                        rs.getTimestamp("created_at"), rs.getString("customer")),
                new Object[]{});
        for (Map<String, Object> o : orders) o.remove("_oid");
        return orders;
    }

    public Map<String, Object> adminGetByNumber(String orderNum) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT o.id, o.order_number, o.status, o.total, o.seller, o.delivery_json, o.created_at, u.name AS customer "
                        + "FROM orders o JOIN users u ON u.id = o.user_id WHERE o.order_number = ?",
                (rs, n) -> baseOrder(rs.getLong("id"), rs.getString("order_number"), rs.getString("status"),
                        rs.getDouble("total"), rs.getString("seller"), rs.getString("delivery_json"),
                        rs.getTimestamp("created_at"), rs.getString("customer")),
                orderNum);
        if (list.isEmpty()) throw ApiException.notFound("Sifaris tapilmadi");
        Map<String, Object> o = list.get(0);
        o.put("items", items((long) o.remove("_oid")));
        return o;
    }

    public void updateStatus(String orderNum, String status) {
        jdbc.update("UPDATE orders SET status = ? WHERE order_number = ?", status, orderNum);
    }

    public Map<String, Integer> countByStatus() {
        Map<String, Integer> m = new LinkedHashMap<>();
        jdbc.query("SELECT status, COUNT(*) AS c FROM orders GROUP BY status", rs -> {
            m.put(rs.getString("status"), rs.getInt("c"));
        });
        return m;
    }

    public double totalRevenue() {
        Double t = jdbc.queryForObject("SELECT COALESCE(SUM(total),0) FROM orders WHERE status != 'cancelled'", Double.class);
        return t == null ? 0 : t;
    }

    public int countAll() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        return c == null ? 0 : c;
    }

    private List<Map<String, Object>> items(long orderId) {
        return jdbc.query(
                "SELECT product_id, name, price, qty, image_url FROM order_items WHERE order_id = ?",
                (rs, n) -> {
                    Map<String, Object> it = new LinkedHashMap<>();
                    long pid = rs.getLong("product_id");
                    if (!rs.wasNull()) it.put("product_id", pid);
                    it.put("name", rs.getString("name"));
                    it.put("price", rs.getDouble("price"));
                    it.put("qty", rs.getInt("qty"));
                    String img = rs.getString("image_url");
                    if (img != null) {
                        it.put("image_url", img);
                        it.put("image", img);
                    }
                    return it;
                }, orderId);
    }

    private Map<String, Object> baseOrder(long id, String orderNum, String status, double total,
                                          String seller, String deliveryJson, Timestamp createdAt, String customer) {
        Map<String, Object> o = new LinkedHashMap<>();
        LocalDateTime t = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
        o.put("id", orderNum);
        o.put("date", t.format(DATE_FMT));
        o.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(t.toInstant(ZoneOffset.UTC)));
        o.put("status", status);
        o.put("total", total);
        o.put("seller", seller);
        o.put("delivery", Json.readObject(deliveryJson));
        if (customer != null) o.put("customer", customer);
        o.put("_oid", id); // daxili: items ucun order id, sonra silinir
        return o;
    }
}
