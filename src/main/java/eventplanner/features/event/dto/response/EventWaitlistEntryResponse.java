package eventplanner.features.event.dto.response;

import eventplanner.features.event.entity.EventWaitlistEntry;
import eventplanner.features.event.enums.EventWaitlistStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for event waitlist entries.
 */
@Schema(description = "Event waitlist entry response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventWaitlistEntryResponse {

    @Schema(description = "Waitlist entry ID")
    private UUID id;

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Requester user ID (if authenticated user)")
    private UUID requesterId;

    @Schema(description = "Requester email")
    private String requesterEmail;

    @Schema(description = "Requester name")
    private String requesterName;

    @Schema(description = "Waitlist status")
    private EventWaitlistStatus status;

    @Schema(description = "User who promoted this entry (if promoted)")
    private UUID promotedById;

    @Schema(description = "When this entry was promoted")
    private LocalDateTime promotedAt;

    @Schema(description = "When this entry was cancelled")
    private LocalDateTime cancelledAt;

    @Schema(description = "When this entry was created")
    private LocalDateTime createdAt;

    @Schema(description = "When this entry was last updated")
    private LocalDateTime updatedAt;

    public static EventWaitlistEntryResponse from(EventWaitlistEntry entry) {
        if (entry == null) {
            return null;
        }
        return EventWaitlistEntryResponse.builder()
                .id(entry.getId())
                .eventId(entry.getEventId())
                .requesterId(entry.getRequesterId())
                .requesterEmail(entry.getRequesterEmail())
                .requesterName(entry.getRequesterName())
                .status(entry.getStatus())
                .promotedById(entry.getPromotedBy() != null ? entry.getPromotedBy().getId() : null)
                .promotedAt(entry.getPromotedAt())
                .cancelledAt(entry.getCancelledAt())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
