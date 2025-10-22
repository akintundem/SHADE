package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private final Key signingKey;
    private final long accessTokenValidityMillis;
    private final long refreshTokenValidityMillis;

    public TokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long accessTokenValidityMillis,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenValidityMillis
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMillis = accessTokenValidityMillis;
        this.refreshTokenValidityMillis = refreshTokenValidityMillis;
    }

    public String generateAccessToken(UserAccount user) {
        return generateAccessToken(user, null);
    }

    public String generateAccessToken(UserAccount user, String clientId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenValidityMillis);
        
        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("type", "access")
                .claim("roles", List.of("USER"));
        
        // Add client ID if provided
        if (clientId != null && !clientId.trim().isEmpty()) {
            builder.claim("clientId", clientId);
        }
        
        return builder.signWith(signingKey).compact();
    }

    public String generateRefreshToken() {
        return "rt_" + UUID.randomUUID();
    }

    public LocalDateTime calculateRefreshExpiry(boolean rememberMe) {
        long ttl = rememberMe ? refreshTokenValidityMillis * 2 : refreshTokenValidityMillis;
        return LocalDateTime.ofInstant(Instant.now().plusMillis(ttl), ZoneOffset.UTC);
    }
}
