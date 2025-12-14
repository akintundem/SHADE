package eventplanner.common.util;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for common UserAccount operations.
 * Provides helper methods to avoid repetitive code for fetching UserAccount entities.
 */
public final class UserAccountUtil {

    private UserAccountUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Get UserAccount from UserPrincipal.
     * Prefers the direct user from principal if available, otherwise fetches from repository.
     * 
     * @param principal The UserPrincipal (can be null)
     * @param userAccountRepository The repository to fetch from if needed
     * @return Optional containing the UserAccount, or empty if principal is null or user not found
     */
    public static Optional<UserAccount> getUserAccount(UserPrincipal principal, UserAccountRepository userAccountRepository) {
        if (principal == null) {
            return Optional.empty();
        }
        
        // Prefer direct user from principal if available
        if (principal.getUser() != null) {
            return Optional.of(principal.getUser());
        }
        
        // Fallback to fetching from repository if user is not directly available
        if (principal.getId() != null) {
            return userAccountRepository.findById(principal.getId());
        }
        
        return Optional.empty();
    }

    /**
     * Get UserAccount from UserPrincipal, throwing exception if not found.
     * 
     * @param principal The UserPrincipal (can be null)
     * @param userAccountRepository The repository to fetch from if needed
     * @param errorMessage Error message if user not found
     * @return The UserAccount entity
     * @throws IllegalArgumentException if principal is null or user not found
     */
    public static UserAccount getUserAccountOrThrow(UserPrincipal principal, UserAccountRepository userAccountRepository, String errorMessage) {
        return getUserAccount(principal, userAccountRepository)
            .orElseThrow(() -> new IllegalArgumentException(errorMessage != null ? errorMessage : "User not found"));
    }

    /**
     * Get UserAccount from UserPrincipal, throwing exception if not found.
     * Uses default error message.
     * 
     * @param principal The UserPrincipal (can be null)
     * @param userAccountRepository The repository to fetch from if needed
     * @return The UserAccount entity
     * @throws IllegalArgumentException if principal is null or user not found
     */
    public static UserAccount getUserAccountOrThrow(UserPrincipal principal, UserAccountRepository userAccountRepository) {
        return getUserAccountOrThrow(principal, userAccountRepository, "User not found");
    }

    /**
     * Get managed UserAccount entity from repository for JPA relationship setting.
     * This ensures the entity is managed by the persistence context.
     * 
     * @param principal The UserPrincipal (can be null)
     * @param userAccountRepository The repository to fetch from
     * @return Optional containing the managed UserAccount, or empty if principal is null or user not found
     */
    public static Optional<UserAccount> getManagedUserAccount(UserPrincipal principal, UserAccountRepository userAccountRepository) {
        if (principal == null || principal.getId() == null) {
            return Optional.empty();
        }
        
        return userAccountRepository.findById(principal.getId());
    }

    /**
     * Get managed UserAccount entity from repository for JPA relationship setting.
     * This ensures the entity is managed by the persistence context.
     * Throws exception if not found.
     * 
     * @param principal The UserPrincipal (can be null)
     * @param userAccountRepository The repository to fetch from
     * @param errorMessage Error message if user not found
     * @return The managed UserAccount entity
     * @throws IllegalArgumentException if principal is null or user not found
     */
    public static UserAccount getManagedUserAccountOrThrow(UserPrincipal principal, UserAccountRepository userAccountRepository, String errorMessage) {
        return getManagedUserAccount(principal, userAccountRepository)
            .orElseThrow(() -> new IllegalArgumentException(errorMessage != null ? errorMessage : "User not found"));
    }

    /**
     * Get managed UserAccount entity from repository for JPA relationship setting.
     * This ensures the entity is managed by the persistence context.
     * Uses default error message.
     * 
     * @param principal The UserPrincipal (can be null)
     * @param userAccountRepository The repository to fetch from
     * @return The managed UserAccount entity
     * @throws IllegalArgumentException if principal is null or user not found
     */
    public static UserAccount getManagedUserAccountOrThrow(UserPrincipal principal, UserAccountRepository userAccountRepository) {
        return getManagedUserAccountOrThrow(principal, userAccountRepository, "User not found");
    }

    /**
     * Get UserAccount from UUID.
     * 
     * @param userId The user ID (can be null)
     * @param userAccountRepository The repository to fetch from
     * @return Optional containing the UserAccount, or empty if userId is null or user not found
     */
    public static Optional<UserAccount> getUserAccountById(UUID userId, UserAccountRepository userAccountRepository) {
        if (userId == null) {
            return Optional.empty();
        }
        return userAccountRepository.findById(userId);
    }

    /**
     * Get UserAccount from UUID, throwing exception if not found.
     * 
     * @param userId The user ID (can be null)
     * @param userAccountRepository The repository to fetch from
     * @param errorMessage Error message if user not found
     * @return The UserAccount entity
     * @throws IllegalArgumentException if userId is null or user not found
     */
    public static UserAccount getUserAccountByIdOrThrow(UUID userId, UserAccountRepository userAccountRepository, String errorMessage) {
        return getUserAccountById(userId, userAccountRepository)
            .orElseThrow(() -> new IllegalArgumentException(errorMessage != null ? errorMessage : "User not found: " + userId));
    }

    /**
     * Get UserAccount from UUID, throwing exception if not found.
     * Uses default error message.
     * 
     * @param userId The user ID (can be null)
     * @param userAccountRepository The repository to fetch from
     * @return The UserAccount entity
     * @throws IllegalArgumentException if userId is null or user not found
     */
    public static UserAccount getUserAccountByIdOrThrow(UUID userId, UserAccountRepository userAccountRepository) {
        return getUserAccountByIdOrThrow(userId, userAccountRepository, null);
    }
}
