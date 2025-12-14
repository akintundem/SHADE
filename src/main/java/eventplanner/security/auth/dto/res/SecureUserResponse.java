package eventplanner.security.auth.dto.res;

import eventplanner.common.domain.enums.UserType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Secure User Response DTO that excludes sensitive internal identifiers
 * and other fields that shouldn't be exposed to clients.
 * Includes userId so clients can use it in subsequent requests without decoding JWT.
 */
@Value
@Builder
public class SecureUserResponse {
    UUID id;
    String email;
    String name;
    String username;
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
