package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.req.SignupRequest;
import eventplanner.security.auth.dto.req.UpdateUserProfileRequest;
import eventplanner.security.auth.dto.req.UserSettingsUpdateRequest;
import eventplanner.security.auth.dto.res.PublicUserResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.entity.Location;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSettings;
import eventplanner.security.auth.repository.LocationRepository;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.util.AuthMapper;
import eventplanner.common.domain.enums.UserType;
import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.common.domain.enums.UserStatus;
import eventplanner.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.normalizeUsername;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;

@Service
@Transactional
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final ProfileImageService profileImageService;
    private final LocationRepository locationRepository;
    private final CognitoUserService cognitoUserService;

    public UserAccountService(UserAccountRepository userAccountRepository,
                             ProfileImageService profileImageService,
                             LocationRepository locationRepository,
                             CognitoUserService cognitoUserService) {
        this.userAccountRepository = userAccountRepository;
        this.profileImageService = profileImageService;
        this.locationRepository = locationRepository;
        this.cognitoUserService = cognitoUserService;
    }

    /**
     * Provision a local user record for Cognito sign-up flows.
     * - Creates a new user if none exists for the email.
     * - Idempotently updates existing users with Cognito subject and preferences.
     * - Rejects attempts to overwrite a different Cognito subject.
     */
    public ProvisionedUser provisionCognitoUser(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new IllegalArgumentException("Email is required");
        }

        String requestedName = safeTrim(request.getName());
        String resolvedName = StringUtils.hasText(requestedName)
                ? requestedName
                : deriveDisplayName(normalizedEmail);

        String phone = safeTrim(request.getPhoneNumber());
        String cognitoSub = safeTrim(request.getCognitoSub());
        UserType userType = request.getUserType() != null ? request.getUserType() : UserType.INDIVIDUAL;

        var existingUserOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        boolean created = existingUserOpt.isEmpty();

        // If a user already exists and is linked to Cognito, block duplicate signup attempts
        if (existingUserOpt.isPresent()) {
            UserAccount existing = existingUserOpt.get();
            String existingSub = safeTrim(existing.getCognitoSub());
            if (StringUtils.hasText(existingSub) && (cognitoSub == null || !existingSub.equals(cognitoSub))) {
                throw new IllegalStateException("Invalid signup attempt");
            }
        }

        UserAccount user = existingUserOpt.orElseGet(() -> {
            UserAccount newUser = UserAccount.builder()
                    .email(normalizedEmail)
                    .name(resolvedName)
                    .phoneNumber(StringUtils.hasText(phone) ? phone : null)
                    .marketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()))
                    .acceptTerms(Boolean.TRUE.equals(request.getAcceptTerms()))
                    .acceptPrivacy(Boolean.TRUE.equals(request.getAcceptPrivacy()))
                    .userType(userType)
                    .status(UserStatus.ACTIVE)
                    .profileCompleted(false)
                    .build();
            if (StringUtils.hasText(cognitoSub)) {
                newUser.setCognitoSub(cognitoSub);
            }
            newUser.setSettings(UserSettings.createDefault(newUser));
            return newUser;
        });

        // Update existing user with new attributes when present
        if (StringUtils.hasText(requestedName) || !StringUtils.hasText(user.getName())) {
            user.setName(resolvedName);
        }
        if (StringUtils.hasText(phone)) {
            user.setPhoneNumber(phone);
        }
        if (request.getMarketingOptIn() != null) {
            user.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));
        }
        if (request.getAcceptTerms() != null && request.getAcceptTerms()) {
            user.setAcceptTerms(true);
        }
        if (request.getAcceptPrivacy() != null && request.getAcceptPrivacy()) {
            user.setAcceptPrivacy(true);
        }
        if (request.getUserType() != null) {
            user.setUserType(userType);
        }

        if (StringUtils.hasText(cognitoSub)) {
            String existingSub = safeTrim(user.getCognitoSub());
            if (StringUtils.hasText(existingSub) && !existingSub.equals(cognitoSub)) {
                throw new IllegalStateException("User already linked to a different Cognito subject");
            }
            user.setCognitoSub(cognitoSub);
        }

        if (user.getSettings() == null) {
            user.setSettings(UserSettings.createDefault(user));
        }

        UserAccount saved = userAccountRepository.save(user);
        return new ProvisionedUser(saved, created);
    }

    public SecureUserResponse getSecureUser(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthMapper.toSecureUserResponse(user);
    }

    public SecureUserResponse updateSecureUser(UUID userId, UserAccount requester, UpdateUserProfileRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.length() < 2) {
                throw new IllegalArgumentException("Name must be between 2 and 100 characters");
            }
            user.setName(trimmedName);
        }
        
        if (request.getUsername() != null) {
            String normalized = normalizeUsername(request.getUsername());
            if (normalized != null) {
                String existing = user.getUsername();
                if (StringUtils.hasText(existing)) {
                    if (!existing.equalsIgnoreCase(normalized)) {
                        throw new IllegalArgumentException("Username cannot be changed once set");
                    }
                } else {
                    boolean taken = userAccountRepository.existsByUsernameIgnoreCase(normalized);
                    if (taken) {
                        throw new IllegalArgumentException("Username is already taken");
                    }
                    user.setUsername(normalized);
                }
            }
        }
        
        if (request.getPhoneNumber() != null) {
            String trimmedPhone = safeTrim(request.getPhoneNumber());
            user.setPhoneNumber(StringUtils.hasText(trimmedPhone) ? trimmedPhone : null);
        }
        
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        
        // Handle profile picture: use provided URL or default placeholder
        String profilePictureUrl = safeTrim(request.getProfilePictureUrl());
        if (StringUtils.hasText(profilePictureUrl)) {
            user.setProfilePictureUrl(profileImageService.normalizeResourceUrl(profilePictureUrl));
        } else if (user.getProfilePictureUrl() == null) {
            // Set default placeholder if no image provided and user doesn't have one
            user.setProfilePictureUrl(getDefaultProfilePictureUrl());
        }
        
        // Update terms/privacy acceptance (only if provided and user hasn't accepted yet)
        if (Boolean.TRUE.equals(request.getAcceptTerms()) && !user.isAcceptTerms()) {
            user.setAcceptTerms(true);
        }
        if (Boolean.TRUE.equals(request.getAcceptPrivacy()) && !user.isAcceptPrivacy()) {
            user.setAcceptPrivacy(true);
        }
        
        // Update other fields
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        if (request.getPreferences() != null) {
            String trimmedPreferences = safeTrim(request.getPreferences());
            user.setPreferences(StringUtils.hasText(trimmedPreferences) ? trimmedPreferences : null);
        }
        if (request.getMarketingOptIn() != null) {
            user.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));
        }
        if (request.getSettings() != null) {
            UserSettings settings = getOrCreateSettings(user);
            applySettingsUpdate(settings, request.getSettings());
        }
        
        // Auto-complete profile if it was incomplete and required fields are now present
        if (!Boolean.TRUE.equals(user.getProfileCompleted()) 
            && StringUtils.hasText(user.getName())
            && user.isAcceptTerms() 
            && user.isAcceptPrivacy()) {
            user.setProfileCompleted(true);
        }

        userAccountRepository.save(user);
        return AuthMapper.toSecureUserResponse(user);
    }

    /**
     * Deactivate a user account and revoke active sessions.
     */
    public void deleteUserAccount(UUID userId, UserAccount requester) {
        if (requester == null || requester.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (!requester.getId().equals(userId)) {
            throw new AccessDeniedException("Cannot delete another user's account");
        }

        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Attempt Cognito removal before marking the account as deleted locally
        cognitoUserService.deleteUser(user.getCognitoSub(), user.getEmail());

        if (user.getStatus() != UserStatus.DELETED) {
            user.setStatus(UserStatus.DELETED);
            userAccountRepository.save(user);
        }
    }

    private UserSettings getOrCreateSettings(UserAccount user) {
        UserSettings settings = user.getSettings();
        if (settings == null) {
            settings = UserSettings.createDefault(user);
            user.setSettings(settings);
        }
        return settings;
    }

    private void applySettingsUpdate(UserSettings settings, UserSettingsUpdateRequest request) {
        if (request.getBio() != null) {
            String bio = safeTrim(request.getBio());
            settings.setBio(StringUtils.hasText(bio) ? bio : null);
        }
        if (request.getLocation() != null && request.getLocation().getLocationId() != null) {
            UUID locationId = request.getLocation().getLocationId();
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + locationId));
            settings.setLocation(location);
        } else if (request.getLocation() != null) {
            settings.setLocation(null);
        }
        if (request.getPreferredLanguage() != null) {
            settings.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getProfileVisibility() != null) {
            settings.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getEventParticipationVisibility() != null) {
            settings.setEventParticipationVisibility(request.getEventParticipationVisibility());
        }
        if (request.getSearchVisibility() != null) {
            settings.setSearchVisibility(request.getSearchVisibility());
        }
        if (request.getThemePreference() != null) {
            settings.setThemePreference(request.getThemePreference());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getPushNotificationsEnabled() != null) {
            settings.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getEventInvitationsEnabled() != null) {
            settings.setEventInvitationsEnabled(request.getEventInvitationsEnabled());
        }
        if (request.getEventUpdatesEnabled() != null) {
            settings.setEventUpdatesEnabled(request.getEventUpdatesEnabled());
        }
        if (request.getEventRemindersEnabled() != null) {
            settings.setEventRemindersEnabled(request.getEventRemindersEnabled());
        }
        if (request.getRsvpNotificationsEnabled() != null) {
            settings.setRsvpNotificationsEnabled(request.getRsvpNotificationsEnabled());
        }
        if (request.getCommentNotificationsEnabled() != null) {
            settings.setCommentNotificationsEnabled(request.getCommentNotificationsEnabled());
        }
        if (request.getCollaborationRequestsEnabled() != null) {
            settings.setCollaborationRequestsEnabled(request.getCollaborationRequestsEnabled());
        }
        if (request.getWeeklyDigestEnabled() != null) {
            settings.setWeeklyDigestEnabled(request.getWeeklyDigestEnabled());
        }
        if (request.getActivityFeedNotificationsEnabled() != null) {
            settings.setActivityFeedNotificationsEnabled(request.getActivityFeedNotificationsEnabled());
        }
        if (request.getAutoAcceptInvitations() != null) {
            settings.setAutoAcceptInvitations(request.getAutoAcceptInvitations());
        }
        if (request.getExportEventDataEnabled() != null) {
            settings.setExportEventDataEnabled(request.getExportEventDataEnabled());
        }
        if (request.getMfaEnabled() != null) {
            settings.setMfaEnabled(request.getMfaEnabled());
        }
        if (request.getReminderTimingMinutes() != null) {
            settings.setReminderTimingMinutes(request.getReminderTimingMinutes());
        }
        if (request.getShowInEventDirectory() != null) {
            settings.setShowInEventDirectory(request.getShowInEventDirectory());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            settings.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
    }

    private String getDefaultProfilePictureUrl() {
        // Return a consistent Unsplash placeholder URL
        // You could also make this configurable via application properties
        return "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&h=400&fit=crop&auto=format";
    }

    public Page<SecureUserResponse> searchSecureUsers(String term, Pageable pageable) {
        if (term == null || term.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty");
        }

        String sanitized = term.trim();
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Search term too long");
        }

        if (sanitized.matches(".*[;'\"\\\\].*")) {
            throw new IllegalArgumentException("Invalid characters in search term");
        }

        return userAccountRepository
            .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(sanitized, sanitized, pageable)
            .map(AuthMapper::toSecureUserResponse);
    }

    public Page<PublicUserResponse> searchPublicUsers(String term, Pageable pageable) {
        if (term == null || term.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty");
        }

        String sanitized = term.trim();
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Search term too long");
        }

        if (sanitized.matches(".*[;'\"\\\\].*")) {
            throw new IllegalArgumentException("Invalid characters in search term");
        }

        return userAccountRepository
            .searchDirectoryUsers(sanitized, VisibilityLevel.PRIVATE, UserStatus.ACTIVE, pageable)
            .map(this::toPublicUserResponse);
    }

    /**
     * Public directory listing (paginated). Intended for "suggested users" UX.
     */
    public Page<PublicUserResponse> listPublicUsers(Pageable pageable) {
        return userAccountRepository.listDirectoryUsers(VisibilityLevel.PRIVATE, UserStatus.ACTIVE, pageable)
            .map(this::toPublicUserResponse);
    }

    private PublicUserResponse toPublicUserResponse(UserAccount user) {
        return PublicUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    public boolean emailExists(String email) {
        return userAccountRepository.existsByEmailIgnoreCase(email);
    }

    public java.util.Optional<UserAccount> findByEmail(String email) {
        return userAccountRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    public UserAccount save(UserAccount user) {
        return userAccountRepository.save(user);
    }

    private String deriveDisplayName(String email) {
        if (!StringUtils.hasText(email)) {
            return "User";
        }
        String localPart = email.split("@")[0];
        if (!StringUtils.hasText(localPart)) {
            return "User";
        }
        String normalized = localPart.trim();
        return normalized.substring(0, Math.min(normalized.length(), 120));
    }

    public record ProvisionedUser(UserAccount user, boolean created) {}
}
