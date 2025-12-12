package eventplanner.security.auth.dto.res;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Public User Response DTO for directory search.
 * Contains only non-sensitive information suitable for public search results.
 */
@Value
@Builder
public class PublicUserResponse {
    UUID id;
    String name;
    String profileImageUrl;
    String email;
}
