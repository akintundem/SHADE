package eventplanner.security.auth.service;

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
import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public UserAccountService(UserAccountRepository userAccountRepository,
                             ProfileImageService profileImageService,
                             LocationRepository locationRepository) {
        this.userAccountRepository = userAccountRepository;
        this.profileImageService = profileImageService;
        this.locationRepository = locationRepository;
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
            .searchDirectoryUsers(sanitized, VisibilityLevel.PRIVATE, pageable)
            .map(this::toPublicUserResponse);
    }

    /**
     * Public directory listing (paginated). Intended for "suggested users" UX.
     */
    public Page<PublicUserResponse> listPublicUsers(Pageable pageable) {
        return userAccountRepository.listDirectoryUsers(VisibilityLevel.PRIVATE, pageable)
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
}
