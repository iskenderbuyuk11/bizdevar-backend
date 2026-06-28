package com.bizdevar.security;

import com.bizdevar.config.AppProperties;
import com.bizdevar.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(AppProperties props) {
        byte[] secret = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttlMillis = (long) props.getJwt().getTtlDays() * 24 * 60 * 60 * 1000;
    }

    public String sign(AppUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .claim("uid", user.id)
                .claim("email", user.email)
                .claim("adm", user.admin)
                .subject(user.email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public long ttlSeconds() {
        return ttlMillis / 1000;
    }

    /** Token-den user id qaytarir, etibarsizdirsa null. */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object uid = claims.get("uid");
            if (uid == null) return null;
            return Long.parseLong(String.valueOf(((Number) uid).longValue()));
        } catch (Exception e) {
            return null;
        }
    }
}
