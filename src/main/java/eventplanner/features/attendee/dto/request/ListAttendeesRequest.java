package eventplanner.features.attendee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for listing and filtering attendees
 */
@Data
@Schema(description = "Request to list and filter attendees for an event")
public class ListAttendeesRequest {

    @Schema(description = "Event ID (required)")
    private UUID eventId;

    @Schema(description = "Filter by RSVP status (comma-separated): PENDING,CONFIRMED,DECLINED,TENTATIVE,NO_SHOW")
    private String status;

    @Schema(description = "Filter by check-in status: true (checked in) or false (not checked in)")
    private Boolean checkedIn;

    @Schema(description = "Search by name or email")
    private String search;

    @Schema(description = "Filter by user ID (from directory)")
    private UUID userId;

    @Schema(description = "Filter by email")
    private String email;

    @Schema(description = "Page number (0-indexed)", example = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "20")
    private Integer size = 20;

    @Schema(description = "Sort field: name, email, rsvpStatus, checkedInAt, createdAt", example = "name")
    private String sortBy = "name";

    @Schema(description = "Sort direction: ASC or DESC", example = "ASC")
    private String sortDirection = "ASC";
}
