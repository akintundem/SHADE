package ai.eventplanner.timeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimelineItemCreateRequest {
    @NotNull
    private UUID eventId;

    @NotBlank
    private String title;

    private String description;

    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    private UUID assignedTo;

    private UUID[] dependencies;

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public UUID[] getDependencies() { return dependencies; }
    public void setDependencies(UUID[] dependencies) { this.dependencies = dependencies; }
}

