package eventplanner.features.social.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_subscriptions_user_event", columnNames = {"user_id", "event_id"})
    },
    indexes = {
        @Index(name = "idx_event_subscriptions_user", columnList = "user_id"),
        @Index(name = "idx_event_subscriptions_event", columnList = "event_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventSubscription extends BaseEntity {

    public enum SubscriptionType {
        /**
         * Follow event to see updates in timeline
         */
        FOLLOW,

        /**
         * Get push notifications for event updates
         */
        NOTIFY,

        /**
         * Both follow and get notifications
         */
        BOTH
    }

    /**
     * User who subscribed to the event
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    /**
     * Event being subscribed to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Type of subscription
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private SubscriptionType subscriptionType = SubscriptionType.BOTH;
}
