package ai.eventplanner.attendee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for sending invitations to attendees
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after sending invitations to attendees")
public class SendInvitesResponse {

    @Schema(description = "Status of the invitation sending process", example = "queued")
    private String status;

    @Schema(description = "Number of invitations queued for sending", example = "5")
    private Integer queuedCount;

    @Schema(description = "Number of invitations that failed to queue", example = "0")
    private Integer failedCount;

    @Schema(description = "List of attendee IDs that were successfully queued")
    private List<UUID> queuedAttendeeIds;

    @Schema(description = "List of attendee IDs that failed to queue")
    private List<UUID> failedAttendeeIds;

    @Schema(description = "Response message", example = "Invitations queued successfully")
    private String message;

    @Schema(description = "Response timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp = LocalDateTime.now();
}
