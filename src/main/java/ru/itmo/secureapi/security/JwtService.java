package ru.itmo.secureapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {

    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl:PT15M}") Duration ttl) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("APP_JWT_TTL must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public String issue(String username) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(ttl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String verifyAndGetSubject(String token) {
        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        if (!"HS256".equals(parsedToken.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("Unexpected JWT algorithm");
        }
        Claims claims = parsedToken.getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return claims.getSubject();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
