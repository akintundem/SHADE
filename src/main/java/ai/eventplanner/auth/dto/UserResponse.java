package ai.eventplanner.auth.dto;

import ai.eventplanner.common.domain.enums.UserType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class UserResponse {
    UUID id;
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
