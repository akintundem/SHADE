package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.res.UserSessionResponse;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import eventplanner.security.auth.repository.UserSessionRepository;
import eventplanner.security.util.AuthMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
}
