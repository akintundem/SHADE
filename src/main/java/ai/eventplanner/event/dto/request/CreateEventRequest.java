package ai.eventplanner.event.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new event
 */
@Schema(description = "Request to create a new event")
public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(min = 2, max = 100, message = "Event name must be between 2 and 100 characters")
    @Schema(description = "Name of the event", example = "Annual Company Conference", required = true)
    private String name;

    @NotBlank(message = "Event type is required")
    @Pattern(regexp = "^(CORPORATE_EVENT|WEDDING|CONFERENCE|BIRTHDAY_PARTY|ANNIVERSARY|GRADUATION|RETIREMENT|HOLIDAY_PARTY|FUNDRAISER|PRODUCT_LAUNCH|NETWORKING_EVENT|TEAM_BUILDING|AWARDS_CEREMONY|GALA|SEMINAR|WORKSHOP|TRADE_SHOW|EXHIBITION|CONCERT|FESTIVAL|SPORTS_EVENT|CHARITY_EVENT|MILESTONE_CELEBRATION|CUSTOM)$", 
             message = "Invalid event type")
    @Schema(description = "Type of the event", example = "CONFERENCE", required = true)
    private String type;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @Schema(description = "Date and time of the event", example = "2024-06-15T09:00:00", required = true)
    private LocalDateTime date;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    @Schema(description = "Location of the event", example = "San Francisco Convention Center", required = true)
    private String location;

    @NotNull(message = "Organizer ID is required")
    @Schema(description = "ID of the event organizer", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID organizerId;

    @Min(value = 1, message = "Guest count must be at least 1")
    @Max(value = 10000, message = "Guest count cannot exceed 10,000")
    @Schema(description = "Expected number of guests", example = "200", minimum = "1", maximum = "10000")
    private Integer guestCount;

    @DecimalMin(value = "0.0", message = "Budget must be non-negative")
    @DecimalMax(value = "1000000.0", message = "Budget cannot exceed $1,000,000")
    @Schema(description = "Event budget in USD", example = "50000.0", minimum = "0.0", maximum = "1000000.0")
    private Double budget;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Event description", example = "Annual technology conference focusing on AI and innovation")
    private String description;

    @Schema(description = "Event preferences and requirements")
    private List<String> preferences;

    @Schema(description = "Additional notes or special requirements")
    private String notes;

    // Constructors
    public CreateEventRequest() {}

    public CreateEventRequest(String name, String type, LocalDateTime date, String location, UUID organizerId) {
        this.name = name;
        this.type = type;
        this.date = date;
        this.location = location;
        this.organizerId = organizerId;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(UUID organizerId) {
        this.organizerId = organizerId;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}