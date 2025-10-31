package ai.eventplanner.auth.service;

import ai.eventplanner.auth.dto.req.ChangePasswordRequest;
import ai.eventplanner.auth.dto.req.ForgotPasswordRequest;
import ai.eventplanner.auth.dto.req.ResetPasswordRequest;
import ai.eventplanner.auth.entity.EmailVerificationToken;
import ai.eventplanner.auth.entity.PasswordResetToken;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.entity.UserSession;
import ai.eventplanner.auth.repo.EmailVerificationTokenRepository;
import ai.eventplanner.auth.repo.PasswordResetTokenRepository;
import ai.eventplanner.auth.repo.UserAccountRepository;
import ai.eventplanner.auth.repo.UserSessionRepository;
import ai.eventplanner.common.exception.UnauthorizedException;
import ai.eventplanner.common.security.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static ai.eventplanner.auth.util.AuthValidationUtil.normalizeEmail;
import static ai.eventplanner.auth.util.AuthValidationUtil.validatePasswordMatch;
import static ai.eventplanner.auth.util.SecureTokenUtil.generateSecureToken;

@Service
@Transactional
public class AccountRecoveryService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountRecoveryService(UserAccountRepository userAccountRepository,
                                  UserSessionRepository sessionRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  EmailVerificationTokenRepository emailVerificationTokenRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void changePassword(UserAccount user, ChangePasswordRequest request) {
        try {
            validatePasswordMatch(request.getNewPassword(), request.getConfirmPassword());
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new UnauthorizedException("Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userAccountRepository.save(user);

            List<UserSession> userSessions = sessionRepository.findByUser(user);
            userSessions.forEach(session -> session.setRevoked(true));
            sessionRepository.saveAll(userSessions);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Password validation failed: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage(), e);
        }
    }

    public String requestPasswordReset(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        return userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
            .map(user -> {
                String rawToken = generateSecureToken();
                String hashed = TokenHashUtil.sha256(rawToken);
                PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .token(hashed)
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(1))
                    .consumed(false)
                    .build();
                passwordResetTokenRepository.save(token);
                return "Password reset email sent";
            })
            .orElse("Password reset email sent");
    }

    public boolean resetPassword(ResetPasswordRequest request) {
        validatePasswordMatch(request.getNewPassword(), request.getConfirmPassword());

        String hashed = TokenHashUtil.sha256(request.getToken());
        PasswordResetToken token = passwordResetTokenRepository.findByToken(hashed)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (token.isConsumed() || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);

        token.setConsumed(true);
        passwordResetTokenRepository.save(token);

        List<UserSession> userSessions = sessionRepository.findByUser(user);
        userSessions.forEach(session -> session.setRevoked(true));
        sessionRepository.saveAll(userSessions);

        return true;
    }

    public String resendVerification(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        return userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
            .map(user -> {
                String rawToken = generateSecureToken();
                String hashed = TokenHashUtil.sha256(rawToken);
                EmailVerificationToken token = EmailVerificationToken.builder()
                    .user(user)
                    .token(hashed)
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
                    .consumed(false)
                    .build();
                emailVerificationTokenRepository.save(token);
                return "Verification email sent";
            })
            .orElse("Verification email sent");
    }

    public boolean verifyEmailToken(String tokenValue) {
        String hashed = TokenHashUtil.sha256(tokenValue);
        return emailVerificationTokenRepository.findByToken(hashed)
            .map(token -> {
                if (token.isConsumed() || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
                    return false;
                }
                UserAccount user = token.getUser();
                user.setEmailVerified(true);
                token.setConsumed(true);
                return true;
            })
            .orElse(false);
    }
}
