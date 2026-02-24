package eventplanner.security.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * Server-side JWT revocation using a Redis blocklist.
 *
 * <p>On logout the token's JTI (or SHA-256 of the raw token value when no JTI is
 * present) is stored in Redis with a TTL equal to the token's remaining lifetime.
 * Subsequent requests carrying that token are rejected by {@link TokenRevocationFilter}
 * before reaching any business logic.
 *
 * <p>This makes logout effective immediately rather than waiting for token expiry,
 * closing the window where a stolen/leaked token remains usable after sign-out.
 */
@Service
public class TokenRevocationService {

    private static final String BLOCKLIST_PREFIX = "token:revoked:";

    private final StringRedisTemplate redisTemplate;

    public TokenRevocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Revoke a token by adding its JTI to the Redis blocklist.
     * The entry expires automatically when the token's own expiry is reached.
     *
     * @param jti       The JWT ID claim (jti). Must not be blank.
     * @param expiresAt The token's expiry instant (from the {@code exp} claim).
     *                  If null or already expired, the entry is stored with a minimum 60-second TTL.
     */
    public void revoke(String jti, Instant expiresAt) {
        if (!StringUtils.hasText(jti)) {
            return;
        }
        Duration ttl = resolveTtl(expiresAt);
        redisTemplate.opsForValue().set(BLOCKLIST_PREFIX + jti, "1", ttl);
    }

    /**
     * Check whether a token JTI has been revoked.
     *
     * @param jti The JWT ID claim.
     * @return {@code true} if the token is on the blocklist.
     */
    public boolean isRevoked(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKLIST_PREFIX + jti));
    }

    private Duration resolveTtl(Instant expiresAt) {
        if (expiresAt == null) {
            return Duration.ofMinutes(60);
        }
        long secondsRemaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return secondsRemaining > 0 ? Duration.ofSeconds(secondsRemaining) : Duration.ofSeconds(60);
    }
}
