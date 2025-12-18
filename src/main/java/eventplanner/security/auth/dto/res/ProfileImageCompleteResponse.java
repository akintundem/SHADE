package eventplanner.security.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@Schema(description = "Result of completing a profile image upload")
public class ProfileImageCompleteResponse {
    String profilePictureUrl;
    LocalDateTime updatedAt;
}


