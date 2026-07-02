package com.bizdevar.catalog;

import com.bizdevar.common.Json;
import org.springframework.jdbc.core.RowMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mehsul setirini frontend-in gozledigi JSON formatina cevirir.
 * Sorgu bu sutunlari secmelidir: id, name, category_slug, slug, price, base_price,
 * discount_percent, popular, stock, image_url, description, specs_json, images_json, status, vendor_name, sold_count, rating_stars, rating_count
 */
public class ProductMapper implements RowMapper<Map<String, Object>> {

    public static final ProductMapper INSTANCE = new ProductMapper();

    public static final String SELECT_COLUMNS =
            "p.id, p.name, p.category_slug, p.slug, p.price, p.base_price, p.discount_percent, "
                    + "p.popular, p.stock, p.image_url, p.description, p.specs_json, p.images_json, p.status, "
                    + "COALESCE(v.name, 'BizdeVar Resmi') AS vendor_name, v.logo_url AS vendor_logo_url, "
                    + "(SELECT COALESCE(SUM(oi.qty), 0) FROM order_items oi WHERE oi.product_id = p.id) AS sold_count, "
                    + "(SELECT COALESCE(ROUND(AVG(pr.stars), 1), 0) FROM product_reviews pr WHERE pr.product_id = p.id) AS rating_stars, "
                    + "(SELECT COUNT(*) FROM product_reviews pr WHERE pr.product_id = p.id) AS rating_count";

    @Override
    public Map<String, Object> mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Map<String, Object> p = new LinkedHashMap<>();
        long id = rs.getLong("id");
        String cat = rs.getString("category_slug");
        double price = rs.getDouble("price");
        String imageUrl = rs.getString("image_url");
        int discount = rs.getInt("discount_percent");

        p.put("id", id);
        p.put("name", rs.getString("name"));
        p.put("cat", cat);
        p.put("category", cat);
        p.put("price", price);

        Object basePrice = rs.getObject("base_price");
        if (basePrice != null) {
            double base = ((Number) basePrice).doubleValue();
            p.put("base_price", base);
            p.put("oldPrice", base);
        }
        p.put("discount_percent", discount);
        p.put("popular", rs.getInt("popular"));
        if (imageUrl != null) {
            p.put("image_url", imageUrl);
            p.put("image", imageUrl);
        }
        p.put("slug", rs.getString("slug"));
        p.put("description", rs.getString("description"));
        p.put("specs", Json.readStringMap(rs.getString("specs_json")));

        List<String> images = Json.readStringList(rs.getString("images_json"));
        if (images.isEmpty() && imageUrl != null && !imageUrl.isBlank()) {
            images = List.of(imageUrl);
        }
        p.put("images", images);
        p.put("stock", rs.getInt("stock"));
        p.put("status", rs.getString("status"));
        p.put("vendor_name", rs.getString("vendor_name"));
        try {
            String logo = rs.getString("vendor_logo_url");
            if (logo != null && !logo.isBlank()) p.put("vendor_logo_url", logo);
        } catch (java.sql.SQLException ignored) {
        }
        p.put("sold_count", rs.getInt("sold_count"));
        p.put("rating_stars", rs.getDouble("rating_stars"));
        p.put("rating_count", rs.getInt("rating_count"));
        p.put("sale", discount > 0);
        return p;
    }
}
