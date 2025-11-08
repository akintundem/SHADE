package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.req.ChangePasswordRequest;
import eventplanner.security.auth.dto.req.ForgotPasswordRequest;
import eventplanner.security.auth.dto.req.ResendEmailVerificationRequest;
import eventplanner.security.auth.dto.req.ResetPasswordRequest;
import eventplanner.security.auth.entity.EmailVerificationToken;
import eventplanner.security.auth.entity.PasswordResetToken;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import eventplanner.security.auth.repository.EmailVerificationTokenRepository;
import eventplanner.security.auth.repository.PasswordResetTokenRepository;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.repository.UserSessionRepository;
import eventplanner.common.exception.UnauthorizedException;
import eventplanner.common.util.EnvironmentUtil;
import eventplanner.security.util.TokenHashUtil;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.channel.email.EmailService;
import eventplanner.common.domain.enums.CommunicationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.validatePasswordMatch;
import static eventplanner.security.util.SecureTokenUtil.generateSecureToken;

@Service
@Transactional
@Slf4j
public class AccountRecoveryService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EnvironmentUtil environmentUtil;

    @Value("${app.base-url}")
    private String baseUrl;

    public AccountRecoveryService(UserAccountRepository userAccountRepository,
                                  UserSessionRepository sessionRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  EmailVerificationTokenRepository emailVerificationTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  NotificationService notificationService,
                                  EnvironmentUtil environmentUtil) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.environmentUtil = environmentUtil;
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

    public String resendVerification(ResendEmailVerificationRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Email not found. Please check your email address."));
        
        // Don't allow resending if email is already verified
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified. You can log in directly.");
        }
        
        // Invalidate existing unused tokens for this user
        List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository.findByUserAndConsumedFalse(user);
        existingTokens.forEach(token -> {
            token.setConsumed(true);
            emailVerificationTokenRepository.save(token);
        });
        
        // Generate new verification token
        String rawToken = generateSecureToken();
        String hashed = TokenHashUtil.sha256(rawToken);
        EmailVerificationToken token = EmailVerificationToken.builder()
            .user(user)
            .token(hashed)
            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
            .consumed(false)
            .build();
        emailVerificationTokenRepository.save(token);
        
        // Send verification email (skip in dev mode)
        if (!environmentUtil.isDevelopmentEnvironment()) {
            try {
                // Use query parameter instead of path parameter
                String confirmLink = baseUrl + "/api/v1/auth/verify-email?token=" + rawToken;
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("user_name", user.getName() != null && !user.getName().trim().isEmpty() 
                    ? user.getName() : "there");
                templateVariables.put("confirm_link", confirmLink);
                templateVariables.put("baseUrl", baseUrl);
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(user.getEmail())
                        .subject("Verify Your Email - SHDE")
                        .templateId(EmailService.TEMPLATE_EMAIL_VERIFICATION)
                        .templateVariables(templateVariables)
                        .eventId(null)
                        .build();
                
                notificationService.send(notificationRequest);
                log.info("Verification email resent to user: {}", user.getEmail());
            } catch (Exception ex) {
                log.error("Failed to send verification email to user: {}", user.getEmail(), ex);
                throw new RuntimeException("Failed to send verification email. Please try again later.");
            }
        } else {
            log.debug("Skipping verification email in dev mode for user: {}", user.getEmail());
        }
        
        return "Verification email sent. Please check your inbox and verify your email before logging in.";
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
                userAccountRepository.save(user);
                token.setConsumed(true);
                emailVerificationTokenRepository.save(token);
                return true;
            })
            .orElse(false);
    }

}
