package ai.eventplanner.timeline.dto;

import ai.eventplanner.timeline.entity.TimelineItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TimelineItemCreateRequest {
    @NotNull
    private UUID eventId;

    @NotBlank
    private String title;

    private String description;

    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    private TimelineItem.ItemType itemType;

    private String priority;

    private String location;

    private UUID assignedTo;

    private List<UUID> dependencies;

    private Integer setupTimeMinutes;

    private Integer teardownTimeMinutes;

    private String resourcesRequired;

    private String notes;

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

    public TimelineItem.ItemType getItemType() { return itemType; }
    public void setItemType(TimelineItem.ItemType itemType) { this.itemType = itemType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public List<UUID> getDependencies() { return dependencies; }
    public void setDependencies(List<UUID> dependencies) { this.dependencies = dependencies; }

    public Integer getSetupTimeMinutes() { return setupTimeMinutes; }
    public void setSetupTimeMinutes(Integer setupTimeMinutes) { this.setupTimeMinutes = setupTimeMinutes; }

    public Integer getTeardownTimeMinutes() { return teardownTimeMinutes; }
    public void setTeardownTimeMinutes(Integer teardownTimeMinutes) { this.teardownTimeMinutes = teardownTimeMinutes; }

    public String getResourcesRequired() { return resourcesRequired; }
    public void setResourcesRequired(String resourcesRequired) { this.resourcesRequired = resourcesRequired; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

