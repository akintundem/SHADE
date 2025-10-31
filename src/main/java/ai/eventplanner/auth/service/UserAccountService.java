package ai.eventplanner.auth.service;

import ai.eventplanner.auth.dto.req.UpdateUserProfileRequest;
import ai.eventplanner.auth.dto.res.SecureUserResponse;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.repo.UserAccountRepository;
import ai.eventplanner.auth.util.AuthMapper;
import ai.eventplanner.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ai.eventplanner.auth.util.AuthValidationUtil.normalizeEmail;
import static ai.eventplanner.auth.util.AuthValidationUtil.safeTrim;

@Service
@Transactional
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public SecureUserResponse getSecureUser(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthMapper.toSecureUserResponse(user);
    }

    public SecureUserResponse updateSecureUser(UUID userId, UserAccount requester, UpdateUserProfileRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(request.getName().trim());
        user.setPhoneNumber(safeTrim(request.getPhoneNumber()));
        user.setProfileImageUrl(safeTrim(request.getProfileImageUrl()));
        user.setUserType(request.getUserType());
        user.setPreferences(safeTrim(request.getPreferences()));
        user.setMarketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()));

        return AuthMapper.toSecureUserResponse(user);
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
