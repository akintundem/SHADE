package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenAndRevokedFalse(String refreshToken);
    List<UserSession> findByUserAndRevokedFalse(UserAccount user);
    List<UserSession> findByUser(UserAccount user);
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
    long countByUserAndRevokedFalse(UserAccount user);
    List<UserSession> findByUserAndRevokedFalseOrderByLastSeenAtAsc(UserAccount user);
    boolean existsByUserAndClientId(UserAccount user, String clientId);
    boolean existsByUserAndDeviceId(UserAccount user, String deviceId);
}
