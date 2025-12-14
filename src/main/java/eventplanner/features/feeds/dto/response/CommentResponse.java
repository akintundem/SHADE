package eventplanner.features.feeds.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Comment response")
public class CommentResponse {
    private UUID id;
    private UUID postId;
    private String content;
    
    @Schema(description = "User ID who created the comment")
    private UUID userId;
    
    @Schema(description = "Author name")
    private String authorName;
    
    @Schema(description = "Author avatar URL")
    private String authorAvatarUrl;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
