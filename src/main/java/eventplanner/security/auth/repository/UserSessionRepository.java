package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenAndRevokedFalse(String refreshToken);
    List<UserSession> findByUserAndRevokedFalse(UserAccount user);
    List<UserSession> findByUser(UserAccount user);
    long countByUserAndRevokedFalse(UserAccount user);
    List<UserSession> findByUserAndRevokedFalseOrderByLastSeenAtAsc(UserAccount user);
    Optional<UserSession> findByUserAndDeviceId(UserAccount user, String deviceId);
    boolean existsByUserAndDeviceId(UserAccount user, String deviceId);

    /**
     * "Touch" the session without loading/saving the full entity.
     * Avoids stale entity updates if the session is concurrently pruned/deleted.
     *
     * @return number of rows updated (0 means session no longer exists/active)
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserSession s
           set s.lastSeenAt = :now
         where s.id = :id
           and s.revoked = false
           and s.deletedAt is null
        """)
    int touchLastSeen(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /**
     * Soft-delete + revoke expired sessions (instead of hard delete).
     * This keeps behavior consistent with BaseEntity soft delete and prevents stale-row errors.
     *
     * @return number of rows updated
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserSession s
           set s.revoked = true,
               s.deletedAt = :now
         where s.expiresAt < :cutoff
           and s.deletedAt is null
        """)
    int softDeleteExpiredSessions(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);
}
