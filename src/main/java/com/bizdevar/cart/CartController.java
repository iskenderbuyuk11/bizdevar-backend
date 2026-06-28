package com.bizdevar.cart;

import com.bizdevar.common.ApiException;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartRepository repo;

    public CartController(CartRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Map<String, Object> get(HttpServletRequest request) {
        AppUser u = AuthSupport.require(request);
        return repo.get(u.id);
    }

    @PostMapping("/items")
    public Map<String, Object> add(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        AppUser u = AuthSupport.require(request);
        long productId = asLong(body.get("product_id"));
        if (productId == 0) throw ApiException.badRequest("Yanlis sorgu");
        int qty = (int) asLong(body.getOrDefault("qty", 1));
        return repo.add(u.id, productId, qty);
    }

    @PatchMapping("/items/{productId}")
    public Map<String, Object> update(HttpServletRequest request, @PathVariable long productId,
                                      @RequestBody Map<String, Object> body) {
        AppUser u = AuthSupport.require(request);
        int qty = (int) asLong(body.getOrDefault("qty", 0));
        return repo.updateQty(u.id, productId, qty);
    }

    private static long asLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
