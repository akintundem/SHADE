package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for event sharing
 */
@Schema(description = "Event sharing response")
@Getter
@Setter
public class EventShareResponse {

    @Schema(description = "Share ID")
    private UUID shareId;

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Sharing channel")
    private String channel;

    @Schema(description = "Number of recipients")
    private Integer recipientCount;

    @Schema(description = "Share status")
    private String status;

    @Schema(description = "Share link (if applicable)")
    private String shareLink;

    @Schema(description = "Custom message sent")
    private String message;

    @Schema(description = "Whether event details were included")
    private Boolean includeEventDetails;

    @Schema(description = "Whether QR code was included")
    private Boolean includeQRCode;

    @Schema(description = "Expiration date")
    private LocalDateTime expirationDate;

    @Schema(description = "Share creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "List of successful recipients")
    private List<String> successfulRecipients;

    @Schema(description = "List of failed recipients")
    private List<String> failedRecipients;
}
