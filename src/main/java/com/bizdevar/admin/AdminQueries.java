package com.bizdevar.admin;

import com.bizdevar.common.Json;
import com.bizdevar.order.OrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bizdevar.admin.AdminFormat.*;

/** Admin paneli ucun butun oxuma sorgulari. */
@Repository
public class AdminQueries {

    private final JdbcTemplate jdbc;
    private final OrderRepository orders;

    public AdminQueries(JdbcTemplate jdbc, OrderRepository orders) {
        this.jdbc = jdbc;
        this.orders = orders;
    }

    private int count(String sql, Object... args) {
        Integer c = jdbc.queryForObject(sql, Integer.class, args);
        return c == null ? 0 : c;
    }

    private double sum(String sql, Object... args) {
        Double c = jdbc.queryForObject(sql, Double.class, args);
        return c == null ? 0 : c;
    }

    // ---------- Dashboard ----------
    public Map<String, Object> dashboard() {
        double revenue = orders.totalRevenue();
        int orderCount = orders.countAll();
        Map<String, Integer> sc = orders.countByStatus();

        int userCount = count("SELECT COUNT(*) FROM users WHERE is_admin = 0");
        int productCount = count("SELECT COUNT(*) FROM products WHERE status = 'active'");
        int vendorCount = count("SELECT COUNT(*) FROM vendors WHERE status = 'active'");
        int pendingProducts = count("SELECT COUNT(*) FROM products WHERE status = 'pending'");
        int pendingOrders = sc.getOrDefault("placed", 0) + sc.getOrDefault("packing", 0);
        int delivered = sc.getOrDefault("delivered", 0);

        List<Map<String, Object>> latest = new ArrayList<>();
        List<Map<String, Object>> all = orders.adminList();
        for (int i = 0; i < all.size() && i < 6; i++) {
            Map<String, Object> o = all.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", o.get("id"));
            row.put("customer", o.getOrDefault("customer", "—"));
            row.put("seller", o.get("seller"));
            row.put("total", azn2((double) o.get("total")));
            row.put("status", statusLabel((String) o.get("status")));
            row.put("status_type", statusType((String) o.get("status")));
            latest.add(row);
        }

        int pendingApplications = count("SELECT COUNT(*) FROM vendors WHERE status IN ('pending','review')");
        int activeStores = count("SELECT COUNT(*) FROM vendors WHERE status='active'");
        int openTickets = count("SELECT COUNT(*) FROM support_tickets WHERE status IN ('new','in_progress')");
        int auditCnt = count("SELECT COUNT(*) FROM audit_logs");
        int couponCnt = count("SELECT COUNT(*) FROM coupons WHERE active = 1");
        int pendingTrx = count("SELECT COUNT(*) FROM transactions WHERE status = 'pending'");

        Map<String, Object> nav = new LinkedHashMap<>();
        nav.put("vendor-applications", pendingApplications);
        nav.put("stores", activeStores);
        nav.put("products", pendingProducts);
        nav.put("orders", orderCount);
        nav.put("payments", pendingTrx);
        nav.put("campaigns", couponCnt);
        nav.put("reviews", pendingProducts);
        nav.put("notifications", auditCnt);
        nav.put("support", openTickets);
        nav.put("security", auditCnt);

        List<Map<String, Object>> metrics = List.of(
                metric("Umumi gelir", azn(revenue), "+12%", "up"),
                metric("Umumi sifaris", String.valueOf(orderCount), "+8%", "up"),
                metric("Gozleyen sifaris", String.valueOf(pendingOrders), "—", "down"),
                metric("Tamamlanan sifaris", String.valueOf(delivered), "+5%", "up"),
                metric("Aktiv saticilar", String.valueOf(vendorCount), "—", "up"),
                metric("Umumi musteri", String.valueOf(userCount), "+3%", "up"),
                metric("Aktiv mehsul", String.valueOf(productCount), "—", "up"),
                metric("Tesdiq gozleyir", String.valueOf(pendingProducts), "—", "down")
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", metrics);
        out.put("latest_orders", latest);
        out.put("top_products", topProducts());
        out.put("top_vendors", topVendors());
        out.put("activities", activities());
        out.put("status_breakdown", statusBreakdown(sc, orderCount));
        out.put("nav_counts", nav);
        out.put("health_score", healthScore(orderCount, pendingOrders, pendingProducts));
        return out;
    }

    private List<Map<String, Object>> topProducts() {
        List<Map<String, Object>> out = jdbc.query(
                "SELECT p.name AS name, COUNT(oi.id) AS cnt FROM order_items oi "
                        + "JOIN products p ON p.id = oi.product_id GROUP BY p.id, p.name ORDER BY cnt DESC LIMIT 5",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", rs.getString("name"));
                    m.put("value", String.valueOf(rs.getInt("cnt")));
                    return m;
                });
        // En cox satilan mehsul yoxdursa, aktiv mehsullari goster
        if (out.isEmpty()) {
            out = jdbc.query("SELECT name, popular FROM products WHERE status='active' ORDER BY popular DESC LIMIT 5",
                    (rs, n) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", rs.getString("name"));
                        m.put("value", String.valueOf(rs.getInt("popular")));
                        return m;
                    });
        }
        int pct = 90;
        for (Map<String, Object> m : out) {
            m.put("percent", Math.max(20, pct));
            pct -= 12;
        }
        return out;
    }

    private List<Map<String, Object>> topVendors() {
        List<Map<String, Object>> out = jdbc.query(
                "SELECT name, revenue FROM vendors ORDER BY revenue DESC LIMIT 5",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", rs.getString("name"));
                    m.put("revenue", rs.getDouble("revenue"));
                    return m;
                });
        double max = 1;
        for (Map<String, Object> m : out) max = Math.max(max, (double) m.get("revenue"));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Map<String, Object> m : out) {
            double rev = (double) m.get("revenue");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("name", m.get("name"));
            r.put("percent", (int) (rev / max * 100));
            r.put("value", "₼" + String.format("%.0fK", rev / 1000));
            res.add(r);
        }
        return res;
    }

    private List<Map<String, Object>> activities() {
        return jdbc.query(
                "SELECT event_type, resource, created_at FROM audit_logs ORDER BY id DESC LIMIT 6",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("title", rs.getString("event_type"));
                    m.put("text", rs.getString("resource"));
                    m.put("time", rs.getString("created_at"));
                    return m;
                });
    }

    private List<Map<String, Object>> statusBreakdown(Map<String, Integer> counts, int total) {
        if (total == 0) total = 1;
        String[][] labels = {
                {"delivered", "Catdirildi", "#0f766e"},
                {"packing", "Hazirlanir", "#1d4ed8"},
                {"delivering", "Yoldadir", "#f59e0b"},
                {"cancelled", "Legv", "#b42318"}
        };
        List<Map<String, Object>> out = new ArrayList<>();
        for (String[] l : labels) {
            int c = counts.getOrDefault(l[0], 0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", l[1]);
            m.put("percent", (int) ((double) c / total * 100));
            m.put("color", l[2]);
            m.put("count", c);
            out.add(m);
        }
        return out;
    }

    private double healthScore(int orderCount, int pendingOrders, int pendingProducts) {
        if (orderCount == 0) return 100.0;
        double score = 100.0 - pendingOrders * 2 - pendingProducts * 0.5;
        return Math.max(50, score);
    }

    // ---------- Vendors / Stores ----------
    public Map<String, Object> vendorApplications() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT id, name, category, verification_status, revenue, rating, status, created_at "
                        + "FROM vendors WHERE status IN ('pending','review') ORDER BY id DESC",
                this::mapVendorListRow);

        int pendingOnly = count("SELECT COUNT(*) FROM vendors WHERE status='pending'");
        int review = count("SELECT COUNT(*) FROM vendors WHERE status='review'");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Gozleyen muraciet", String.valueOf(pendingOnly), "—", "up"),
                metric("Yoxlamada", String.valueOf(review), "—", "up"),
                metric("Cemi sorğu", String.valueOf(list.size()), "—", "up")
        ));
        out.put("applications", list);
        return out;
    }

    public Map<String, Object> stores() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT id, name, category, verification_status, revenue, rating, status, created_at "
                        + "FROM vendors WHERE status IN ('active','restricted') ORDER BY revenue DESC",
                this::mapVendorListRow);

        int active = count("SELECT COUNT(*) FROM vendors WHERE status='active'");
        int restricted = count("SELECT COUNT(*) FROM vendors WHERE status='restricted'");
        double avg = sum("SELECT COALESCE(AVG(rating),0) FROM vendors WHERE status='active'");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Aktiv magaza", String.valueOf(active), "—", "up"),
                metric("Mehdud magaza", String.valueOf(restricted), "—", "down"),
                metric("Orta reytinq", String.format("%.2f", avg), "—", "up")
        ));
        out.put("stores", list);
        return out;
    }

    private Map<String, Object> mapVendorListRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("name", rs.getString("name"));
        m.put("category", rs.getString("category"));
        m.put("verification", verLabel(rs.getString("verification_status")));
        m.put("verification_type", verType(rs.getString("verification_status")));
        m.put("revenue", azn(rs.getDouble("revenue")));
        m.put("rating", String.format("%.1f", rs.getDouble("rating")));
        String rawStatus = rs.getString("status");
        m.put("raw_status", rawStatus);
        m.put("status", statusLabel(rawStatus));
        m.put("status_type", statusType(rawStatus));
        java.sql.Timestamp created = rs.getTimestamp("created_at");
        m.put("created_at", created != null ? formatTs(created) : "");
        return m;
    }

    /** @deprecated admin panel stores() istifade edir */
    public Map<String, Object> vendors() {
        return stores();
    }

    public Map<String, Object> vendorDetail(long id) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT v.id, v.seller_id, v.user_id, v.name, v.category, v.verification_status, v.status, "
                        + "v.store_type, v.voen, v.phone, v.revenue, v.rating, v.rejection_reason, v.auto_named, v.created_at, "
                        + "s.email AS seller_email, s.owner_name, s.owner_surname, s.phone AS seller_phone, "
                        + "s.store_name AS seller_store_name, s.store_type AS seller_store_type, s.voen AS seller_voen, "
                        + "s.verification_status AS seller_verification, s.status AS seller_status, "
                        + "s.rejection_reason AS seller_rejection_reason, s.approved_at, s.rejected_at, s.created_at AS seller_created_at, "
                        + "u.email AS user_email, u.name AS user_name, u.phone AS user_phone, "
                        + "(SELECT COUNT(*) FROM products p WHERE p.vendor_id = v.id AND p.status != 'deleted') AS product_count "
                        + "FROM vendors v "
                        + "LEFT JOIN sellers s ON s.id = v.seller_id "
                        + "LEFT JOIN users u ON u.id = v.user_id "
                        + "WHERE v.id = ?",
                (rs, n) -> {
                    String status = rs.getString("status");
                    String verification = rs.getString("verification_status");
                    String storeType = rs.getString("store_type");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("seller_id", rs.getObject("seller_id"));
                    m.put("name", rs.getString("name"));
                    m.put("category", rs.getString("category"));
                    m.put("verification_status", verification);
                    m.put("verification_label", verLabel(verification));
                    m.put("verification_type", verType(verification));
                    m.put("status", status);
                    m.put("status_label", statusLabel(status));
                    m.put("status_type", statusType(status));
                    m.put("store_type", storeType);
                    m.put("store_type_label", storeTypeLabel(storeType));
                    m.put("voen", nonEmpty(rs.getString("voen"), rs.getString("seller_voen")));
                    m.put("phone", nonEmpty(rs.getString("phone"), rs.getString("seller_phone"), rs.getString("user_phone")));
                    m.put("revenue", rs.getDouble("revenue"));
                    m.put("revenue_label", azn(rs.getDouble("revenue")));
                    m.put("rating", String.format("%.1f", rs.getDouble("rating")));
                    m.put("rejection_reason", nonEmpty(rs.getString("rejection_reason"), rs.getString("seller_rejection_reason")));
                    m.put("auto_named", rs.getInt("auto_named") == 1);
                    m.put("product_count", rs.getInt("product_count"));
                    m.put("created_at", formatTs(rs.getTimestamp("created_at")));

                    String ownerName = trim(rs.getString("owner_name"));
                    String ownerSurname = trim(rs.getString("owner_surname"));
                    String ownerFull = (ownerName + " " + ownerSurname).trim();
                    if (ownerFull.isEmpty()) ownerFull = trim(rs.getString("user_name"));
                    m.put("owner_name", ownerFull.isEmpty() ? "—" : ownerFull);

                    String email = nonEmpty(rs.getString("seller_email"), rs.getString("user_email"));
                    m.put("email", email.isEmpty() ? "—" : email);

                    m.put("seller_status", rs.getString("seller_status"));
                    m.put("seller_status_label", statusLabel(rs.getString("seller_status")));
                    m.put("registered_at", formatTs(rs.getTimestamp("seller_created_at")));
                    if (rs.getTimestamp("approved_at") != null) {
                        m.put("approved_at", formatTs(rs.getTimestamp("approved_at")));
                    }
                    if (rs.getTimestamp("rejected_at") != null) {
                        m.put("rejected_at", formatTs(rs.getTimestamp("rejected_at")));
                    }
                    return m;
                }, id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("vendor", list.isEmpty() ? Map.of() : list.get(0));
        return out;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    private static String formatTs(java.sql.Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().toString().replace("T", " ");
    }

    private static String storeTypeLabel(String type) {
        if (type == null || type.isBlank()) return "—";
        return switch (type) {
            case "voenli" -> "VOEN ilə";
            case "voensiz" -> "VOEN-siz";
            case "online" -> "Onlayn mağaza";
            default -> type;
        };
    }

    // ---------- Products ----------
    public Map<String, Object> products() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT p.id, p.name, COALESCE(v.name,'—') AS vendor, p.price, p.stock, p.status, p.moderation_action, "
                        + "p.rejection_reason, p.deletion_reason "
                        + "FROM products p LEFT JOIN vendors v ON v.id = p.vendor_id "
                        + "WHERE p.status != 'deleted' ORDER BY p.id DESC",
                (rs, n) -> {
                    String status = rs.getString("status");
                    String action = rs.getString("moderation_action");
                    String label = productStatusLabel(status);
                    if ("create".equals(action)) label = "Yeni — tesdiq gozleyir";
                    else if ("update".equals(action)) label = "Deyisiklik — tesdiq gozleyir";
                    else if ("delete".equals(action)) label = "Silinme — tesdiq gozleyir";
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("name", rs.getString("name"));
                    m.put("vendor", rs.getString("vendor"));
                    m.put("price", azn(rs.getDouble("price")));
                    m.put("stock", rs.getInt("stock"));
                    m.put("status", label);
                    m.put("status_type", productStatusType(status));
                    m.put("moderation_action", action);
                    m.put("rejection_reason", rs.getString("rejection_reason"));
                    m.put("deletion_reason", rs.getString("deletion_reason"));
                    return m;
                });

        int active = count("SELECT COUNT(*) FROM products WHERE status='active'");
        int pending = count("SELECT COUNT(*) FROM products WHERE status='pending'");
        int complaint = count("SELECT COUNT(*) FROM products WHERE status='complaint'");
        int totalStock = count("SELECT COALESCE(SUM(stock),0) FROM products");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Aktiv mehsul", String.valueOf(active), "—", "up"),
                metric("Tesdiq gozleyir", String.valueOf(pending), "—", "down"),
                metric("Sikayet", String.valueOf(complaint), "—", "down"),
                metric("Umumi stok", String.valueOf(totalStock), "—", "up"),
                metric("Cemi mehsul", String.valueOf(active + pending + complaint), "—", "up")
        ));
        out.put("products", list);
        return out;
    }

    public Map<String, Object> productDetail(long id) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT p.id, p.vendor_id, p.name, p.slug, p.category_slug, p.price, p.base_price, p.discount_percent, "
                        + "p.stock, p.status, p.description, p.image_url, p.images_json, p.specs_json, "
                        + "p.rejection_reason, p.deletion_reason, p.moderation_action, p.created_at, "
                        + "COALESCE(v.name,'—') AS vendor, COALESCE(c.name, p.category_slug) AS category_name "
                        + "FROM products p "
                        + "LEFT JOIN vendors v ON v.id = p.vendor_id "
                        + "LEFT JOIN categories c ON c.slug = p.category_slug "
                        + "WHERE p.id = ?",
                (rs, n) -> {
                    String status = rs.getString("status");
                    String action = rs.getString("moderation_action");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("vendor_id", rs.getLong("vendor_id"));
                    m.put("name", rs.getString("name"));
                    m.put("slug", rs.getString("slug"));
                    m.put("category_slug", rs.getString("category_slug"));
                    m.put("category_name", rs.getString("category_name"));
                    m.put("vendor", rs.getString("vendor"));
                    m.put("price", rs.getDouble("price"));
                    m.put("price_label", azn(rs.getDouble("price")));
                    double base = rs.getDouble("base_price");
                    if (!rs.wasNull()) {
                        m.put("base_price", base);
                        m.put("base_price_label", azn(base));
                    }
                    int discount = rs.getInt("discount_percent");
                    m.put("discount_percent", discount);
                    m.put("stock", rs.getInt("stock"));
                    m.put("status", status);
                    m.put("status_label", productStatusLabel(status));
                    m.put("status_type", productStatusType(status));
                    m.put("moderation_action", action);
                    m.put("moderation_label", moderationLabel(action));
                    m.put("description", rs.getString("description"));
                    String cover = rs.getString("image_url");
                    m.put("image_url", cover);
                    List<String> images = mergeImages(cover, Json.readStringList(rs.getString("images_json")));
                    m.put("images", images);
                    m.put("image_count", images.size());
                    m.put("specs", Json.readStringMap(rs.getString("specs_json")));
                    m.put("rejection_reason", rs.getString("rejection_reason"));
                    m.put("deletion_reason", rs.getString("deletion_reason"));
                    m.put("created_at", rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime().toString().replace("T", " ")
                            : "");
                    return m;
                }, id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("product", list.isEmpty() ? Map.of() : list.get(0));
        return out;
    }

    private static String moderationLabel(String action) {
        if (action == null || action.isBlank()) return "—";
        return switch (action) {
            case "create" -> "Yeni mehsul";
            case "update" -> "Deyisiklik";
            case "delete" -> "Silinme sorğusu";
            default -> action;
        };
    }

    private static List<String> mergeImages(String cover, List<String> extra) {
        List<String> out = new ArrayList<>();
        if (cover != null && !cover.isBlank()) out.add(cover.trim());
        if (extra != null) {
            for (String img : extra) {
                if (img == null || img.isBlank()) continue;
                String u = img.trim();
                if (!out.contains(u)) out.add(u);
            }
        }
        return out;
    }

    // ---------- Categories ----------
    public Map<String, Object> categories() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT c.slug, c.name, c.status, (SELECT COUNT(*) FROM products p WHERE p.category_slug = c.slug) AS cnt "
                        + "FROM categories c ORDER BY c.name",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("slug", rs.getString("slug"));
                    m.put("name", rs.getString("name"));
                    m.put("product_count", rs.getInt("cnt"));
                    m.put("status", statusLabel(rs.getString("status")));
                    m.put("status_type", "success");
                    return m;
                });
        int cnt = count("SELECT COUNT(*) FROM categories");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Kateqoriya", String.valueOf(cnt), "—", "up"),
                metric("Aktiv", String.valueOf(cnt), "—", "up"),
                metric("Draft", "0", "—", "up"),
                metric("SEO", "OK", "—", "up"),
                metric("Vitrin", String.valueOf(cnt), "—", "up")
        ));
        out.put("categories", list);
        return out;
    }

    // ---------- Orders ----------
    public Map<String, Object> ordersList() {
        List<Map<String, Object>> raw = orders.adminList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> o : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            String status = (String) o.get("status");
            m.put("id", o.get("id"));
            m.put("customer", o.getOrDefault("customer", "—"));
            m.put("seller", o.get("seller"));
            m.put("total", azn2((double) o.get("total")));
            m.put("status", statusLabel(status));
            m.put("status_type", statusType(status));
            m.put("raw_status", status);
            out.add(m);
        }
        Map<String, Integer> sc = orders.countByStatus();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("metrics", List.of(
                metric("Verildi", String.valueOf(sc.getOrDefault("placed", 0)), "—", "down"),
                metric("Hazirlanir", String.valueOf(sc.getOrDefault("packing", 0)), "—", "up"),
                metric("Kargoda", String.valueOf(sc.getOrDefault("cargo", 0)), "—", "up"),
                metric("Catdirilir", String.valueOf(sc.getOrDefault("delivering", 0)), "—", "up"),
                metric("Tamamlandi", String.valueOf(sc.getOrDefault("delivered", 0)), "—", "up")
        ));
        res.put("orders", out);
        res.put("status_counts", sc);
        res.put("kanban", kanban(out));
        return res;
    }

    private Map<String, Object> kanban(List<Map<String, Object>> ordersList) {
        String[] cols = {"placed", "packing", "cargo", "delivering", "delivered", "cancelled"};
        Map<String, Object> board = new LinkedHashMap<>();
        for (String c : cols) board.put(c, new ArrayList<Map<String, Object>>());
        for (Map<String, Object> o : ordersList) {
            String st = (String) o.getOrDefault("raw_status", "placed");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bucket = (List<Map<String, Object>>) board.getOrDefault(st, null);
            if (bucket == null) { st = "placed"; bucket = (List<Map<String, Object>>) board.get(st); }
            if (bucket.size() >= 4) continue;
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", o.get("id"));
            card.put("total", o.get("total"));
            bucket.add(card);
        }
        return board;
    }

    // ---------- Customers ----------
    public Map<String, Object> customers() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT u.id, u.name, u.email, "
                        + "(SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) AS order_count, "
                        + "COALESCE((SELECT SUM(total) FROM orders o WHERE o.user_id = u.id),0) AS clv "
                        + "FROM users u WHERE u.is_admin = 0 ORDER BY clv DESC LIMIT 100",
                (rs, n) -> {
                    double clv = rs.getDouble("clv");
                    String tier = clv > 5000 ? "VIP" : (clv > 1000 ? "Premium" : "Standart");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("name", rs.getString("name"));
                    m.put("email", rs.getString("email"));
                    m.put("tier", tier);
                    m.put("orders", rs.getInt("order_count") + " sifaris");
                    m.put("clv", azn(clv));
                    m.put("status", "Aktiv");
                    m.put("status_type", "success");
                    return m;
                });
        int vip = count("SELECT COUNT(*) FROM (SELECT user_id FROM orders GROUP BY user_id HAVING SUM(total) > 5000) t");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Umumi musteri", String.valueOf(list.size()), "—", "up"),
                metric("VIP musteri", String.valueOf(vip), "—", "up"),
                metric("Aktiv hesab", String.valueOf(list.size()), "—", "up"),
                metric("Orta CLV", "—", "—", "up"),
                metric("Risk hesab", "0", "—", "up")
        ));
        out.put("customers", list);
        return out;
    }

    // ---------- Coupons / campaigns ----------
    public Map<String, Object> coupons() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT code, discount_percent, active FROM coupons ORDER BY id DESC",
                (rs, n) -> {
                    int active = rs.getInt("active");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", rs.getString("code"));
                    m.put("discount", rs.getInt("discount_percent"));
                    m.put("status", active == 1 ? "Aktiv" : "Deaktiv");
                    m.put("status_type", active == 1 ? "success" : "neutral");
                    return m;
                });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Aktiv kupon", String.valueOf(list.size()), "—", "up"),
                metric("Kampaniya", String.valueOf(list.size()), "—", "up"),
                metric("Promo geliri", "—", "—", "up"),
                metric("Banner CTR", "—", "—", "up"),
                metric("Flash sale", "0", "—", "up")
        ));
        out.put("coupons", list);
        return out;
    }

    // ---------- Payments ----------
    public Map<String, Object> payments() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT trx_id, title, amount, trx_type, status FROM transactions ORDER BY id DESC LIMIT 100",
                (rs, n) -> {
                    String status = rs.getString("status");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getString("trx_id"));
                    m.put("title", rs.getString("title"));
                    m.put("amount", azn2(rs.getDouble("amount")));
                    m.put("type", rs.getString("trx_type"));
                    m.put("status", "pending".equals(status) ? "Gozleyir" : statusLabel(status));
                    m.put("status_type", "pending".equals(status) ? "warning" : "success");
                    return m;
                });
        double revenue = orders.totalRevenue();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Umumi gelir", azn(revenue), "—", "up"),
                metric("Gozleyen", String.valueOf(count("SELECT COUNT(*) FROM transactions WHERE status='pending'")), "—", "down"),
                metric("Komissiya", "8%", "—", "up"),
                metric("Geri odeme", "0", "—", "up"),
                metric("Tranzaksiya", String.valueOf(list.size()), "—", "up")
        ));
        out.put("transactions", list);
        return out;
    }

    // ---------- Shipping ----------
    public Map<String, Object> shipping() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT name, zone, rate, status FROM shipping_providers ORDER BY id",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", rs.getString("name"));
                    m.put("zone", rs.getString("zone"));
                    m.put("rate", azn2(rs.getDouble("rate")));
                    m.put("status", statusLabel(rs.getString("status")));
                    m.put("status_type", statusType(rs.getString("status")));
                    return m;
                });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Provayder", String.valueOf(list.size()), "—", "up"),
                metric("Aktiv zona", String.valueOf(list.size()), "—", "up"),
                metric("Orta tarif", "—", "—", "up"),
                metric("Catdirilma", "1-3 gun", "—", "up"),
                metric("Pulsuz limit", "₼ 50", "—", "up")
        ));
        out.put("providers", list);
        return out;
    }

    // ---------- CMS ----------
    public Map<String, Object> cms() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT id, slug, title, content_type, status, updated_at FROM cms_pages ORDER BY id",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("slug", rs.getString("slug"));
                    m.put("title", rs.getString("title"));
                    m.put("type", rs.getString("content_type"));
                    m.put("status", statusLabel(rs.getString("status")));
                    m.put("status_type", "published".equals(rs.getString("status")) ? "success" : "warning");
                    m.put("updated", rs.getString("updated_at"));
                    return m;
                });
        int published = count("SELECT COUNT(*) FROM cms_pages WHERE status='published'");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Cemi mezmun", String.valueOf(list.size()), "—", "up"),
                metric("Yayimlanib", String.valueOf(published), "—", "up"),
                metric("Draft", String.valueOf(list.size() - published), "—", "down"),
                metric("FAQ", "1", "—", "up"),
                metric("SEO", "OK", "—", "up")
        ));
        out.put("pages", list);
        return out;
    }

    // ---------- Settings ----------
    public Map<String, Object> settings() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT setting_key, setting_value, group_name, status FROM settings ORDER BY group_name, setting_key",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", rs.getString("setting_key"));
                    m.put("value", rs.getString("setting_value"));
                    m.put("group", rs.getString("group_name"));
                    m.put("status", rs.getString("status"));
                    return m;
                });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Ayar sayi", String.valueOf(list.size()), "—", "up"),
                metric("Gateway", "1", "—", "up"),
                metric("Valyuta", "AZN", "—", "up"),
                metric("Dil", "az", "—", "up"),
                metric("Template", "8", "—", "up")
        ));
        out.put("settings", list);
        return out;
    }

    // ---------- Support ----------
    public Map<String, Object> supportTickets() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT ticket_number, customer_name, subject, priority, status FROM support_tickets ORDER BY id DESC",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getString("ticket_number"));
                    m.put("customer", rs.getString("customer_name"));
                    m.put("subject", rs.getString("subject"));
                    m.put("priority", rs.getString("priority"));
                    m.put("status", rs.getString("status"));
                    return m;
                });
        int open = count("SELECT COUNT(*) FROM support_tickets WHERE status IN ('new','in_progress')");
        int critical = count("SELECT COUNT(*) FROM support_tickets WHERE priority='critical'");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Aciq ticket", String.valueOf(open), "—", "down"),
                metric("Kritik", String.valueOf(critical), "—", "down"),
                metric("Cemi ticket", String.valueOf(list.size()), "—", "up"),
                metric("SLA", "96%", "—", "up"),
                metric("Staff", "1", "—", "up")
        ));
        out.put("tickets", list);
        return out;
    }

    // ---------- Audit ----------
    public Map<String, Object> auditLogs() {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT a.event_type, COALESCE(u.name,'Sistem') AS admin, a.ip, a.resource, a.status, a.created_at "
                        + "FROM audit_logs a LEFT JOIN users u ON u.id = a.admin_id ORDER BY a.id DESC LIMIT 50",
                (rs, n) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("event", rs.getString("event_type"));
                    m.put("admin", rs.getString("admin"));
                    m.put("ip", rs.getString("ip"));
                    m.put("resource", rs.getString("resource"));
                    m.put("status", rs.getString("status"));
                    m.put("created_at", rs.getString("created_at"));
                    return m;
                });
        int admins = count("SELECT COUNT(*) FROM admins");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Audit qeydi", String.valueOf(list.size()), "—", "up"),
                metric("Admin sayi", String.valueOf(admins), "—", "up"),
                metric("2FA", "—", "—", "up"),
                metric("Risk login", "0", "—", "up"),
                metric("Backup", "Gunluk", "OK", "up")
        ));
        out.put("logs", list);
        return out;
    }

    // ---------- Reviews / Reports / Notifications (sade) ----------
    public Map<String, Object> reviews() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Rey sayi", "0", "—", "up"),
                metric("Orta bal", "—", "—", "up"),
                metric("Gozleyen", "0", "—", "down"),
                metric("Sikayet", String.valueOf(count("SELECT COUNT(*) FROM products WHERE status='complaint'")), "—", "down"),
                metric("Tesdiqli", "0", "—", "up")
        ));
        out.put("reviews", List.of());
        return out;
    }

    public Map<String, Object> reports() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Umumi gelir", azn(orders.totalRevenue()), "—", "up"),
                metric("Sifaris", String.valueOf(orders.countAll()), "—", "up"),
                metric("Musteri", String.valueOf(count("SELECT COUNT(*) FROM users WHERE is_admin=0")), "—", "up"),
                metric("Mehsul", String.valueOf(count("SELECT COUNT(*) FROM products WHERE status='active'")), "—", "up"),
                metric("Satici", String.valueOf(count("SELECT COUNT(*) FROM vendors WHERE status='active'")), "—", "up")
        ));
        out.put("status_breakdown", statusBreakdown(orders.countByStatus(), orders.countAll()));
        out.put("top_products", topProducts());
        return out;
    }

    public Map<String, Object> notifications() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", List.of(
                metric("Bildiris", String.valueOf(count("SELECT COUNT(*) FROM audit_logs")), "—", "up"),
                metric("Oxunmamis", "0", "—", "down"),
                metric("Push", "Aktiv", "—", "up"),
                metric("Email", "Aktiv", "—", "up"),
                metric("SMS", "Aktiv", "—", "up")
        ));
        out.put("notifications", activities());
        return out;
    }

    public Map<String, Object> search(String q) {
        String like = "%" + (q == null ? "" : q.toLowerCase()) + "%";
        List<Map<String, Object>> products = jdbc.query(
                "SELECT id, name FROM products WHERE LOWER(name) LIKE ? LIMIT 8",
                (rs, n) -> Map.of("id", rs.getLong("id"), "name", rs.getString("name"), "type", "product"), like);
        List<Map<String, Object>> vendors = jdbc.query(
                "SELECT id, name FROM vendors WHERE LOWER(name) LIKE ? LIMIT 8",
                (rs, n) -> Map.of("id", rs.getLong("id"), "name", rs.getString("name"), "type", "vendor"), like);
        List<Map<String, Object>> results = new ArrayList<>();
        results.addAll(products);
        results.addAll(vendors);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("results", results);
        return out;
    }
}
