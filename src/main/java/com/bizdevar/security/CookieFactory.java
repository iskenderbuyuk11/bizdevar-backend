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

    public ResponseCookie create(String token) {
        return ResponseCookie.from(props.getCookie().getName(), token)
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .path("/")
                .maxAge(props.getJwt().getTtlDays() * 24L * 60 * 60)
                .sameSite(props.getCookie().getSameSite())
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(props.getCookie().getName(), "")
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .path("/")
                .maxAge(0)
                .sameSite(props.getCookie().getSameSite())
                .build();
    }
}
