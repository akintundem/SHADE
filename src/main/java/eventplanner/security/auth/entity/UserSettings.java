package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.LanguagePreference;
import eventplanner.common.domain.enums.ThemePreference;
import eventplanner.common.domain.enums.VisibilityLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "user_settings",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_settings_user", columnNames = "user_id")
)
@Getter
@Setter
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, unique = true)
    private UserAccount user;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 30)
    private VisibilityLevel profileVisibility = VisibilityLevel.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_participation_visibility", nullable = false, length = 30)
    private VisibilityLevel eventParticipationVisibility = VisibilityLevel.PUBLIC;

    @Column(name = "search_visibility", nullable = false)
    private Boolean searchVisibility = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 10)
    private LanguagePreference preferredLanguage = LanguagePreference.EN;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false, length = 20)
    private ThemePreference themePreference = ThemePreference.SYSTEM;

    @Column(name = "email_notifications_enabled", nullable = false)
    private Boolean emailNotificationsEnabled = Boolean.TRUE;

    @Column(name = "push_notifications_enabled", nullable = false)
    private Boolean pushNotificationsEnabled = Boolean.TRUE;

    @Column(name = "event_invitations_enabled", nullable = false)
    private Boolean eventInvitationsEnabled = Boolean.TRUE;

    @Column(name = "event_updates_enabled", nullable = false)
    private Boolean eventUpdatesEnabled = Boolean.TRUE;

    @Column(name = "event_reminders_enabled", nullable = false)
    private Boolean eventRemindersEnabled = Boolean.TRUE;

    @Column(name = "rsvp_notifications_enabled", nullable = false)
    private Boolean rsvpNotificationsEnabled = Boolean.TRUE;

    @Column(name = "comment_notifications_enabled", nullable = false)
    private Boolean commentNotificationsEnabled = Boolean.TRUE;

    @Column(name = "collaboration_requests_enabled", nullable = false)
    private Boolean collaborationRequestsEnabled = Boolean.TRUE;

    @Column(name = "weekly_digest_enabled", nullable = false)
    private Boolean weeklyDigestEnabled = Boolean.FALSE;

    @Column(name = "activity_feed_notifications_enabled", nullable = false)
    private Boolean activityFeedNotificationsEnabled = Boolean.TRUE;

    @Column(name = "auto_accept_invitations", nullable = false)
    private Boolean autoAcceptInvitations = Boolean.FALSE;

    @Column(name = "export_event_data_enabled", nullable = false)
    private Boolean exportEventDataEnabled = Boolean.FALSE;

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = Boolean.FALSE;

    @Column(name = "reminder_timing_minutes", nullable = false)
    private Integer reminderTimingMinutes = 30;

    @Column(name = "show_in_event_directory", nullable = false)
    private Boolean showInEventDirectory = Boolean.TRUE;

    @Column(name = "sms_notifications_enabled", nullable = false)
    private Boolean smsNotificationsEnabled = Boolean.FALSE;

    public static UserSettings createDefault(UserAccount user) {
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        return settings;
    }
}
