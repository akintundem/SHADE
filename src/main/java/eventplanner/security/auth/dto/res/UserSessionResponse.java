package eventplanner.security.auth.dto.res;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class UserSessionResponse {
    UUID id;
    String deviceId;
    String clientId;
    String ipAddress;
    LocalDateTime createdAt;
    LocalDateTime lastSeenAt;
    LocalDateTime expiresAt;
    boolean active;
}
