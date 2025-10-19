package ai.eventplanner.event.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * EventUsers are regular event creators and attendees
 * Organizations are service providers (vendors, venues, etc.)
 */
@Entity
@Table(name = "event_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventUser extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    private String phone;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences; // AI memory: themes, vendors, budgets
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType; // CREATOR, ATTENDEE, BOTH
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Relationships
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Event> organizedEvents;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventAttendance> eventAttendances;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventRole> eventRoles;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationHistory> conversations;
    
    public EventUser(String email, String name, UserType userType) {
        this.email = email;
        this.name = name;
        this.userType = userType;
    }
}
