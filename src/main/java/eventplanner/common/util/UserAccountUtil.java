package eventplanner.common.util;

import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;

/**
 * Utility class for common UserAccount operations.
 * Provides helper methods to avoid repetitive code for fetching UserAccount entities.
 */
public final class UserAccountUtil {

    private UserAccountUtil() {
        // Utility class - prevent instantiation
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
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException(errorMessage != null ? errorMessage : "User not found");
        }
        
        return userAccountRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalArgumentException(errorMessage != null ? errorMessage : "User not found"));
    }
}
