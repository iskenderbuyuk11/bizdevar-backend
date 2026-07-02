package com.bizdevar.security;

import com.bizdevar.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public record TokenPayload(AuthPrincipal.Role role, long id, Long staffId) {}

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(AppProperties props) {
        String secretText = props.getJwt().getSecret();
        if (secretText == null || secretText.isBlank()) {
            throw new IllegalStateException(
                    "bizdevar.jwt.secret bosdur. application.yml ve ya --spring.config.location ile duzgun secret verin (minimum 32 simvol).");
        }
        byte[] secret = secretText.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "bizdevar.jwt.secret cox qisadir (minimum 32 simvol / 256 bit lazimdir).");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttlMillis = (long) props.getJwt().getTtlDays() * 24 * 60 * 60 * 1000;
    }

    public String sign(AuthPrincipal principal) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        var builder = Jwts.builder()
                .claim("role", principal.role.name())
                .subject(principal.email)
                .issuedAt(now)
                .expiration(exp);

        switch (principal.role) {
            case CUSTOMER -> builder.claim("cid", principal.id);
            case SELLER -> {
                builder.claim("sid", principal.id);
                if (principal.staffId != null) {
                    builder.claim("staff_id", principal.staffId);
                    builder.claim("staff_role", principal.staffRole);
                }
            }
            case ADMIN -> builder.claim("uid", principal.id);
        }
        return builder.signWith(key).compact();
    }

    /** Legacy admin/customer token (uid only). */
    public String signLegacy(long userId, String email, boolean admin) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .claim("uid", userId)
                .claim("role", admin ? "ADMIN" : "CUSTOMER")
                .subject(email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public TokenPayload parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String roleStr = claims.get("role", String.class);
            AuthPrincipal.Role role = AuthPrincipal.Role.CUSTOMER;
            if (roleStr != null) {
                try { role = AuthPrincipal.Role.valueOf(roleStr); } catch (Exception ignored) {}
            }
            long id = 0;
            Long staffId = null;
            if (role == AuthPrincipal.Role.SELLER) {
                Object sid = claims.get("sid");
                if (sid != null) id = Long.parseLong(String.valueOf(((Number) sid).longValue()));
                Object st = claims.get("staff_id");
                if (st != null) staffId = Long.parseLong(String.valueOf(((Number) st).longValue()));
            } else if (role == AuthPrincipal.Role.ADMIN) {
                Object uid = claims.get("uid");
                if (uid != null) id = Long.parseLong(String.valueOf(((Number) uid).longValue()));
            } else {
                Object cid = claims.get("cid");
                if (cid != null) id = Long.parseLong(String.valueOf(((Number) cid).longValue()));
                else {
                    Object uid = claims.get("uid");
                    if (uid != null) {
                        id = Long.parseLong(String.valueOf(((Number) uid).longValue()));
                        role = AuthPrincipal.Role.ADMIN;
                    }
                }
            }
            if (id == 0) return null;
            return new TokenPayload(role, id, staffId);
        } catch (Exception e) {
            return null;
        }
    }

    public long ttlSeconds() {
        return ttlMillis / 1000;
    }
}
