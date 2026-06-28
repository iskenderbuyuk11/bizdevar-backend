package com.bizdevar.order;

import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderRepository repo;

    public OrderController(OrderRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/orders")
    public Map<String, Object> list(HttpServletRequest request) {
        AppUser u = AuthSupport.require(request);
        return Map.of("orders", repo.listByUser(u.id));
    }

    @GetMapping("/orders/{id}")
    public Map<String, Object> get(HttpServletRequest request, @PathVariable String id) {
        AppUser u = AuthSupport.require(request);
        return Map.of("order", repo.getByNumber(u.id, id));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        AppUser u = AuthSupport.require(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> delivery = body.get("delivery") instanceof Map
                ? (Map<String, Object>) body.get("delivery") : null;
        int promoPct = asInt(body.get("promo_discount_percent"));
        String promoCode = body.get("promo_code") == null ? "" : String.valueOf(body.get("promo_code"));
        if (!promoCode.isBlank() && promoPct == 0) {
            try { promoPct = repo.validatePromo(promoCode); } catch (Exception ignored) {}
        }
        return Map.of("order", repo.create(u.id, delivery, promoPct));
    }

    @PostMapping("/promo/validate")
    public Map<String, Object> validatePromo(@RequestBody Map<String, Object> body) {
        String code = body.get("code") == null ? "" : String.valueOf(body.get("code"));
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            int pct = repo.validatePromo(code);
            out.put("valid", true);
            out.put("discount_percent", pct);
            out.put("code", code);
        } catch (Exception e) {
            out.put("valid", false);
            out.put("discount_percent", 0);
        }
        return out;
    }

    private static int asInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
