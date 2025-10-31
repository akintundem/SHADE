package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
@Schema(description = "Event cover image response")
public class EventCoverImageResponse {
    @Schema(description = "Event identifier")
    UUID eventId;

    @Schema(description = "Cover image URL")
    String coverImageUrl;

    @Schema(description = "Timestamp of update")
    LocalDateTime updatedAt;
}
