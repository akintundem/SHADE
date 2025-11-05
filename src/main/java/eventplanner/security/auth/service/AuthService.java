package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.req.LoginRequest;
import eventplanner.security.auth.dto.req.RefreshTokenRequest;
import eventplanner.security.auth.dto.req.RegisterRequest;
import eventplanner.security.auth.dto.res.SecureAuthResponse;
import eventplanner.security.auth.entity.EmailVerificationToken;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import eventplanner.security.auth.repository.EmailVerificationTokenRepository;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.repository.UserSessionRepository;
import eventplanner.security.util.AuthMapper;
import eventplanner.common.exception.UnauthorizedException;
import eventplanner.security.util.TokenHashUtil;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.channel.email.EmailTemplateService;
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
import static eventplanner.security.util.AuthValidationUtil.safeTrim;
import static eventplanner.security.util.AuthValidationUtil.validatePasswordMatch;
import static eventplanner.security.util.SecureTokenUtil.generateSecureToken;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository sessionRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    private final EmailTemplateService emailTemplateService;

    @Value("${auth.session.max-concurrent:5}")
    private int maxConcurrentSessions;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AuthService(UserAccountRepository userAccountRepository,
                       UserSessionRepository sessionRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       NotificationService notificationService,
                       EmailTemplateService emailTemplateService) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
        this.emailTemplateService = emailTemplateService;
    }

    public SecureAuthResponse register(RegisterRequest request, String clientIp) {
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());
        if (!Boolean.TRUE.equals(request.getAcceptTerms()) || !Boolean.TRUE.equals(request.getAcceptPrivacy())) {
            throw new IllegalArgumentException("Terms and privacy policy must be accepted");
        }
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Registration failed: email is already registered");
        }

        UserAccount user = UserAccount.builder()
            .email(normalizedEmail)
            .name(request.getName().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phoneNumber(safeTrim(request.getPhoneNumber()))
            .dateOfBirth(request.getDateOfBirth())
            .emailVerified(false)
            .acceptTerms(Boolean.TRUE.equals(request.getAcceptTerms()))
            .acceptPrivacy(Boolean.TRUE.equals(request.getAcceptPrivacy()))
            .marketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()))
            .userType(eventplanner.common.domain.enums.UserType.INDIVIDUAL)
            .build();
        userAccountRepository.save(user);
        userAccountRepository.flush();

        // Generate email verification token
        String rawToken = generateSecureToken();
        String hashedToken = TokenHashUtil.sha256(rawToken);
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .user(user)
            .token(hashedToken)
            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
            .consumed(false)
            .build();
        emailVerificationTokenRepository.save(verificationToken);

        // Send confirmation email using Resend template
        try {
            String confirmLink = baseUrl + "/api/v1/auth/verify-email/" + rawToken;
            Map<String, Object> templateVariables = emailTemplateService.prepareWelcomeEmailVariables(
                user.getName(), 
                confirmLink, 
                baseUrl
            );
            notificationService.sendEmailWithTemplate(
                user.getEmail(),
                "Welcome to SHDE - Confirm Your Email",
                EmailTemplateService.TEMPLATE_EMAIL_VERIFICATION,
                templateVariables,
                null // No eventId for auth emails
            );
            log.info("Welcome email sent using template to user: {}", user.getEmail());
        } catch (Exception ex) {
            // Log error but don't fail registration
            // Email sending failures shouldn't block user registration
            log.error("Failed to send welcome email to user: {}", user.getEmail(), ex);
        }

        // Send welcome push notification
        try {
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("type", "welcome");
            notificationData.put("action", "view_profile");

            notificationService.sendPushNotification(
                user.getId(),
                "Welcome to SHDE!",
                "Welcome " + user.getName() + "! We're excited to have you on board. Start planning your first event!",
                notificationData,
                baseUrl + "/profile",
                null // No eventId for auth notifications
            );
            log.info("Welcome push notification sent to user: {}", user.getId());
        } catch (Exception ex) {
            // Log error but don't fail registration
            // Push notification failures shouldn't block user registration
            log.error("Failed to send welcome push notification to user: {}", user.getId(), ex);
        }

        return issueAuthResponse(user, false, request.getClientId(), request.getDeviceId(), clientIp,
            "User registered successfully");
    }

    public SecureAuthResponse login(LoginRequest request, String clientIp) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));

        return issueAuthResponse(user, request.isRememberMe(), request.getClientId(), request.getDeviceId(), clientIp,
            "Login successful");
    }

    public SecureAuthResponse refreshToken(RefreshTokenRequest request) {
        UserSession session = sessionRepository.findByRefreshTokenAndRevokedFalse(request.getRefreshToken())
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            session.setRevoked(true);
            throw new UnauthorizedException("Refresh token expired");
        }

        session.setLastSeenAt(LocalDateTime.now(ZoneOffset.UTC));
        session.setDeviceId(request.getDeviceId());
        session.setClientId(request.getClientId());

        UserAccount user = session.getUser();
        String accessToken = tokenService.generateAccessToken(user, session.getClientId());

        return SecureAuthResponse.builder()
            .message("Token refreshed successfully")
            .user(AuthMapper.toSecureUserResponse(user))
            .accessToken(accessToken)
            .refreshToken(session.getRefreshToken())
            .tokenType("Bearer")
            .build();
    }

    public void logout(UserAccount user) {
        List<UserSession> userSessions = sessionRepository.findByUser(user);
        userSessions.forEach(session -> session.setRevoked(true));
        sessionRepository.saveAll(userSessions);
    }

    private SecureAuthResponse issueAuthResponse(UserAccount user,
                                                 boolean rememberMe,
                                                 String clientId,
                                                 String deviceId,
                                                 String ipAddress,
                                                 String message) {
        long activeSessionCount = sessionRepository.countByUserAndRevokedFalse(user);
        if (activeSessionCount >= maxConcurrentSessions) {
            int sessionsToRevoke = (int) (activeSessionCount - maxConcurrentSessions + 1);
            sessionRepository.findByUserAndRevokedFalseOrderByLastSeenAtAsc(user)
                .stream()
                .limit(sessionsToRevoke)
                .forEach(UserSession::revoke);
        }

        String accessToken = tokenService.generateAccessToken(user, clientId);
        String refreshToken = tokenService.generateRefreshToken();

        UserSession session = UserSession.builder()
            .user(user)
            .refreshToken(refreshToken)
            .clientId(clientId)
            .deviceId(deviceId)
            .ipAddress(ipAddress)
            .expiresAt(tokenService.calculateRefreshExpiry(rememberMe))
            .revoked(false)
            .build();
        sessionRepository.save(session);

        return SecureAuthResponse.builder()
            .message(message)
            .user(AuthMapper.toSecureUserResponse(user))
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .build();
    }
}
