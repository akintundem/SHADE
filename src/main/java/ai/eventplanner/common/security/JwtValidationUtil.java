package ai.eventplanner.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtValidationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtValidationUtil.class);
    
    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyChangeInProduction}")
    private String jwtSecret;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    public String getEmailFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("roles", List.class);
        } catch (Exception e) {
            logger.error("Error extracting roles from token: {}", e.getMessage());
            return List.of();
        }
    }
    
    public String getOrganizationIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("organizationId", String.class);
        } catch (Exception e) {
            logger.error("Error extracting organization ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new java.util.Date());
    }
    
    public boolean hasRole(String token, String role) {
        List<String> roles = getRolesFromToken(token);
        return roles.contains(role);
    }
    
    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }
}
