package com.bizdevar.seller;

import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.common.ApiException;
import com.bizdevar.common.Json;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SellerRepository {

    private final JdbcTemplate jdbc;

    public SellerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasVendor(long userId) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM vendors WHERE user_id = ?", Integer.class, userId);
        return c != null && c > 0;
    }

    public long createVendor(long userId, SellerRegisterRequest req) {
        if (hasVendor(userId)) {
            throw ApiException.conflict("Artiq satici hesabiniz var");
        }
        String storeName = req.storeName == null ? "" : req.storeName.trim();
        boolean autoNamed = false;
        if (storeName.isEmpty()) {
            autoNamed = true;
            storeName = "Magaza-" + userId;
        }
        String category = req.category == null ? "" : req.category.trim();
        String storeType = (req.storeType == null || req.storeType.isBlank()) ? "online" : req.storeType.trim();
        String voen = req.voen == null ? "" : req.voen.trim();
        String phone = req.phone == null ? "" : req.phone.trim();

        jdbc.update("INSERT INTO vendors (user_id, name, category, verification_status, status, store_type, voen, phone, auto_named) "
                        + "VALUES (?, ?, ?, 'pending', 'pending', ?, ?, ?, ?)",
                userId, storeName, category, storeType, voen, phone, autoNamed ? 1 : 0);

        return jdbc.queryForObject("SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
    }

    public VendorInfo vendorByUserId(long userId) {
        List<VendorInfo> list = jdbc.query(
                "SELECT id, name, category, status, verification_status, store_type, rejection_reason, auto_named "
                        + "FROM vendors WHERE user_id = ?",
                (rs, n) -> {
                    VendorInfo v = new VendorInfo();
                    v.id = rs.getLong("id");
                    v.name = rs.getString("name");
                    v.category = rs.getString("category");
                    v.status = rs.getString("status");
                    v.verificationStatus = rs.getString("verification_status");
                    v.storeType = rs.getString("store_type");
                    v.rejectionReason = rs.getString("rejection_reason");
                    v.autoNamed = rs.getInt("auto_named") == 1;
                    return v;
                }, userId);
        if (list.isEmpty()) {
            throw ApiException.forbidden("Satici hesabi tapilmadi");
        }
        return list.get(0);
    }

    public Map<String, Object> dashboard(long vendorId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ?", Integer.class, vendorId);
        Integer active = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ? AND status = 'active'", Integer.class, vendorId);
        Integer pending = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ? AND status = 'pending'", Integer.class, vendorId);
        Double revenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(o.total),0) FROM orders o WHERE o.vendor_id = ? AND o.status != 'cancelled'",
                Double.class, vendorId);
        Integer orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE vendor_id = ?", Integer.class, vendorId);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_products", total == null ? 0 : total);
        m.put("active_products", active == null ? 0 : active);
        m.put("pending_products", pending == null ? 0 : pending);
        m.put("orders", orderCount == null ? 0 : orderCount);
        m.put("revenue", revenue == null ? 0 : revenue);
        return m;
    }

    public List<Map<String, Object>> listProducts(long vendorId) {
        return jdbc.query(
                "SELECT id, name, category_slug, price, base_price, discount_percent, stock, image_url, status, moderation_action "
                        + "FROM products WHERE vendor_id = ? AND status != 'deleted' ORDER BY id DESC",
                (rs, n) -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("id", rs.getLong("id"));
                    p.put("name", rs.getString("name"));
                    p.put("category_slug", rs.getString("category_slug"));
                    p.put("price", rs.getDouble("price"));
                    double base = rs.getDouble("base_price");
                    if (!rs.wasNull()) p.put("base_price", base);
                    p.put("discount_percent", rs.getInt("discount_percent"));
                    p.put("stock", rs.getInt("stock"));
                    p.put("image_url", rs.getString("image_url"));
                    p.put("status", rs.getString("status"));
                    p.put("moderation_action", rs.getString("moderation_action"));
                    return p;
                }, vendorId);
    }

    public Map<String, Object> getProduct(long vendorId, long id) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT id, name, category_slug, slug, price, base_price, discount_percent, stock, image_url, "
                        + "images_json, specs_json, description, status FROM products WHERE vendor_id = ? AND id = ?",
                (rs, n) -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("id", rs.getLong("id"));
                    p.put("name", rs.getString("name"));
                    p.put("category_slug", rs.getString("category_slug"));
                    p.put("slug", rs.getString("slug"));
                    p.put("price", rs.getDouble("price"));
                    double base = rs.getDouble("base_price");
                    if (!rs.wasNull()) p.put("base_price", base);
                    p.put("discount_percent", rs.getInt("discount_percent"));
                    p.put("stock", rs.getInt("stock"));
                    p.put("image_url", rs.getString("image_url"));
                    p.put("images", Json.readStringList(rs.getString("images_json")));
                    p.put("specs", Json.readStringMap(rs.getString("specs_json")));
                    p.put("description", rs.getString("description"));
                    p.put("status", rs.getString("status"));
                    return p;
                }, vendorId, id);
        if (list.isEmpty()) throw ApiException.notFound("Mehsul tapilmadi");
        return list.get(0);
    }

    public long createProduct(long vendorId, Map<String, Object> req) {
        String name = str(req.get("name"));
        if (name.isBlank()) throw ApiException.badRequest("Mehsul adi teleb olunur");
        String categorySlug = str(req.get("category_slug"));
        if (categorySlug.isBlank()) throw ApiException.badRequest("Kateqoriya secilmelidir");
        String description = str(req.get("description"));
        if (description.isBlank()) throw ApiException.badRequest("Tesvir teleb olunur");

        int stock = (int) dbl(req.get("stock"));
        if (stock < 1) throw ApiException.badRequest("Stok sayi minimum 1 olmalidir");

        List<String> images = readImages(req);
        if (images.size() < 3) throw ApiException.badRequest("Minimum 3 sekil teleb olunur");
        if (images.size() > 12) throw ApiException.badRequest("Maximum 12 sekil ola bilər");

        int coverIndex = (int) dbl(req.get("cover_index"));
        if (coverIndex < 0 || coverIndex >= images.size()) coverIndex = 0;
        List<String> ordered = new ArrayList<>(images);
        if (coverIndex > 0) {
            String cover = ordered.remove(coverIndex);
            ordered.add(0, cover);
        }
        String imageUrl = ordered.get(0);

        double basePrice = dbl(req.get("base_price"));
        if (basePrice <= 0 && req.get("price") != null) {
            basePrice = dbl(req.get("price"));
        }
        if (basePrice <= 0) throw ApiException.badRequest("Qiymet teleb olunur");

        int discount = clampDiscount(req.get("discount_percent"));
        double price = discount > 0
                ? Math.round(basePrice * (100 - discount)) / 100.0
                : basePrice;

        String slug = slugify(name) + "-" + System.currentTimeMillis();

        jdbc.update("INSERT INTO products (vendor_id, category_slug, name, slug, price, base_price, discount_percent, "
                        + "stock, image_url, images_json, specs_json, description, status, moderation_action) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', ?, 'pending', 'create')",
                vendorId, categorySlug, name, slug, price, basePrice, discount, stock, imageUrl,
                Json.write(ordered), description);

        return jdbc.queryForObject("SELECT id FROM products WHERE slug = ?", Long.class, slug);
    }

    public void updateProduct(long vendorId, long id, Map<String, Object> req) {
        Integer owns = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE id = ? AND vendor_id = ?", Integer.class, id, vendorId);
        if (owns == null || owns == 0) throw ApiException.badRequest("Mehsul tapilmadi");

        List<String> images = readImages(req);
        String imageUrl = str(req.get("image_url"));
        String imagesJson = null;
        if (!images.isEmpty()) {
            if (images.size() < 3) throw ApiException.badRequest("Minimum 3 sekil teleb olunur");
            if (images.size() > 12) throw ApiException.badRequest("Maximum 12 sekil ola bilər");
            int coverIndex = (int) dbl(req.get("cover_index"));
            if (coverIndex < 0 || coverIndex >= images.size()) coverIndex = 0;
            List<String> ordered = new ArrayList<>(images);
            if (coverIndex > 0) {
                String cover = ordered.remove(coverIndex);
                ordered.add(0, cover);
            }
            imageUrl = ordered.get(0);
            imagesJson = Json.write(ordered);
        }

        double basePrice = dbl(req.get("base_price"));
        if (basePrice <= 0 && req.get("price") != null) basePrice = dbl(req.get("price"));
        int discount = clampDiscount(req.get("discount_percent"));
        double price = basePrice > 0
                ? (discount > 0 ? Math.round(basePrice * (100 - discount)) / 100.0 : basePrice)
                : dbl(req.get("price"));

        String categorySlug = str(req.get("category_slug"));

        if (imagesJson != null) {
            jdbc.update("UPDATE products SET name = ?, category_slug = COALESCE(NULLIF(?, ''), category_slug), "
                            + "price = ?, base_price = ?, discount_percent = ?, stock = ?, description = ?, "
                            + "image_url = ?, images_json = ?, moderation_action = 'update', status = 'pending' "
                            + "WHERE id = ? AND vendor_id = ?",
                    str(req.get("name")), categorySlug, price, basePrice > 0 ? basePrice : null, discount,
                    (int) dbl(req.get("stock")), str(req.get("description")), imageUrl, imagesJson, id, vendorId);
        } else {
            jdbc.update("UPDATE products SET name = ?, category_slug = COALESCE(NULLIF(?, ''), category_slug), "
                            + "price = ?, base_price = ?, discount_percent = ?, stock = ?, description = ?, "
                            + "image_url = COALESCE(NULLIF(?, ''), image_url), moderation_action = 'update', status = 'pending' "
                            + "WHERE id = ? AND vendor_id = ?",
                    str(req.get("name")), categorySlug, price, basePrice > 0 ? basePrice : null, discount,
                    (int) dbl(req.get("stock")), str(req.get("description")), imageUrl, id, vendorId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readImages(Map<String, Object> req) {
        Object raw = req.get("images");
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                String s = str(item).trim();
                if (!s.isBlank()) out.add(s);
            }
            return out;
        }
        String single = str(req.get("image_url")).trim();
        if (!single.isBlank()) return List.of(single);
        return List.of();
    }

    private static int clampDiscount(Object o) {
        int d = (int) dbl(o);
        if (d < 0) return 0;
        if (d > 90) return 90;
        return d;
    }

    public void requestDelete(long vendorId, long id, String reason) {
        Integer owns = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE id = ? AND vendor_id = ?", Integer.class, id, vendorId);
        if (owns == null || owns == 0) throw ApiException.badRequest("Mehsul tapilmadi");
        jdbc.update("UPDATE products SET moderation_action = 'delete', deletion_reason = ?, status = 'pending' WHERE id = ? AND vendor_id = ?",
                reason == null ? "" : reason, id, vendorId);
    }

    public List<Map<String, Object>> listCategories() {
        return jdbc.query("SELECT slug, name FROM categories WHERE status = 'active' ORDER BY name",
                (rs, n) -> {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("slug", rs.getString("slug"));
                    c.put("name", rs.getString("name"));
                    return c;
                });
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private static String slugify(String s) {
        String base = s.toLowerCase()
                .replace("ə", "e").replace("ı", "i").replace("ö", "o").replace("ü", "u")
                .replace("ç", "c").replace("ş", "s").replace("ğ", "g")
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return base.isBlank() ? "mehsul" : base;
    }
}
