package eventplanner.features.feeds.dto.response;

import eventplanner.features.feeds.entity.PostComment;
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

    /**
     * Create a CommentResponse from a PostComment entity.
     */
    public static CommentResponse from(PostComment comment) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setPostId(comment.getPost() != null ? comment.getPost().getId() : null);
        resp.setContent(comment.getContent());
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setUpdatedAt(comment.getUpdatedAt());

        if (comment.getUser() != null) {
            resp.setUserId(comment.getUser().getId());
            resp.setAuthorName(comment.getUser().getName());
            resp.setAuthorAvatarUrl(comment.getUser().getProfilePictureUrl());
        }

        return resp;
    }
}
