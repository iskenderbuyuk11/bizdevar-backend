package com.bizdevar.catalog;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogRepository repo;

    public CatalogController(CatalogRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/categories")
    public Map<String, Object> categories() {
        return Map.of("categories", repo.listCategories());
    }

    @GetMapping("/products")
    public Map<String, Object> products(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String slug) {
        List<Map<String, Object>> list = repo.listProducts(cat, q, slug, true);
        return Map.of("products", list);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> product(@PathVariable long id) {
        return Map.of("product", repo.getProduct(id));
    }
}
