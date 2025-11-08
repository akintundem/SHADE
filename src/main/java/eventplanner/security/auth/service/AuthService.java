package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.req.LoginRequest;
import eventplanner.security.auth.dto.req.LogoutRequest;
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
import eventplanner.security.util.JwtValidationUtil;
import eventplanner.security.auth.dto.res.TokenValidationResponse;
import io.jsonwebtoken.Claims;
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
import java.util.UUID;

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
    private final RateLimitingService rateLimitingService;
    private final SecurityAuditService securityAuditService;
    private final JwtValidationUtil jwtValidationUtil;

    @Value("${auth.session.max-concurrent}")
    private int maxConcurrentSessions;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${auth.lockout.max-attempts}")
    private int maxFailedAttempts;

    @Value("${auth.lockout.initial-duration-minutes}")
    private int initialLockoutDurationMinutes;

    @Value("${auth.lockout.max-duration-hours}")
    private int maxLockoutDurationHours;

    // Dummy password hash that will never match any real password
    // This is used to prevent account enumeration by always performing password verification
    // Format: $2a$10$[22-char salt][31-char hash] - valid BCrypt format that will never match
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    public AuthService(UserAccountRepository userAccountRepository,
                       UserSessionRepository sessionRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       NotificationService notificationService,
                       RegistrationValidator registrationValidator,
                       EnvironmentUtil environmentUtil,
                       RateLimitingService rateLimitingService,
                       SecurityAuditService securityAuditService,
                       JwtValidationUtil jwtValidationUtil) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
        this.registrationValidator = registrationValidator;
        this.environmentUtil = environmentUtil;
        this.rateLimitingService = rateLimitingService;
        this.securityAuditService = securityAuditService;
        this.jwtValidationUtil = jwtValidationUtil;
    }

    public SecureAuthResponse register(RegisterRequest request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        
        // Email-based + IP-based rate limiting for registration endpoint
        String endpoint = "/api/v1/auth/register";
        if (!rateLimitingService.isIpAndEmailWithinRateLimit(clientIp, normalizedEmail, endpoint)) {
            securityAuditService.logRateLimitExceeded(normalizedEmail, clientIp, endpoint, "IP+Email");
            throw new BadRequestException("RATE_LIMIT_EXCEEDED", 
                "Too many registration attempts. Please try again later.");
        }
        
        // Validate registration request (password match, terms, email uniqueness)
        registrationValidator.validate(request);
        
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
        
        SecureAuthResponse response = issueAuthResponse(user, false, clientIp, "User registered successfully. Please check your email for verification.");
        
        // Audit log successful registration
        securityAuditService.logRegistration(true, user.getId(), normalizedEmail, clientIp, null);
        
        return response;
    }
    
    public SecureAuthResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        
        // Email-based + IP-based rate limiting for login endpoint
        String endpoint = "/api/v1/auth/login";
        if (!rateLimitingService.isIpAndEmailWithinRateLimit(clientIp, normalizedEmail, endpoint)) {
            securityAuditService.logRateLimitExceeded(normalizedEmail, clientIp, endpoint, "IP+Email");
            throw new BadRequestException("RATE_LIMIT_EXCEEDED", 
                "Too many login attempts. Please try again later.");
        }
        
        Optional<UserAccount> userOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        
        // Always perform password verification to prevent account enumeration
        String passwordHash = userOpt.map(UserAccount::getPasswordHash)
            .orElse(DUMMY_PASSWORD_HASH);
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), passwordHash);
        
        // If user doesn't exist or password doesn't match, treat as invalid credentials
        if (!userOpt.isPresent() || !passwordMatches) {
            // Only increment failed attempts if user exists
            // This implements incremental lockout: failed attempts extend lockout duration
            if (userOpt.isPresent()) {
                handleFailedLoginAttempt(userOpt.get());
            }
            // Audit log failed login attempt
            UUID userId = userOpt.map(UserAccount::getId).orElse(null);
            securityAuditService.logLoginAttempt(false, userId, normalizedEmail, clientIp, 
                null, null, null, "Invalid credentials");
            // Always return the same error message and status code to prevent account enumeration
            throw new BadRequestException("INVALID_CREDENTIALS", "Invalid credentials");
        }
        
        UserAccount user = userOpt.get();
        
        // Check if account is locked (after password verification to prevent timing leaks)
        if (isAccountLocked(user)) {
            long minutesRemaining = java.time.Duration.between(
                LocalDateTime.now(ZoneOffset.UTC), 
                user.getLockedUntil()
            ).toMinutes();
            // Audit log locked account access attempt
            securityAuditService.logLoginAttempt(false, user.getId(), normalizedEmail, clientIp, 
                null, null, null, "Account locked");
            throw new BadRequestException("ACCOUNT_LOCKED", 
                String.format("Account is temporarily locked due to multiple failed login attempts. Please try again in %d minute(s).", 
                    minutesRemaining));
        }
        
        // Check email verification
        // Return same error as invalid credentials to prevent account enumeration so that attacker cannot determine if an account exists and has correct password but unverified email
        if (!user.isEmailVerified()) {
            securityAuditService.logLoginAttempt(false, user.getId(), normalizedEmail, clientIp, 
                null, null, null, "Email not verified");
            throw new BadRequestException("INVALID_CREDENTIALS", "Invalid credentials");
        }
        
        // Successful login - reset failed attempts and unlock account
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userAccountRepository.save(user);
        
        // Server generates new clientId and deviceId for this session
        // These are returned to the client for use in subsequent requests (security and logging)
        SecureAuthResponse response = issueAuthResponse(user, request.isRememberMe(), clientIp, "Login successful");
        
        // Audit log successful login
        securityAuditService.logLoginAttempt(true, user.getId(), normalizedEmail, clientIp, 
            null, response.getClientId(), response.getDeviceId(), null);
        
        return response;
    }
    
    /**
     * Refreshes a token.
     * @param request The refresh token request
     * @return The refreshed token response
     */
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

    /**
     * Logs out a user from all devices.
     * Requires explicit confirmation to prevent accidental sign-outs.
     * 
     * @param request The logout request containing confirmation
     * @param user The user account to logout
     * @throws BadRequestException if confirmation is not provided or not true
     */
    public void logout(LogoutRequest request, UserAccount user) {
        // Validate confirmation to prevent accidental sign-outs
        if (request.getConfirm() == null || !request.getConfirm()) {
            throw new BadRequestException("CONFIRMATION_REQUIRED", 
                "Confirmation required. Set 'confirm' to true to logout from all devices.");
        }
        
        List<UserSession> userSessions = sessionRepository.findByUser(user);
        userSessions.forEach(session -> session.setRevoked(true));
        sessionRepository.saveAll(userSessions);
    }

    /**
     * Issues authentication response with server-generated clientId and deviceId.
     * These identifiers are generated fresh for each session and returned to the client
     * for use in subsequent requests (security validation and logging).
     * 
     * @param user The authenticated user account
     * @param rememberMe Whether to extend refresh token expiry
     * @param ipAddress Client IP address for session tracking
     * @param message Success message
     * @return SecureAuthResponse with tokens and server-generated identifiers
     */
    private SecureAuthResponse issueAuthResponse(UserAccount user,
                                                 boolean rememberMe,
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

        // Server generates new clientId and deviceId for this session
        String clientId = generateSecureToken();
        String deviceId = generateSecureToken();

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
            .clientId(clientId)
            .deviceId(deviceId)
            .build();
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

    /**
     * Checks if an account is currently locked.
     * @param user The user account to check
     * @return true if the account is locked, false otherwise
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
     * If account is already locked, extends the lockout duration based on new attempt count.
     * 
     * @param user The user account to handle the failed login attempt for
     */
    private void handleFailedLoginAttempt(UserAccount user) {
        int currentAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        int newAttempts = currentAttempts + 1;
        user.setFailedLoginAttempts(newAttempts);
        
        // Apply or extend incremental lockout based on number of failed attempts
        if (newAttempts >= maxFailedAttempts) {
            LocalDateTime lockoutUntil = calculateLockoutDuration(newAttempts);
            LocalDateTime currentLockout = user.getLockedUntil();
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            
            // Always update lockout when threshold is reached or exceeded (15min → 1hr → 24hr).
            boolean isCurrentlyLocked = currentLockout != null && currentLockout.isAfter(now);
            boolean shouldUpdate = currentLockout == null || 
                                  lockoutUntil.isAfter(currentLockout) || 
                                  isCurrentlyLocked;
            
            if (shouldUpdate) {
                user.setLockedUntil(lockoutUntil);
                
                // Calculate lockout duration in minutes
                long lockoutDurationMinutes = java.time.Duration.between(now, lockoutUntil).toMinutes();
                
                if (isCurrentlyLocked) {
                    // Account was already locked - extending lockout due to continued attempts
                    log.warn("Account lockout extended due to continued failed attempts: {} attempts (was {}), " +
                            "locked until {}", newAttempts, currentAttempts, lockoutUntil);
                } else {
                    // First time locking this account - audit log the lockout
                    log.warn("Account locked due to {} failed login attempts: {}, locked until {}", 
                            newAttempts, user.getEmail(), lockoutUntil);
                    securityAuditService.logAccountLocked(user.getId(), user.getEmail(), null, 
                        newAttempts, lockoutDurationMinutes);
                }
            }
        }
        
        userAccountRepository.save(user);
    }
    
    /**
     * Calculates lockout duration based on number of failed attempts.
     * Implements incremental lockout:
     * - 5-9 attempts: 15 minutes
     * - 10-14 attempts: 1 hour
     * - 15+ attempts: 24 hours
     * 
     * @param failedAttempts The number of failed login attempts
     * @return The lockout duration
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

    /**
     * Validates a JWT token and returns validation result.
     * Prevents token enumeration by:
     * - Returning uniform error responses for all failure cases
     * - Adding delays for invalid tokens to slow down enumeration attempts
     * 
     * @param token The JWT token to validate
     * @return TokenValidationResponse with validation result
     */
    public TokenValidationResponse validateToken(String token) {
        // Prevent token enumeration: always return uniform responses
        // Add small delay for invalid tokens to slow down enumeration attempts
        try {
            if (token == null || token.trim().isEmpty()) {
                // Add delay to prevent timing attacks
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return TokenValidationResponse.invalid(null);
            }
            
            boolean valid = jwtValidationUtil.validateToken(token);
            if (!valid) {
                // Add delay for invalid tokens to slow down enumeration
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return TokenValidationResponse.invalid(null);
            }
            
            Claims claims = jwtValidationUtil.getClaimsFromToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            Optional<UserAccount> userOpt = userAccountRepository.findById(userId);
            
            if (userOpt.isEmpty()) {
                // User not found - add delay and return uniform response
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return TokenValidationResponse.invalid(null);
            }
            
            return TokenValidationResponse.valid(AuthMapper.toSecureUserResponse(userOpt.get()));
        } catch (Exception e) {
            // Add delay for any exception to prevent timing-based enumeration
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            // Return uniform response - don't leak error details
            return TokenValidationResponse.invalid(null);
        }
    }
}
