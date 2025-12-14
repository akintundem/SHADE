package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Create an event post (text/image/video)")
public class EventPostCreateRequest {

    @NotBlank
    @Schema(description = "Post type", example = "TEXT")
    private String type;

    @Schema(description = "Text content (for TEXT posts or caption)", example = "Hello world")
    private String content;

    @Schema(description = "Optional stored media object id (for IMAGE/VIDEO posts)")
    private UUID mediaObjectId;
}


