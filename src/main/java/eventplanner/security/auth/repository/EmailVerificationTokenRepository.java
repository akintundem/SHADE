package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.EmailVerificationToken;
import eventplanner.security.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);
    List<EmailVerificationToken> findByUserAndConsumedFalse(UserAccount user);
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
