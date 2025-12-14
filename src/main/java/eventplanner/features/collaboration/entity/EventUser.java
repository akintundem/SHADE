package eventplanner.features.collaboration.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.RegistrationStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    /**
     * Many-to-one relationship with the event.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Many-to-one relationship with the user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private EventUserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status")
    private RegistrationStatus registrationStatus;

    @Column(name = "registration_date")
    private java.time.LocalDateTime registrationDate;

    @Column(name = "check_in_time")
    private java.time.LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private java.time.LocalDateTime checkOutTime;

    @Column(name = "special_requirements")
    private String specialRequirements;

    @Column(name = "dietary_restrictions")
    private String dietaryRestrictions;

    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_volunteer", nullable = false)
    private Boolean isVolunteer = false;

    @Column(name = "volunteer_hours")
    private Integer volunteerHours;
}