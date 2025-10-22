package ai.eventplanner.auth.repo;

import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.entity.UserSession;
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
}
