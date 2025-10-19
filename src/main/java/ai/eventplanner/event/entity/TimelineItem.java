package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event timeline and run-of-show management
 */
@Entity
@Table(name = "timeline_items")
public class TimelineItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private ItemType itemType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;
    
    @Column(name = "priority")
    private String priority = "MEDIUM";
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;
    
    @Column(name = "assigned_to_organization_id")
    private UUID assignedToOrganizationId;
    
    @Column(name = "dependencies", columnDefinition = "TEXT")
    private String dependencies; // Array of timeline_item IDs
    
    @Column(name = "setup_time_minutes")
    private Integer setupTimeMinutes;
    
    @Column(name = "teardown_time_minutes")
    private Integer teardownTimeMinutes;
    
    @Column(name = "resources_required", columnDefinition = "TEXT")
    private String resourcesRequired;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public TimelineItem() {}
    
    public TimelineItem(Event event, String title, LocalDateTime scheduledAt, Integer durationMinutes) {
        this.event = event;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.endTime = scheduledAt.plusMinutes(durationMinutes);
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    
    public UUID getAssignedToOrganizationId() { return assignedToOrganizationId; }
    public void setAssignedToOrganizationId(UUID assignedToOrganizationId) { this.assignedToOrganizationId = assignedToOrganizationId; }
    
    public String getDependencies() { return dependencies; }
    public void setDependencies(String dependencies) { this.dependencies = dependencies; }
    
    public Integer getSetupTimeMinutes() { return setupTimeMinutes; }
    public void setSetupTimeMinutes(Integer setupTimeMinutes) { this.setupTimeMinutes = setupTimeMinutes; }
    
    public Integer getTeardownTimeMinutes() { return teardownTimeMinutes; }
    public void setTeardownTimeMinutes(Integer teardownTimeMinutes) { this.teardownTimeMinutes = teardownTimeMinutes; }
    
    public String getResourcesRequired() { return resourcesRequired; }
    public void setResourcesRequired(String resourcesRequired) { this.resourcesRequired = resourcesRequired; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum ItemType {
        SETUP,
        REGISTRATION,
        WELCOME,
        PRESENTATION,
        BREAK,
        NETWORKING,
        MEAL,
        ENTERTAINMENT,
        AWARDS,
        CLOSING,
        TEARDOWN,
        OTHER
    }
    
    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        DELAYED
    }
}
