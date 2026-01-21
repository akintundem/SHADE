package eventplanner.features.social.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "User profile response with follow status")
public class UserProfileResponse {
    private UUID id;
    private String name;
    private String email;
    private String profilePictureUrl;

    @Schema(description = "Whether the current user is following this user")
    private Boolean isFollowing;

    @Schema(description = "Whether this user is following the current user")
    private Boolean isFollowedBy;

    @Schema(description = "Whether this is a mutual follow relationship")
    private Boolean isMutual;
}
