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
    private final RateLimitingService rateLimitingService;
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
                       RateLimitingService rateLimitingService,
                       JwtValidationUtil jwtValidationUtil) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
        this.registrationValidator = registrationValidator;
        this.rateLimitingService = rateLimitingService;
        this.jwtValidationUtil = jwtValidationUtil;
    }

    public void register(RegisterRequest request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        
        // Email-based + IP-based rate limiting for registration endpoint
        String endpoint = "/api/v1/auth/register";
        if (!rateLimitingService.isIpAndEmailWithinRateLimit(clientIp, normalizedEmail, endpoint)) {
            throw new BadRequestException("RATE_LIMIT_EXCEEDED", 
                "Too many registration attempts. Please try again later.");
        }
        
        // Validate registration request (password match, email uniqueness)
        registrationValidator.validate(request);
        
        Optional<UserAccount> existingUserOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        UserAccount user;
        boolean isReRegistration = existingUserOpt.isPresent();
        
        if (isReRegistration) {
            // User exists but is unverified - update password and resend verification
            // Profile fields will be collected during onboarding after email verification
            user = existingUserOpt.get();
            log.info("Updating unverified user account and resending verification: {}", normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userAccountRepository.save(user);
        } else {
            // New user - create account with minimal info (email + password only)
            // Profile completion happens during onboarding after email verification
            // Name is required (non-nullable), so we set a placeholder that will be replaced during onboarding
            user = UserAccount.builder()
                .email(normalizedEmail)
                .name("") // Placeholder - will be set during onboarding
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .profileCompleted(false)
                .acceptTerms(false) // Will be collected during onboarding
                .acceptPrivacy(false) // Will be collected during onboarding
                .marketingOptIn(false)
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

        // Send confirmation email - registration fails if email cannot be sent
        String confirmLink = baseUrl + "/api/v1/auth/verify-email?token=" + rawToken;
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("user_name", ""); // Name not collected yet during minimal registration
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
        
        // Registration complete - user must verify email and login to get tokens/deviceId
    }
    
    public SecureAuthResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        
        // Email-based + IP-based rate limiting for login endpoint
        String endpoint = "/api/v1/auth/login";
        if (!rateLimitingService.isIpAndEmailWithinRateLimit(clientIp, normalizedEmail, endpoint)) {
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
            throw new BadRequestException("ACCOUNT_LOCKED", 
                String.format("Account is temporarily locked due to multiple failed login attempts. Please try again in %d minute(s).", 
                    minutesRemaining));
        }
        
        // Check email verification
        // Return same error as invalid credentials to prevent account enumeration so that attacker cannot determine if an account exists and has correct password but unverified email
        if (!user.isEmailVerified()) {
            throw new BadRequestException("INVALID_CREDENTIALS", "Invalid credentials");
        }
        
        // Successful login - reset failed attempts and unlock account
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userAccountRepository.save(user);
        
        // Check if onboarding is required (profile not completed)
        // Profile is considered incomplete if profileCompleted flag is false or required fields are missing
        boolean onboardingRequired = !isProfileComplete(user);
        
        // Server generates deviceId for this session
        // DeviceId is returned to the client for use in subsequent requests (security and logging)
        SecureAuthResponse response = issueAuthResponse(user, request.isRememberMe(), clientIp, "Login successful", onboardingRequired);
        
        return response;
    }
    
    /**
     * Refreshes a token.
     * Validates that the deviceId from header matches the session's deviceId.
     * @param principal The authenticated principal (must include device context)
     * @return The refreshed token response
     */
    public SecureAuthResponse refreshToken(RefreshTokenRequest request, UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String deviceId = principal.getDeviceId();
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BadRequestException("DEVICE_ID_REQUIRED", "Device identifier is required");
        }

        UserSession session = sessionRepository.findByRefreshTokenAndRevokedFalse(request.getRefreshToken())
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            session.setRevoked(true);
            sessionRepository.save(session);
            throw new UnauthorizedException("Refresh token expired");
        }
        
        // Validate deviceId matches session's deviceId
        if (!deviceId.trim().equals(session.getDeviceId())) {
            log.warn("DeviceId mismatch for refresh token: header={}, session={}", deviceId, session.getDeviceId());
            throw new UnauthorizedException("Device ID mismatch");
        }

        // Ensure refresh token belongs to the authenticated user
        if (!session.getUser().getId().equals(principal.getId())) {
            log.warn("Refresh token user mismatch: token user={}, authenticated user={}",
                session.getUser().getId(), principal.getId());
            throw new UnauthorizedException("Refresh token does not belong to the authenticated user");
        }

        // Validate session is still valid
        if (!session.isValid()) {
            throw new UnauthorizedException("Session expired or revoked");
        }

        session.setLastSeenAt(LocalDateTime.now(ZoneOffset.UTC));
        sessionRepository.save(session);
        UserAccount user = session.getUser();
        String accessToken = tokenService.generateAccessToken(user);

        // Check if onboarding is still required after token refresh
        boolean onboardingRequired = !isProfileComplete(user);
        
        return SecureAuthResponse.builder()
            .message("Token refreshed successfully")
            .user(AuthMapper.toSecureUserResponse(user))
            .accessToken(accessToken)
            .refreshToken(session.getRefreshToken())
            .tokenType("Bearer")
            .deviceId(session.getDeviceId())
            .onboardingRequired(onboardingRequired)
            .build();
    }

    /**
     * Logs out a user from a specific device.
     * Revokes the session for the given user + deviceId combination.
     * 
     * @param request The logout request containing confirmation
     * @param user The user account to logout
     * @param deviceId The device identifier associated with the session
     * @throws BadRequestException if confirmation is not provided or not true
     * @throws UnauthorizedException if session not found for user + deviceId
     */
    public void logout(LogoutRequest request, UserAccount user, String deviceId) {
        // Validate confirmation to prevent accidental sign-outs
        if (request.getConfirm() == null || !request.getConfirm()) {
            throw new BadRequestException("CONFIRMATION_REQUIRED", 
                "Confirmation required. Set 'confirm' to true to logout from this device.");
        }
        
        // Look up session by user + deviceId
        Optional<UserSession> sessionOpt = sessionRepository.findByUserAndDeviceId(user, deviceId);
        
        if (sessionOpt.isEmpty()) {
            throw new UnauthorizedException("Session not found for this device");
        }
        
        UserSession session = sessionOpt.get();
        session.revoke();
        sessionRepository.save(session);
    }

    /**
     * Issues authentication response with server-generated deviceId.
     * DeviceId is generated fresh for each session and returned to the client
     * for use in subsequent requests (security validation and logging).
     * Sessions are looked up by user (from JWT) + deviceId in filters.
     * 
     * @param user The authenticated user account
     * @param rememberMe Whether to extend refresh token expiry
     * @param ipAddress Client IP address for session tracking
     * @param message Success message
     * @param onboardingRequired Whether user needs to complete profile onboarding
     * @return SecureAuthResponse with tokens, server-generated deviceId, and onboarding status
     */
    private SecureAuthResponse issueAuthResponse(UserAccount user,
                                                 boolean rememberMe,
                                                 String ipAddress,
                                                 String message,
                                                 boolean onboardingRequired) {
        long activeSessionCount = sessionRepository.countByUserAndRevokedFalse(user);
        if (activeSessionCount >= maxConcurrentSessions) {
            int sessionsToRevoke = (int) (activeSessionCount - maxConcurrentSessions + 1);
            sessionRepository.findByUserAndRevokedFalseOrderByLastSeenAtAsc(user)
                .stream()
                .limit(sessionsToRevoke)
                .forEach(UserSession::revoke);
        }

        // Server generates deviceId for this session
        String deviceId = generateSecureToken();

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken();

        UserSession session = UserSession.builder()
            .user(user)
            .refreshToken(refreshToken)
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
            .deviceId(deviceId)
            .onboardingRequired(onboardingRequired)
            .build();
    }
    
    
    /**
     * Checks if user profile is complete.
     * Profile is considered complete if:
     * - profileCompleted flag is true AND
     * - Required fields are present (name, terms accepted, privacy accepted)
     * 
     * @param user The user account to check
     * @return true if profile is complete, false otherwise
     */
    private boolean isProfileComplete(UserAccount user) {
        if (user.getProfileCompleted() == null || !user.getProfileCompleted()) {
            return false;
        }
        
        // Verify required fields are present
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return false;
        }
        
        if (!user.isAcceptTerms() || !user.isAcceptPrivacy()) {
            return false;
        }
        
        return true;
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
                
                if (isCurrentlyLocked) {
                    // Account was already locked - extending lockout due to continued attempts
                    log.warn("Account lockout extended due to continued failed attempts: {} attempts (was {}), " +
                            "locked until {}", newAttempts, currentAttempts, lockoutUntil);
                } else {
                    // First time locking this account
                    log.warn("Account locked due to {} failed login attempts: {}, locked until {}", 
                            newAttempts, user.getEmail(), lockoutUntil);
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
