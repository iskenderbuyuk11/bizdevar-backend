package com.bizdevar.security;

import com.bizdevar.config.AppProperties;
import com.bizdevar.user.AppUser;
import com.bizdevar.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Hər sorğuda JWT cookie-ni oxuyur, etibarlidirsa istifadeçini request atributuna qoyur.
 * Go-daki WithUser middleware-in qarsiligi.
 */
@Component
@Order(1)
public class CurrentUserFilter extends OncePerRequestFilter {

    public static final String ATTR = "bizdevar.currentUser";

    private final JwtService jwt;
    private final UserRepository users;
    private final String cookieName;

    public CurrentUserFilter(JwtService jwt, UserRepository users, AppProperties props) {
        this.jwt = jwt;
        this.users = users;
        this.cookieName = props.getCookie().getName();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = readCookie(request);
        if (token != null && !token.isBlank()) {
            Long uid = jwt.parseUserId(token);
            if (uid != null) {
                AppUser user = users.findById(uid).orElse(null);
                if (user != null) {
                    request.setAttribute(ATTR, user);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (cookieName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
