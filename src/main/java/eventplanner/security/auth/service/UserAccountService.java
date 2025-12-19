package eventplanner.security.auth.service;

import eventplanner.security.auth.dto.req.UpdateUserProfileRequest;
import eventplanner.security.auth.dto.res.PublicUserResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.util.AuthMapper;
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

    public UserAccountService(UserAccountRepository userAccountRepository,
                             ProfileImageService profileImageService) {
        this.userAccountRepository = userAccountRepository;
        this.profileImageService = profileImageService;
    }

    public SecureUserResponse getSecureUser(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthMapper.toSecureUserResponse(user);
    }

    public SecureUserResponse updateSecureUser(UUID userId, UserAccount requester, UpdateUserProfileRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update name
        user.setName(request.getName().trim());
        
        // Update username with uniqueness check
        if (request.getUsername() != null) {
            String normalized = normalizeUsername(request.getUsername());
            if (normalized != null) {
                // Ensure uniqueness (case-insensitive) - allow keeping current username
                boolean taken = userAccountRepository.existsByUsernameIgnoreCase(normalized)
                        && (user.getUsername() == null || !user.getUsername().equalsIgnoreCase(normalized));
                if (taken) {
                    throw new IllegalArgumentException("Username is already taken");
                }
                user.setUsername(normalized);
            }
        }
        
        // Update phone number
        user.setPhoneNumber(safeTrim(request.getPhoneNumber()));
        
        // Update date of birth
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
        user.setPreferences(safeTrim(request.getPreferences()));
        if (request.getMarketingOptIn() != null) {
            user.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));
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
            .findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(sanitized, sanitized, pageable)
            .map(this::toPublicUserResponse);
    }

    /**
     * Public directory listing (paginated). Intended for "suggested users" UX.
     */
    public Page<PublicUserResponse> listPublicUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable).map(this::toPublicUserResponse);
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
