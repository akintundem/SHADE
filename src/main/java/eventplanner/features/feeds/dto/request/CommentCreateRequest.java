package eventplanner.features.feeds.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to create a comment on a post")
public class CommentCreateRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 2000, message = "Comment content must not exceed 2000 characters")
    @Schema(description = "Comment content", example = "Great post!")
    private String content;
}
