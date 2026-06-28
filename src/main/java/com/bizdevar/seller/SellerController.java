package com.bizdevar.seller;

import com.bizdevar.auth.dto.SellerRegisterRequest;
import com.bizdevar.security.AuthSupport;
import com.bizdevar.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerRepository repo;

    public SellerController(SellerRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/store")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createStore(HttpServletRequest request, @RequestBody SellerRegisterRequest req) {
        AppUser u = AuthSupport.require(request);
        if (req.phone == null || req.phone.isBlank()) req.phone = u.phone;
        long vendorId = repo.createVendor(u.id, req);
        return Map.of("vendor", repo.vendorByUserId(u.id).toMap());
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest request) {
        VendorInfo v = vendor(request);
        return repo.dashboard(v.id);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpServletRequest request) {
        return Map.of("vendor", vendor(request).toMap());
    }

    @GetMapping("/categories")
    public Map<String, Object> categories(HttpServletRequest request) {
        AuthSupport.require(request);
        return Map.of("categories", repo.listCategories());
    }

    @GetMapping("/products")
    public Map<String, Object> products(HttpServletRequest request) {
        VendorInfo v = vendor(request);
        return Map.of("products", repo.listProducts(v.id));
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> productGet(HttpServletRequest request, @PathVariable long id) {
        VendorInfo v = vendor(request);
        return Map.of("product", repo.getProduct(v.id, id));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> productCreate(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        VendorInfo v = vendor(request);
        long id = repo.createProduct(v.id, req);
        return Map.of("product_id", id, "message", "Mehsul admin tesdiqine gonderildi");
    }

    @PatchMapping("/products/{id}")
    public Map<String, Object> productUpdate(HttpServletRequest request, @PathVariable long id,
                                             @RequestBody Map<String, Object> req) {
        VendorInfo v = vendor(request);
        repo.updateProduct(v.id, id, req);
        return Map.of("status", "ok", "message", "Deyisiklik admin tesdiqine gonderildi");
    }

    @PostMapping("/products/{id}/delete-request")
    public Map<String, Object> productDeleteRequest(HttpServletRequest request, @PathVariable long id,
                                                     @RequestBody(required = false) Map<String, Object> req) {
        VendorInfo v = vendor(request);
        String reason = req != null && req.get("reason") != null ? String.valueOf(req.get("reason")) : "";
        repo.requestDelete(v.id, id, reason);
        return Map.of("status", "ok", "message", "Silinme sorgusu admin tesdiqine gonderildi");
    }

    private VendorInfo vendor(HttpServletRequest request) {
        AppUser u = AuthSupport.require(request);
        return repo.vendorByUserId(u.id);
    }
}
