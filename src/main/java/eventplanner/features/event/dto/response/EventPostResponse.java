package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Event post response")
public class EventPostResponse {

    private UUID id;
    private UUID eventId;
    private String type;
    private String content;

    @Schema(description = "Stored object id for media")
    private UUID mediaObjectId;

    @Schema(description = "Presigned download URL for media (if present)")
    private String mediaUrl;

    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


