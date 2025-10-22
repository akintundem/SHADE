package ai.eventplanner.auth.service;

import ai.eventplanner.auth.dto.AuthResponse;
import ai.eventplanner.auth.dto.ChangePasswordRequest;
import ai.eventplanner.auth.dto.ForgotPasswordRequest;
import ai.eventplanner.auth.dto.LoginRequest;
import ai.eventplanner.auth.dto.OrganizationAddressRequest;
import ai.eventplanner.auth.dto.OrganizationRegisterRequest;
import ai.eventplanner.auth.dto.OrganizationResponse;
import ai.eventplanner.auth.dto.OrganizationUpdateRequest;
import ai.eventplanner.auth.dto.RefreshTokenRequest;
import ai.eventplanner.auth.dto.RegisterRequest;
import ai.eventplanner.auth.dto.ResetPasswordRequest;
import ai.eventplanner.auth.dto.UpdateUserProfileRequest;
import ai.eventplanner.auth.dto.UserResponse;
import ai.eventplanner.auth.dto.UserSessionResponse;
import ai.eventplanner.auth.entity.EmailVerificationToken;
import ai.eventplanner.auth.entity.OrganizationAddress;
import ai.eventplanner.auth.entity.OrganizationProfile;
import ai.eventplanner.auth.entity.PasswordResetToken;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.entity.UserSession;
import ai.eventplanner.auth.repo.EmailVerificationTokenRepository;
import ai.eventplanner.auth.repo.OrganizationProfileRepository;
import ai.eventplanner.auth.repo.PasswordResetTokenRepository;
import ai.eventplanner.auth.repo.UserAccountRepository;
import ai.eventplanner.auth.repo.UserSessionRepository;
import ai.eventplanner.common.domain.enums.OrganizationType;
import ai.eventplanner.common.exception.ForbiddenException;
import ai.eventplanner.common.exception.ResourceNotFoundException;
import ai.eventplanner.common.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final OrganizationProfileRepository organizationRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserAccountRepository userAccountRepository,
                       OrganizationProfileRepository organizationRepository,
                       UserSessionRepository sessionRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse register(RegisterRequest request, String clientIp) {
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

        return issueAuthResponse(user, false, request.getClientId(), request.getDeviceId(), clientIp, "User registered successfully");
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));

        return issueAuthResponse(user, request.isRememberMe(), request.getClientId(), request.getDeviceId(), clientIp, "Login successful");
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
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

        return AuthResponse.builder()
                .message("Token refreshed successfully")
                .user(AuthMapper.toUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(session.getRefreshToken())
                .tokenType("Bearer")
                .build();
    }

    public void logout(UserAccount user) {
        sessionRepository.findByUser(user).forEach(session -> session.setRevoked(true));
    }

    public UserResponse currentUser(UserAccount user) {
        return AuthMapper.toUserResponse(user);
    }

    public UserResponse getUser(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthMapper.toUserResponse(user);
    }

    public UserResponse updateUser(UUID userId, UserAccount requester, UpdateUserProfileRequest request) {
        if (!requester.getId().equals(userId)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(request.getName().trim());
        user.setPhoneNumber(safeTrim(request.getPhoneNumber()));
        user.setProfileImageUrl(safeTrim(request.getProfileImageUrl()));
        user.setUserType(request.getUserType());
        user.setPreferences(safeTrim(request.getPreferences()));
        user.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));

        return AuthMapper.toUserResponse(user);
    }

    public Page<UserResponse> searchUsers(String term, Pageable pageable) {
        String sanitized = term == null ? "" : term.trim();
        return userAccountRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(sanitized, sanitized, pageable)
                .map(AuthMapper::toUserResponse);
    }

    public void changePassword(UserAccount user, ChangePasswordRequest request) {
        validatePasswordMatch(request.getNewPassword(), request.getConfirmPassword());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        sessionRepository.findByUser(user).forEach(session -> session.setRevoked(true));
    }

    public String requestPasswordReset(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        return userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(user -> {
                    PasswordResetToken token = PasswordResetToken.builder()
                            .user(user)
                            .token("prt_" + UUID.randomUUID())
                            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(1))
                            .consumed(false)
                            .build();
                    passwordResetTokenRepository.save(token);
                    return "Password reset email sent";
                })
                .orElse("Password reset email sent");
    }

    public boolean resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (token.isConsumed() || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        token.setConsumed(true);
        sessionRepository.findByUser(user).forEach(session -> session.setRevoked(true));
        return true;
    }

    public String resendVerification(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        return userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(user -> {
                    EmailVerificationToken token = EmailVerificationToken.builder()
                            .user(user)
                            .token("evt_" + UUID.randomUUID())
                            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
                            .consumed(false)
                            .build();
                    emailVerificationTokenRepository.save(token);
                    return "Verification email sent";
                })
                .orElse("Verification email sent");
    }

    public boolean verifyEmailToken(String tokenValue) {
        return emailVerificationTokenRepository.findByToken(tokenValue)
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

    public OrganizationResponse registerOrganization(UserAccount owner, OrganizationRegisterRequest request) {
        OrganizationProfile organization = OrganizationProfile.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : OrganizationType.CORPORATE)
                .website(safeTrim(request.getWebsite()))
                .phoneNumber(request.getPhoneNumber())
                .contactEmail(normalizeEmail(request.getContactEmail()))
                .taxId(safeTrim(request.getTaxId()))
                .registrationNumber(safeTrim(request.getRegistrationNumber()))
                .owner(owner)
                .address(toAddress(request.getAddress()))
                .build();
        organizationRepository.save(organization);
        return AuthMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse updateOrganization(UUID organizationId, UserAccount owner, OrganizationUpdateRequest request) {
        OrganizationProfile organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (organization.getOwner() != null && !organization.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("You are not allowed to update this organization");
        }

        organization.setName(request.getName().trim());
        organization.setDescription(request.getDescription());
        organization.setType(request.getType());
        organization.setWebsite(safeTrim(request.getWebsite()));
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setContactEmail(normalizeEmail(request.getContactEmail()));
        organization.setTaxId(safeTrim(request.getTaxId()));
        organization.setRegistrationNumber(safeTrim(request.getRegistrationNumber()));
        organization.setAddress(toAddress(request.getAddress()));

        if (organization.getOwner() == null) {
            organization.setOwner(owner);
        }

        return AuthMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse getOrganization(UUID organizationId) {
        OrganizationProfile organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return AuthMapper.toOrganizationResponse(organization);
    }

    public Page<OrganizationResponse> searchOrganizations(String term, Pageable pageable) {
        String sanitized = term == null ? "" : term.trim();
        return organizationRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(sanitized, sanitized, pageable)
                .map(AuthMapper::toOrganizationResponse);
    }

    public List<UserSessionResponse> getActiveSessions(UserAccount user) {
        return sessionRepository.findByUserAndRevokedFalse(user).stream()
                .map(AuthMapper::toSessionResponse)
                .collect(Collectors.toList());
    }

    public void terminateAllSessions(UserAccount user) {
        sessionRepository.findByUser(user).forEach(session -> session.setRevoked(true));
    }

    private AuthResponse issueAuthResponse(UserAccount user,
                                           boolean rememberMe,
                                           String clientId,
                                           String deviceId,
                                           String ipAddress,
                                           String message) {
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

        return AuthResponse.builder()
                .message(message)
                .user(AuthMapper.toUserResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    private void validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private OrganizationAddress toAddress(OrganizationAddressRequest request) {
        if (request == null) {
            return null;
        }
        OrganizationAddress address = new OrganizationAddress();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        return address;
    }
}
