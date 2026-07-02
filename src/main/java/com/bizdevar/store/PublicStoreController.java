package com.bizdevar.store;

import com.bizdevar.seller.SellerAccountRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stores")
public class PublicStoreController {

    private final SellerAccountRepository accounts;

    public PublicStoreController(SellerAccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/{slug}")
    public Map<String, Object> store(@PathVariable String slug) {
        return accounts.publicProfile(slug);
    }
}
