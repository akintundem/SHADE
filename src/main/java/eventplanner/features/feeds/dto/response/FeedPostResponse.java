package eventplanner.features.feeds.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Feed post response")
public class FeedPostResponse {
    private UUID id;
    private UUID eventId;
    private String type;
    private String content;

    @Schema(description = "Stored object id for media (if present)")
    private UUID mediaObjectId;

    @Schema(description = "Presigned download URL for media (if present)")
    private String mediaUrl;

    @Schema(description = "User ID who created the post")
    private UUID createdBy;
    
    @Schema(description = "Author name")
    private String authorName;
    
    @Schema(description = "Author avatar URL")
    private String authorAvatarUrl;
    
    @Schema(description = "Number of likes")
    private Long likeCount;
    
    @Schema(description = "Number of comments")
    private Long commentCount;
    
    @Schema(description = "Whether the current user has liked this post")
    private Boolean isLiked;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


