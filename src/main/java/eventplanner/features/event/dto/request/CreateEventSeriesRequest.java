package eventplanner.features.event.dto.request;

import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventType;
import eventplanner.features.event.enums.RecurrenceEndType;
import eventplanner.features.event.enums.RecurrencePattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for creating a new event series.
 */
@Schema(description = "Request to create a new recurring event series")
@Getter
@Setter
public class CreateEventSeriesRequest {

    // ==================== SERIES INFO ====================

    @NotBlank(message = "Series name is required")
    @Size(max = 255, message = "Series name must not exceed 255 characters")
    @Schema(description = "Name of the event series", example = "Weekly Team Standup", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 10000, message = "Description is too long")
    @Schema(description = "Description of the series")
    private String description;

    // ==================== RECURRENCE SETTINGS ====================

    @NotNull(message = "Recurrence pattern is required")
    @Schema(description = "How often the event repeats", example = "WEEKLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private RecurrencePattern recurrencePattern;

    @Min(value = 1, message = "Recurrence interval must be at least 1")
    @Schema(description = "Interval for recurrence (e.g., every 2 weeks)", example = "1", defaultValue = "1")
    private Integer recurrenceInterval = 1;

    @NotNull(message = "Recurrence end type is required")
    @Schema(description = "How the series ends", example = "BY_OCCURRENCES", requiredMode = Schema.RequiredMode.REQUIRED)
    private RecurrenceEndType recurrenceEndType;

    @NotNull(message = "Series start date is required")
    @Schema(description = "Start date of the first occurrence", example = "2024-06-15T09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime seriesStartDate;

    @Schema(description = "End date of the series (if recurrenceEndType = BY_DATE)", example = "2024-12-31T23:59:59")
    private LocalDateTime seriesEndDate;

    @Min(value = 1, message = "Max occurrences must be at least 1")
    @Schema(description = "Maximum number of occurrences (if recurrenceEndType = BY_OCCURRENCES)", example = "52")
    private Integer maxOccurrences;

    // ==================== WEEKLY RECURRENCE ====================

    @Schema(description = "Days of week for WEEKLY recurrence", example = "[\"MONDAY\", \"WEDNESDAY\", \"FRIDAY\"]")
    private List<DayOfWeek> daysOfWeek;

    // ==================== MONTHLY RECURRENCE ====================

    @Schema(description = "Day of month for MONTHLY recurrence (1-31)", example = "15")
    private Integer dayOfMonth;

    @Schema(description = "Week of month for MONTHLY recurrence (1-5, where 5 = last week)", example = "2")
    private Integer weekOfMonth;

    @Schema(description = "Day of week for nth weekday MONTHLY recurrence", example = "TUESDAY")
    private DayOfWeek dayOfWeekForMonthly;

    // ==================== EVENT DEFAULTS ====================

    @Schema(description = "Default duration of each event in minutes", example = "60")
    private Integer defaultDurationMinutes;

    @Schema(description = "Default start time for events", example = "09:00:00")
    private LocalTime defaultStartTime;

    @Schema(description = "Timezone for the series", example = "America/New_York", defaultValue = "UTC")
    private String timezone = "UTC";

    // ==================== EVENT TEMPLATE ====================

    @Schema(description = "Event type for generated events", example = "MEETING")
    private EventType eventType;

    @Schema(description = "Access type for generated events", example = "OPEN")
    private EventAccessType accessType;

    @Schema(description = "Capacity for generated events", example = "50")
    private Integer capacity;

    @Schema(description = "Whether generated events are public", example = "true")
    private Boolean isPublic = true;

    @Schema(description = "Whether generated events require approval", example = "false")
    private Boolean requiresApproval = false;

    @Schema(description = "Venue information for generated events")
    private eventplanner.features.event.dto.VenueDTO venue;

    // ==================== GENERATION SETTINGS ====================

    @Schema(description = "Whether to auto-generate future occurrences", example = "true", defaultValue = "true")
    private Boolean autoGenerate = true;

    @Schema(description = "How many days ahead to auto-generate occurrences", example = "90", defaultValue = "90")
    private Integer autoGenerateDaysAhead = 90;

    @Schema(description = "Number of initial occurrences to generate immediately", example = "4")
    private Integer initialOccurrencesToGenerate;
}
