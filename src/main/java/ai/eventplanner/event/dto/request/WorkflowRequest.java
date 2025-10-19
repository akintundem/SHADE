package ai.eventplanner.event.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for workflow orchestration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for event planning workflow orchestration")
public class WorkflowRequest {

    @NotNull(message = "Event ID is required")
    @Schema(description = "Unique identifier of the event", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID eventId;

    @NotBlank(message = "Event name is required")
    @Size(min = 2, max = 100, message = "Event name must be between 2 and 100 characters")
    @Schema(description = "Name of the event", example = "Annual Company Conference", required = true)
    private String eventName;

    @NotBlank(message = "Event type is required")
    @Pattern(regexp = "^(CORPORATE_EVENT|WEDDING|CONFERENCE|BIRTHDAY_PARTY|ANNIVERSARY|GRADUATION|RETIREMENT|HOLIDAY_PARTY|FUNDRAISER|PRODUCT_LAUNCH|NETWORKING_EVENT|TEAM_BUILDING|AWARDS_CEREMONY|GALA|SEMINAR|WORKSHOP|TRADE_SHOW|EXHIBITION|CONCERT|FESTIVAL|SPORTS_EVENT|CHARITY_EVENT|MILESTONE_CELEBRATION|CUSTOM)$", 
             message = "Invalid event type")
    @Schema(description = "Type of the event", example = "CONFERENCE", required = true)
    private String eventType;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @Schema(description = "Date and time of the event", example = "2024-06-15T09:00:00", required = true)
    private LocalDateTime date;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    @Schema(description = "Location of the event", example = "San Francisco, CA", required = true)
    private String location;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "Guest count must be at least 1")
    @Max(value = 10000, message = "Guest count cannot exceed 10,000")
    @Schema(description = "Expected number of guests", example = "200", required = true, minimum = "1", maximum = "10000")
    private Integer guestCount;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.0", message = "Budget must be non-negative")
    @DecimalMax(value = "1000000.0", message = "Budget cannot exceed $1,000,000")
    @Schema(description = "Event budget in USD", example = "50000.0", required = true, minimum = "0.0", maximum = "1000000.0")
    private Double budget;

    @Schema(description = "Event preferences and requirements")
    private List<String> preferences;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Event description", example = "Annual technology conference focusing on AI and innovation")
    private String description;

    @Schema(description = "Event theme or style", example = "Professional and modern")
    private String theme;

    @Schema(description = "Special requirements or constraints")
    private List<String> requirements;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Whether to include parallel processing", example = "true")
    private Boolean parallelProcessing = true;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    @Schema(description = "Workflow timeout in seconds", example = "60", minimum = "1", maximum = "300")
    private Integer timeoutSeconds = 60;
}
