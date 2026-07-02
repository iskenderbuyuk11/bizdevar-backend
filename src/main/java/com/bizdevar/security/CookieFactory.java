package com.bizdevar.security;

import com.bizdevar.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieFactory {

    private final AppProperties props;

    public CookieFactory(AppProperties props) {
        this.props = props;
    }

    public String customerCookieName() {
        return props.getCookie().getName();
    }

    public String sellerCookieName() {
        return props.getCookie().getSellerName();
    }

    public String adminCookieName() {
        return props.getCookie().getAdminName();
    }

    public ResponseCookie createCustomer(String token) {
        return build(customerCookieName(), token, props.getJwt().getTtlDays() * 24L * 60 * 60);
    }

    public ResponseCookie createSeller(String token) {
        return build(sellerCookieName(), token, props.getJwt().getTtlDays() * 24L * 60 * 60);
    }

    public ResponseCookie createAdmin(String token) {
        return build(adminCookieName(), token, props.getJwt().getTtlDays() * 24L * 60 * 60);
    }

    public ResponseCookie clearCustomer() {
        return build(customerCookieName(), "", 0);
    }

    public ResponseCookie clearSeller() {
        return build(sellerCookieName(), "", 0);
    }

    public ResponseCookie clearAdmin() {
        return build(adminCookieName(), "", 0);
    }

    /** @deprecated use createCustomer */
    public ResponseCookie create(String token) {
        return createCustomer(token);
    }

    /** @deprecated use clearCustomer */
    public ResponseCookie clear() {
        return clearCustomer();
    }

    private ResponseCookie build(String name, String value, long maxAge) {
        var builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite(props.getCookie().getSameSite());
        String domain = props.getCookie().getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain.trim());
        }
        return builder.build();
    }
}
