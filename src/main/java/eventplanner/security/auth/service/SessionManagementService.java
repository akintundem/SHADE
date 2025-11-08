package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.res.UserSessionResponse;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import eventplanner.security.auth.repository.UserSessionRepository;
import eventplanner.security.util.AuthMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class SessionManagementService {

    private final UserSessionRepository sessionRepository;

    public SessionManagementService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public List<UserSessionResponse> getActiveSessions(UserAccount user) {
        return sessionRepository.findByUserAndRevokedFalse(user).stream()
            .map(AuthMapper::toSessionResponse)
            .collect(Collectors.toList());
    }

    public void terminateAllSessions(UserAccount user) {
        List<UserSession> userSessions = sessionRepository.findByUser(user);
        userSessions.forEach(session -> session.setRevoked(true));
        sessionRepository.saveAll(userSessions);
    }

    /**
     * Prunes expired sessions from the database.
     * This method deletes all sessions that have expired (expiresAt is before current time).
     * 
     * @return The number of expired sessions that were deleted
     */
    public long pruneExpiredSessions() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long deletedCount = sessionRepository.deleteByExpiresAtBefore(now);
        if (deletedCount > 0) {
            log.info("Pruned {} expired session(s)", deletedCount);
        }
        return deletedCount;
    }

    /**
     * Scheduled task to periodically prune expired sessions.
     * Runs at the interval configured in application.yml (auth.session.cleanup-interval-ms).
     * Defaults to 1 hour (3600000 milliseconds) if not configured.
     */
    @Scheduled(fixedRateString = "${auth.session.cleanup-interval-ms:3600000}")
    public void scheduledPruneExpiredSessions() {
        try {
            pruneExpiredSessions();
        } catch (Exception e) {
            log.error("Error during scheduled session cleanup", e);
        }
    }
}
