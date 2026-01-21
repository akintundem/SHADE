package eventplanner.features.social.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Follow status between two users")
public class FollowStatusResponse {
    @Schema(description = "The user ID being checked")
    private UUID userId;

    @Schema(description = "Whether the current user is following this user")
    private Boolean isFollowing;

    @Schema(description = "Whether this user is following the current user")
    private Boolean isFollowedBy;

    @Schema(description = "Whether this is a mutual follow relationship")
    private Boolean isMutual;
}
