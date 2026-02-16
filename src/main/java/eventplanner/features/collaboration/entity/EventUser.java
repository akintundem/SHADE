package eventplanner.features.collaboration.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.enums.EventUserType;
import eventplanner.features.collaboration.enums.RegistrationStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventUser extends BaseEntity {

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

    @Column(name = "is_volunteer", nullable = false)
    private Boolean isVolunteer = false;

    @Column(name = "volunteer_hours")
    private Integer volunteerHours;

    /**
     * Optional granular permissions for the collaborator.
     */
    @OneToMany(mappedBy = "eventUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventUserPermission> permissions = new ArrayList<>();
}
