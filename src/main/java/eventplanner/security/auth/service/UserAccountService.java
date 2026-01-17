package eventplanner.security.auth.service;

import eventplanner.security.auth.enums.UserStatus;
import eventplanner.security.auth.enums.VisibilityLevel;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.feeds.dto.response.PostListResponse;
import eventplanner.features.feeds.service.FeedPostService;
import eventplanner.security.auth.dto.req.NotificationSettingsUpdateRequest;
import eventplanner.security.auth.dto.req.PrivacySettingsUpdateRequest;
import eventplanner.security.auth.dto.req.SecuritySettingsUpdateRequest;
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
import eventplanner.security.auth.dto.AuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.normalizeUsername;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;

@Service
@Transactional
public class UserAccountService {

    /**
     * Result of provisioning a Cognito user - indicates whether user was newly created.
     */
    public record ProvisionResult(UserAccount user, boolean created) {}

    private final UserAccountRepository userAccountRepository;
    private final ProfileImageService profileImageService;
    private final LocationRepository locationRepository;
    private final CognitoUserService cognitoUserService;
    
    @Autowired(required = false)
    private FeedPostService feedPostService;

    public UserAccountService(UserAccountRepository userAccountRepository,
                             ProfileImageService profileImageService,
                             LocationRepository locationRepository,
                             CognitoUserService cognitoUserService) {
        this.userAccountRepository = userAccountRepository;
        this.profileImageService = profileImageService;
        this.locationRepository = locationRepository;
        this.cognitoUserService = cognitoUserService;
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

    /**
     * Provision or update a local user record from a Cognito signup.
     * If the user already exists (by email), update their Cognito sub if provided.
     * Otherwise, create a new user account.
     *
     * @param request The signup request containing user details
     * @return ProvisionResult with the user and whether they were newly created
     */
    public ProvisionResult provisionCognitoUser(SignupRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email == null) {
            throw new IllegalArgumentException("Valid email is required");
        }

        Optional<UserAccount> existingUser = userAccountRepository.findByEmailIgnoreCase(email);
        
        if (existingUser.isPresent()) {
            UserAccount user = existingUser.get();
            // Update Cognito sub if provided and not already set
            if (StringUtils.hasText(request.getCognitoSub()) && !StringUtils.hasText(user.getCognitoSub())) {
                user.setCognitoSub(request.getCognitoSub());
                userAccountRepository.save(user);
            }
            return new ProvisionResult(user, false);
        }

        // Create new user
        UserAccount newUser = new UserAccount();
        newUser.setEmail(email);
        newUser.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : email.split("@")[0]);
        newUser.setCognitoSub(request.getCognitoSub());
        newUser.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null);
        newUser.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));
        newUser.setAcceptTerms(Boolean.TRUE.equals(request.getAcceptTerms()));
        newUser.setAcceptPrivacy(Boolean.TRUE.equals(request.getAcceptPrivacy()));
        newUser.setUserType(request.getUserType());
        newUser.setProfilePictureUrl(getDefaultProfilePictureUrl());
        
        // Mark profile as complete if required fields are present
        if (StringUtils.hasText(newUser.getName()) && newUser.isAcceptTerms() && newUser.isAcceptPrivacy()) {
            newUser.setProfileCompleted(true);
        }

        UserAccount savedUser = userAccountRepository.save(newUser);
        return new ProvisionResult(savedUser, true);
    }

    /**
     * Get all posts created by the current user.
     */
    public PostListResponse getUserPosts(UserPrincipal principal, Integer page, Integer size) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        if (feedPostService == null) {
            // Return empty response if FeedPostService is not available
            PostListResponse response = new PostListResponse();
            response.setPosts(Collections.emptyList());
            response.setCurrentPage(page != null ? page : 0);
            response.setPageSize(size != null ? size : 20);
            response.setTotalPosts(0L);
            response.setTotalPages(0);
            response.setHasNext(false);
            response.setHasPrevious(false);
            return response;
        }
        
        return feedPostService.getUserPosts(principal.getId(), page, size, principal);
    }

    /**
     * Update notification settings for the current user
     */
    public SecureUserResponse updateNotificationSettings(
            UserPrincipal principal, NotificationSettingsUpdateRequest request) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        UserAccount user = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserSettings settings = getOrCreateSettings(user);
        if (request.getEmailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getPushNotificationsEnabled() != null) {
            settings.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            settings.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
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
        if (request.getReminderTimingMinutes() != null) {
            settings.setReminderTimingMinutes(request.getReminderTimingMinutes());
        }
        
        userAccountRepository.save(user);
        return AuthMapper.toSecureUserResponse(user);
    }

    /**
     * Update privacy settings for the current user
     */
    public SecureUserResponse updatePrivacySettings(
            UserPrincipal principal, PrivacySettingsUpdateRequest request) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        UserAccount user = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserSettings settings = getOrCreateSettings(user);
        if (request.getProfileVisibility() != null) {
            settings.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getEventParticipationVisibility() != null) {
            settings.setEventParticipationVisibility(request.getEventParticipationVisibility());
        }
        if (request.getSearchVisibility() != null) {
            settings.setSearchVisibility(request.getSearchVisibility());
        }
        if (request.getShowInEventDirectory() != null) {
            settings.setShowInEventDirectory(request.getShowInEventDirectory());
        }
        
        userAccountRepository.save(user);
        return AuthMapper.toSecureUserResponse(user);
    }

    /**
     * Update security settings for the current user
     */
    public SecureUserResponse updateSecuritySettings(
            UserPrincipal principal, SecuritySettingsUpdateRequest request) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        UserAccount user = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserSettings settings = getOrCreateSettings(user);
        if (request.getMfaEnabled() != null) {
            settings.setMfaEnabled(request.getMfaEnabled());
        }
        if (request.getAutoAcceptInvitations() != null) {
            settings.setAutoAcceptInvitations(request.getAutoAcceptInvitations());
        }
        if (request.getExportEventDataEnabled() != null) {
            settings.setExportEventDataEnabled(request.getExportEventDataEnabled());
        }
        
        userAccountRepository.save(user);
        return AuthMapper.toSecureUserResponse(user);
    }

}
