package com.bizdevar.catalog;

import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}")
public class ProductEngagementController {

    private final ProductEngagementRepository repo;

    public ProductEngagementController(ProductEngagementRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/reviews")
    public Map<String, Object> reviews(HttpServletRequest request, @PathVariable long productId) {
        AppUser user = AuthSupport.optional(request);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reviews", repo.reviews(productId));
        out.put("can_review", user != null && repo.canReview(user.id, productId));
        return out;
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addReview(
            HttpServletRequest request,
            @PathVariable long productId,
            @RequestBody Map<String, Object> body) {
        AppUser user = AuthSupport.require(request);
        int stars = asInt(body.get("stars"), 5);
        String text = body.get("text") == null ? "" : String.valueOf(body.get("text"));
        return Map.of("review", repo.upsertReview(user.id, productId, stars, text));
    }

    @GetMapping("/questions")
    public Map<String, Object> questions(@PathVariable long productId) {
        return Map.of("questions", repo.questions(productId));
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addQuestion(
            HttpServletRequest request,
            @PathVariable long productId,
            @RequestBody Map<String, Object> body) {
        AppUser user = AuthSupport.require(request);
        String text = body.get("text") == null ? "" : String.valueOf(body.get("text"));
        return Map.of("question", repo.addQuestion(user.id, productId, text));
    }

    private static int asInt(Object o, int fallback) {
        if (o == null) return fallback;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return fallback;
        }
    }
}
