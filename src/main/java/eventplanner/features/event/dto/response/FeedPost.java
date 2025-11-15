package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A single post in the event feed")
@Getter
@Setter
public class FeedPost {
    
    @Schema(description = "Post ID")
    private UUID id;
    
    @Schema(description = "Post type: VIDEO, IMAGE, TEXT")
    private String type;
    
    @Schema(description = "Post content/text")
    private String content;
    
    @Schema(description = "Media URL (for videos/images)")
    private String mediaUrl;
    
    @Schema(description = "Thumbnail URL (for videos)")
    private String thumbnailUrl;
    
    @Schema(description = "Author name")
    private String authorName;
    
    @Schema(description = "Author avatar URL")
    private String authorAvatarUrl;
    
    @Schema(description = "When the post was created")
    private LocalDateTime postedAt;
    
    @Schema(description = "Number of likes")
    private Integer likes;
    
    @Schema(description = "Number of comments")
    private Integer comments;
}

