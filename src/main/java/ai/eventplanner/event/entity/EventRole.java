package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event team roles and responsibilities
 */
@Entity
@Table(name = "event_roles")
public class EventRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private EventUser user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type")
    private RoleType roleType;
    
    @Column(name = "role_title")
    private String roleTitle;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private String responsibilities;
    
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;
    
    @Column(name = "is_lead")
    private Boolean isLead = false;
    
    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;
    
    @Column(name = "status")
    private String status = "ACTIVE";
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EventRole() {}
    
    public EventRole(Event event, EventUser user, RoleType roleType) {
        this.event = event;
        this.user = user;
        this.roleType = roleType;
        this.assignedDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public EventUser getUser() { return user; }
    public void setUser(EventUser user) { this.user = user; }
    
    public RoleType getRoleType() { return roleType; }
    public void setRoleType(RoleType roleType) { this.roleType = roleType; }
    
    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }
    
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    
    public Boolean getIsLead() { return isLead; }
    public void setIsLead(Boolean isLead) { this.isLead = isLead; }
    
    public LocalDateTime getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDateTime assignedDate) { this.assignedDate = assignedDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum RoleType {
        EVENT_LEAD,         // Project manager/event lead
        LOGISTICS,          // Logistics coordinator
        MARKETING,          // Marketing coordinator
        VENDOR_LIAISON,     // Vendor relations
        VOLUNTEER_COORD,    // Volunteer coordination
        TECH_COORD,         // Technical coordinator
        SPONSOR_RELATIONS,  // Sponsor relations
        SECURITY,           // Security coordinator
        CATERING_COORD,     // Catering coordinator
        AV_COORD,           // Audio-visual coordinator
        DECOR_COORD,        // Decoration coordinator
        REGISTRATION,       // Registration coordinator
        COMMUNICATIONS,     // Communications coordinator
        FINANCE,            // Finance coordinator
        OTHER               // Other roles
    }
}
