package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.enums.RecurrenceEndType;
import eventplanner.features.event.enums.RecurrencePattern;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a series of recurring events.
 * The series defines the recurrence pattern and generates individual event occurrences.
 */
@Entity
@Table(name = "event_series", indexes = {
    @Index(name = "idx_event_series_owner_id", columnList = "owner_id"),
    @Index(name = "idx_event_series_is_active", columnList = "is_active"),
    @Index(name = "idx_event_series_recurrence_pattern", columnList = "recurrence_pattern")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventSeries extends BaseEntity {

    /**
     * Name of the event series (e.g., "Weekly Team Standup", "Monthly Book Club")
     */
    @Column(nullable = false)
    private String name;

    /**
     * Description of the series.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The owner of this event series.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    // ==================== RECURRENCE SETTINGS ====================

    /**
     * The recurrence pattern (DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_pattern", nullable = false)
    private RecurrencePattern recurrencePattern;

    /**
     * Interval for recurrence (e.g., every 2 weeks, every 3 months).
     * Default is 1.
     */
    @Column(name = "recurrence_interval", nullable = false)
    private Integer recurrenceInterval = 1;

    /**
     * How the series ends (BY_DATE, BY_OCCURRENCES, NEVER).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_end_type", nullable = false)
    private RecurrenceEndType recurrenceEndType = RecurrenceEndType.NEVER;

    /**
     * Start date of the series (first occurrence).
     */
    @Column(name = "series_start_date", nullable = false)
    private LocalDateTime seriesStartDate;

    /**
     * End date of the series (if recurrenceEndType = BY_DATE).
     */
    @Column(name = "series_end_date")
    private LocalDateTime seriesEndDate;

    /**
     * Maximum number of occurrences (if recurrenceEndType = BY_OCCURRENCES).
     */
    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    /**
     * Number of occurrences generated so far.
     */
    @Column(name = "occurrences_generated", nullable = false)
    private Integer occurrencesGenerated = 0;

    // ==================== WEEKLY RECURRENCE SETTINGS ====================

    /**
     * Days of week for WEEKLY recurrence (stored as comma-separated values).
     * Example: "MONDAY,WEDNESDAY,FRIDAY"
     */
    @Column(name = "days_of_week")
    private String daysOfWeek;

    // ==================== MONTHLY RECURRENCE SETTINGS ====================

    /**
     * Day of month for MONTHLY recurrence (1-31).
     * If null, uses nth weekday pattern instead.
     */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /**
     * Week of month for MONTHLY recurrence (1-5, where 5 = last week).
     * Used with dayOfWeekForMonthly.
     */
    @Column(name = "week_of_month")
    private Integer weekOfMonth;

    /**
     * Day of week for nth weekday MONTHLY recurrence.
     * Used with weekOfMonth.
     */
    @Column(name = "day_of_week_for_monthly")
    private String dayOfWeekForMonthly;

    // ==================== EVENT TEMPLATE SETTINGS ====================
    // These settings are copied to each generated occurrence

    /**
     * Default duration of each event in minutes.
     */
    @Column(name = "default_duration_minutes")
    private Integer defaultDurationMinutes;

    /**
     * Default start time for events (combined with occurrence date).
     */
    @Column(name = "default_start_time")
    private LocalTime defaultStartTime;

    /**
     * Reference to the master/template event that defines defaults.
     * If null, events are created with series-level defaults.
     */
    @Column(name = "master_event_id")
    private UUID masterEventId;

    // ==================== TIMEZONE ====================

    /**
     * Timezone for the series (e.g., "America/New_York", "UTC").
     * Used for calculating occurrence dates.
     */
    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";

    // ==================== STATUS ====================

    /**
     * Whether the series is active. Inactive series don't generate new occurrences.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Whether to auto-generate future occurrences.
     * If false, occurrences must be manually generated.
     */
    @Column(name = "auto_generate", nullable = false)
    private Boolean autoGenerate = true;

    /**
     * How many days ahead to auto-generate occurrences.
     */
    @Column(name = "auto_generate_days_ahead", nullable = false)
    private Integer autoGenerateDaysAhead = 90;

    // ==================== RELATIONSHIPS ====================

    /**
     * Events in this series.
     */
    @OneToMany(mappedBy = "parentSeries", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    // ==================== HELPER METHODS ====================

    /**
     * Parse days of week from stored string.
     */
    public List<DayOfWeek> getDaysOfWeekList() {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return List.of();
        }
        List<DayOfWeek> days = new ArrayList<>();
        for (String day : daysOfWeek.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(day.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Skip invalid values
            }
        }
        return days;
    }

    /**
     * Set days of week from a list.
     */
    public void setDaysOfWeekList(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            this.daysOfWeek = null;
            return;
        }
        this.daysOfWeek = days.stream()
                .map(DayOfWeek::name)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    /**
     * Check if series can generate more occurrences.
     */
    public boolean canGenerateMore() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        
        switch (recurrenceEndType) {
            case BY_DATE:
                return seriesEndDate == null || LocalDateTime.now().isBefore(seriesEndDate);
            case BY_OCCURRENCES:
                return maxOccurrences == null || occurrencesGenerated < maxOccurrences;
            case NEVER:
            default:
                return true;
        }
    }

    /**
     * Increment the generated occurrences count.
     */
    public void incrementOccurrencesGenerated() {
        this.occurrencesGenerated = (this.occurrencesGenerated != null ? this.occurrencesGenerated : 0) + 1;
    }
}
