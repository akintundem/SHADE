package ai.eventplanner.auth.service;

import ai.eventplanner.auth.dto.req.LoginRequest;
import ai.eventplanner.auth.dto.req.RefreshTokenRequest;
import ai.eventplanner.auth.dto.req.RegisterRequest;
import ai.eventplanner.auth.dto.res.SecureAuthResponse;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.entity.UserSession;
import ai.eventplanner.auth.repo.UserAccountRepository;
import ai.eventplanner.auth.repo.UserSessionRepository;
import ai.eventplanner.auth.util.AuthMapper;
import ai.eventplanner.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static ai.eventplanner.auth.util.AuthValidationUtil.normalizeEmail;
import static ai.eventplanner.auth.util.AuthValidationUtil.safeTrim;
import static ai.eventplanner.auth.util.AuthValidationUtil.validatePasswordMatch;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Value("${auth.session.max-concurrent:5}")
    private int maxConcurrentSessions;

    public AuthService(UserAccountRepository userAccountRepository,
                       UserSessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
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
            .userType(ai.eventplanner.common.domain.enums.UserType.INDIVIDUAL)
            .build();
        userAccountRepository.save(user);
        userAccountRepository.flush();

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
