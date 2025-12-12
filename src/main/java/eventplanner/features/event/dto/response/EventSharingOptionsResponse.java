package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.List;

/**
 * Response DTO for event sharing options
 */
@Schema(description = "Event sharing options response")
@Getter
@Setter
public class EventSharingOptionsResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Available sharing channels")
    private List<String> availableChannels;

    @Schema(description = "Share link for the event")
    private String shareLink;

    @Schema(description = "Whether the event is public")
    private Boolean isPublic;

    @Schema(description = "Social media sharing options")
    private List<String> socialMediaOptions;

    @Schema(description = "Email sharing options")
    private List<String> emailOptions;
}
