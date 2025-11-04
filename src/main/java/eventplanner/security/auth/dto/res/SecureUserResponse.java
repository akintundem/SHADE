package eventplanner.security.auth.dto.res;

import eventplanner.common.domain.enums.UserType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Secure User Response DTO that excludes sensitive internal identifiers
 * and other fields that shouldn't be exposed to clients.
 */
@Value
@Builder
public class SecureUserResponse {
    String email;
    String name;
    String phoneNumber;
    LocalDate dateOfBirth;
    UserType userType;
    boolean emailVerified;
    boolean marketingOptIn;
    String profileImageUrl;
    String preferences;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
