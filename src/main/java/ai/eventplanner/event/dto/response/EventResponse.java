package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for event information
 */
@Schema(description = "Event information response")
public class EventResponse {

    @Schema(description = "Unique identifier of the event", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Name of the event", example = "Annual Company Conference")
    private String name;

    @Schema(description = "Type of the event", example = "CONFERENCE")
    private String type;

    @Schema(description = "Date and time of the event", example = "2024-06-15T09:00:00")
    private LocalDateTime date;

    @Schema(description = "Location of the event", example = "San Francisco Convention Center")
    private String location;

    @Schema(description = "Current status of the event", example = "planning")
    private String status;

    @Schema(description = "ID of the event organizer", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID organizerId;

    @Schema(description = "Expected number of guests", example = "200")
    private Integer guestCount;

    @Schema(description = "Event budget in USD", example = "50000.0")
    private Double budget;

    @Schema(description = "Event description", example = "Annual technology conference focusing on AI and innovation")
    private String description;

    @Schema(description = "When the event was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "When the event was last updated", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;

    // Constructors
    public EventResponse() {}

    public EventResponse(UUID id, String name, String type, LocalDateTime date, String location, String status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.date = date;
        this.location = location;
        this.status = status;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}