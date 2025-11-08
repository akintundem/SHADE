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
import eventplanner.security.auth.validation.RegistrationValidator;
import eventplanner.common.domain.enums.UserType;
import eventplanner.common.util.EnvironmentUtil;
import eventplanner.security.util.AuthMapper;
import eventplanner.common.exception.UnauthorizedException;
import eventplanner.common.exception.BadRequestException;
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
import java.util.Optional;
import java.util.regex.Pattern;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;
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
    private final RegistrationValidator registrationValidator;
    private final EnvironmentUtil environmentUtil;

    @Value("${auth.session.max-concurrent:5}")
    private int maxConcurrentSessions;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${auth.lockout.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lockout.initial-duration-minutes:15}")
    private int initialLockoutDurationMinutes;

    @Value("${auth.lockout.max-duration-hours:24}")
    private int maxLockoutDurationHours;

    // Dummy password hash that will never match any real password
    // This is used to prevent account enumeration by always performing password verification
    // Format: $2a$10$[22-char salt][31-char hash] - valid BCrypt format that will never match
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final Pattern TRUSTED_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    public AuthService(UserAccountRepository userAccountRepository,
                       UserSessionRepository sessionRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       NotificationService notificationService,
                       RegistrationValidator registrationValidator,
                       EnvironmentUtil environmentUtil) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
        this.registrationValidator = registrationValidator;
        this.environmentUtil = environmentUtil;
    }

    public SecureAuthResponse register(RegisterRequest request, String clientIp) {
        // Validate registration request (password match, terms, email uniqueness)
        registrationValidator.validate(request);
        
        // Normalize email (soft-enforce lowercasing)
        String normalizedEmail = normalizeEmail(request.getEmail());
        Optional<UserAccount> existingUserOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        UserAccount user;
        boolean isReRegistration = existingUserOpt.isPresent();
        
        if (isReRegistration) {
            // User exists but is unverified - update account info and resend verification
            user = existingUserOpt.get();
            log.info("Updating unverified user account and resending verification: {}", normalizedEmail);
            user.setName(request.getName().trim());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPhoneNumber(safeTrim(request.getPhoneNumber()));
            user.setDateOfBirth(request.getDateOfBirth());
            user.setAcceptTerms(Boolean.TRUE.equals(request.getAcceptTerms()));
            user.setAcceptPrivacy(Boolean.TRUE.equals(request.getAcceptPrivacy()));
            user.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));
            userAccountRepository.save(user);
        } else {
            // Entirely New user - create account
            user = UserAccount.builder()
                .email(normalizedEmail)
                .name(request.getName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(safeTrim(request.getPhoneNumber()))
                .dateOfBirth(request.getDateOfBirth())
                .emailVerified(false)
                .acceptTerms(Boolean.TRUE.equals(request.getAcceptTerms()))
                .acceptPrivacy(Boolean.TRUE.equals(request.getAcceptPrivacy()))
                .marketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()))
                .userType(UserType.INDIVIDUAL)
                .build();
            userAccountRepository.save(user);
            userAccountRepository.flush();
        }

        // Generate email verification token (single-active: revoke existing tokens first)
        // This handles both new users and unverified users re-registering after token expiration
        revokeExistingVerificationTokens(user);
        
        String rawToken = generateSecureToken();
        String hashedToken = TokenHashUtil.sha256(rawToken);
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .user(user)
            .token(hashedToken)
            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
            .consumed(false)
            .build();
        emailVerificationTokenRepository.save(verificationToken);

        // Send confirmation email (skip in dev mode)
        if (environmentUtil.isProductionEnvironment()) {
            try {
                // Use query parameter instead of path parameter
                String confirmLink = baseUrl + "/api/v1/auth/verify-email?token=" + rawToken;
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("user_name", user.getName() != null && !user.getName().trim().isEmpty() 
                    ? user.getName() : "");
                templateVariables.put("confirm_link", confirmLink);
                templateVariables.put("baseUrl", baseUrl);
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(user.getEmail())
                        .subject("Welcome to SHDE - Confirm Your Email")
                        .templateId(EmailService.TEMPLATE_EMAIL_VERIFICATION)
                        .templateVariables(templateVariables)
                        .eventId(null) // No eventId for auth emails
                        .build();
                
                notificationService.send(notificationRequest);
                log.info("Welcome email sent using template to user: {}", user.getEmail());
            } catch (Exception ex) {
                log.error("Failed to send welcome email to user: {}", user.getEmail(), ex);
            }
        } else {
            log.debug("Skipping verification email in dev mode for user: {}", user.getEmail());
        }
            
        return issueAuthResponse(user, false, null, null, clientIp, "User registered successfully. Please check your email for verification.");
    }
    


    public SecureAuthResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Optional<UserAccount> userOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        
        // Always perform password verification to prevent account enumeration
        // Use dummy hash if user doesn't exist to maintain consistent timing
        String passwordHash = userOpt.map(UserAccount::getPasswordHash)
            .orElse(DUMMY_PASSWORD_HASH);
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), passwordHash);
        
        // If user doesn't exist or password doesn't match, treat as invalid credentials
        if (!userOpt.isPresent() || !passwordMatches) {
            // Only increment failed attempts if user exists
            if (userOpt.isPresent()) {
                handleFailedLoginAttempt(userOpt.get());
            }
            // Always return the same error message and status code to prevent account enumeration
            throw new BadRequestException("INVALID_CREDENTIALS", "Invalid credentials");
        }
        
        UserAccount user = userOpt.get();
        
        // Check if account is locked
        if (isAccountLocked(user)) {
            long minutesRemaining = java.time.Duration.between(
                LocalDateTime.now(ZoneOffset.UTC), 
                user.getLockedUntil()
            ).toMinutes();
            throw new BadRequestException("ACCOUNT_LOCKED", 
                String.format("Account is temporarily locked due to multiple failed login attempts. Please try again in %d minute(s).", 
                    minutesRemaining));
        }
        
        // Check email verification
        if (!user.isEmailVerified()) {
            throw new BadRequestException("EMAIL_NOT_VERIFIED", 
                "Email not verified. Please verify your email address before logging in. Check your inbox for the verification link or request a new one.");
        }
        
        // Successful login - reset failed attempts and unlock account
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userAccountRepository.save(user);
        
        return issueAuthResponse(user, request.isRememberMe(), request.getClientId(), request.getDeviceId(), clientIp,
            "Login successful");
    }
    
    /**
     * Checks if an account is currently locked.
     */
    private boolean isAccountLocked(UserAccount user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (user.getLockedUntil().isBefore(now) || user.getLockedUntil().isEqual(now)) {
            // Lockout period has expired, unlock the account
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userAccountRepository.save(user);
            return false;
        }
        
        return true;
    }
    
    /**
     * Handles a failed login attempt by incrementing the counter and applying lockout if necessary.
     * Implements incremental lockout: longer lockout durations for repeated failures.
     */
    private void handleFailedLoginAttempt(UserAccount user) {
        int currentAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        int newAttempts = currentAttempts + 1;
        user.setFailedLoginAttempts(newAttempts);
        
        // Apply incremental lockout based on number of failed attempts
        if (newAttempts >= maxFailedAttempts) {
            LocalDateTime lockoutUntil = calculateLockoutDuration(newAttempts);
            user.setLockedUntil(lockoutUntil);
            log.warn("Account locked due to {} failed login attempts: {}", newAttempts, user.getEmail());
        }
        
        userAccountRepository.save(user);
    }
    
    /**
     * Calculates lockout duration based on number of failed attempts.
     * Implements incremental lockout:
     * - 5-9 attempts: 15 minutes
     * - 10-14 attempts: 1 hour
     * - 15+ attempts: 24 hours
     */
    private LocalDateTime calculateLockoutDuration(int failedAttempts) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        
        if (failedAttempts >= 15) {
            return now.plusHours(maxLockoutDurationHours);
        } else if (failedAttempts >= 10) {
            return now.plusHours(1);
        } else {
            return now.plusMinutes(initialLockoutDurationMinutes);
        }
    }

    public SecureAuthResponse refreshToken(RefreshTokenRequest request) {
        UserSession session = sessionRepository.findByRefreshTokenAndRevokedFalse(request.getRefreshToken())
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            session.setRevoked(true);
            throw new UnauthorizedException("Refresh token expired");
        }

        session.setLastSeenAt(LocalDateTime.now(ZoneOffset.UTC));
        UserAccount user = session.getUser();
        String accessToken = tokenService.generateAccessToken(user, session.getClientId());

        return SecureAuthResponse.builder()
            .message("Token refreshed successfully")
            .user(AuthMapper.toSecureUserResponse(user))
            .accessToken(accessToken)
            .refreshToken(session.getRefreshToken())
            .tokenType("Bearer")
            .clientId(session.getClientId())
            .deviceId(session.getDeviceId())
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

        String safeClientId = resolveClientIdentifier(user, clientId);
        String safeDeviceId = resolveDeviceIdentifier(user, deviceId);

        String accessToken = tokenService.generateAccessToken(user, safeClientId);
        String refreshToken = tokenService.generateRefreshToken();

        UserSession session = UserSession.builder()
            .user(user)
            .refreshToken(refreshToken)
            .clientId(safeClientId)
            .deviceId(safeDeviceId)
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
            .clientId(safeClientId)
            .deviceId(safeDeviceId)
            .build();
    }

    /**
     * Resolves client identifier: only reuses if it was previously issued by the server for this user.
     * Otherwise generates a new secure server-side identifier.
     * This prevents attackers from flooding sessions with arbitrary client IDs.
     */
    private String resolveClientIdentifier(UserAccount user, String requestedClientId) {
        // Only trust identifiers that match our pattern AND were previously issued for this user
        if (isTrustedIdentifier(requestedClientId) && sessionRepository.existsByUserAndClientId(user, requestedClientId)) {
            return requestedClientId; // Reuse trusted identifier from previous session
        }
        // Generate new secure identifier (always on registration, or when client sends invalid/unknown ID)
        return generateTrustedIdentifier();
    }

    /**
     * Resolves device identifier: only reuses if it was previously issued by the server for this user.
     * Otherwise generates a new secure server-side identifier.
     * This prevents attackers from flooding sessions with arbitrary device IDs.
     */
    private String resolveDeviceIdentifier(UserAccount user, String requestedDeviceId) {
        // Only trust identifiers that match our pattern AND were previously issued for this user
        if (isTrustedIdentifier(requestedDeviceId) && sessionRepository.existsByUserAndDeviceId(user, requestedDeviceId)) {
            return requestedDeviceId; // Reuse trusted identifier from previous session
        }
        // Generate new secure identifier (always on registration, or when client sends invalid/unknown ID)
        return generateTrustedIdentifier();
    }

    private boolean isTrustedIdentifier(String candidate) {
        return candidate != null && TRUSTED_IDENTIFIER_PATTERN.matcher(candidate).matches();
    }

    /**
     * Generates a trusted identifier.
     * 
     * @return String the trusted identifier
     */
    private String generateTrustedIdentifier() {
        return generateSecureToken();
    }

    /**
     * Revokes all existing unconsumed verification tokens for a user.
     * Ensures only one active verification token exists at a time.
     * 
     * @param user The user to revoke the verification tokens for
     * @return void
     */
    private void revokeExistingVerificationTokens(UserAccount user) {
        List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository
            .findByUserAndConsumedFalse(user);
        existingTokens.forEach(token -> {
            token.setConsumed(true);
            emailVerificationTokenRepository.save(token);
        });
        if (!existingTokens.isEmpty()) {
            log.debug("Revoked {} existing verification token(s) for user: {}", existingTokens.size(), user.getEmail());
        }
    }
}
