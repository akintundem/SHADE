package ai.eventplanner.roles.entity;

import ai.eventplanner.event.entity.Event;
import ai.eventplanner.user.entity.EventUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event team roles and responsibilities.
 */
@Entity
@Table(name = "event_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private Boolean isLead = false;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RoleType {
        EVENT_LEAD,
        LOGISTICS,
        MARKETING,
        VENDOR_LIAISON,
        VOLUNTEER_COORD,
        TECH_COORD,
        SPONSOR_RELATIONS,
        SECURITY,
        CATERING_COORD,
        AV_COORD,
        DECOR_COORD,
        REGISTRATION,
        COMMUNICATIONS,
        FINANCE,
        OTHER
    }
}
