package eventplanner.security.auth.validation;

import eventplanner.security.auth.dto.req.RegisterRequest;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.validatePasswordMatch;

/**
 * Validates registration requests before processing.
 * Ensures password match and email uniqueness.
 * Profile completion (name, terms, etc.) happens during onboarding after email verification.
 */
@Component
@RequiredArgsConstructor
public class RegistrationValidator {

    private final UserAccountRepository userAccountRepository;

    /**
     * Validates a registration request.
     * Only validates email and password. Other profile fields are collected during onboarding.
     * 
     * @param request The registration request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        // Validate password match and strength
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());

        // Check email uniqueness (only for verified users - unverified users can re-register)
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(user -> user.isEmailVerified())
                .orElse(false)) {
            throw new IllegalArgumentException("Registration failed: email is already registered");
        }
    }
}

