package eventplanner.features.social.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "User follow statistics")
public class FollowStatsResponse {
    @Schema(description = "The user ID")
    private UUID userId;

    @Schema(description = "Number of users this user is following")
    private Long followingCount;

    @Schema(description = "Number of followers this user has")
    private Long followersCount;
}
