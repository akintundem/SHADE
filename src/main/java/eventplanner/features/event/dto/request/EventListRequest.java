package eventplanner.features.event.dto.request;

import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for listing events with pagination and filtering
 */
@Schema(description = "Request parameters for listing events")
@Getter
@Setter
public class EventListRequest {

    @Min(value = 0, message = "Page number must be >= 0")
    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 100, message = "Page size must be <= 100")
    @Schema(description = "Number of items per page", example = "20", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "Filter by event status", example = "PUBLISHED")
    private EventStatus status;

    @Schema(description = "Filter by event type", example = "WORKSHOP")
    private EventType eventType;

    @Schema(description = "Filter by visibility (true for public, false for private)", example = "true")
    private Boolean isPublic;

    @Schema(description = "Filter events starting on or after this date", example = "2024-06-01T00:00:00")
    private LocalDateTime startDateFrom;

    @Schema(description = "Filter events starting before this date", example = "2024-12-31T23:59:59")
    private LocalDateTime startDateTo;

    @Schema(description = "Filter by archived status (defaults to false)", example = "false")
    private Boolean isArchived;

    @Schema(description = "Shortcut: filter to events owned by the current user", example = "true")
    private Boolean mine;

    @Schema(description = "Timeframe filter relative to now (UTC)", example = "UPCOMING", allowableValues = {"UPCOMING", "PAST"})
    private String timeframe;

    @Schema(description = "Search term to match against event name, description, hashtag, or theme", example = "conference")
    private String search;

    @Schema(description = "Sort field", example = "startDateTime", allowableValues = {"startDateTime", "createdAt", "name", "currentAttendeeCount"})
    private String sortBy = "startDateTime";

    @Schema(description = "Sort direction", example = "ASC", allowableValues = {"ASC", "DESC"})
    private String sortDirection = "ASC";
}

