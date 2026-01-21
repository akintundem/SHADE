package eventplanner.features.social.service;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.social.dto.request.EventSubscriptionRequest;
import eventplanner.features.social.dto.response.EventSubscriptionResponse;
import eventplanner.features.social.dto.response.UserProfileResponse;
import eventplanner.features.social.entity.EventSubscription;
import eventplanner.features.social.repository.EventSubscriptionRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class EventSubscriptionService {

    private final EventSubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;

    public EventSubscriptionService(EventSubscriptionRepository subscriptionRepository,
                                   EventRepository eventRepository,
                                   UserAccountRepository userAccountRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Subscribe to an event
     */
    public EventSubscriptionResponse subscribeToEvent(UUID eventId, UserPrincipal principal, EventSubscriptionRequest request) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = principal.getId();

        // Check if event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if already subscribed
        EventSubscription existingSubscription = subscriptionRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElse(null);

        EventSubscription.SubscriptionType type = request != null && request.getSubscriptionType() != null
                ? EventSubscription.SubscriptionType.valueOf(request.getSubscriptionType())
                : EventSubscription.SubscriptionType.BOTH;

        if (existingSubscription != null) {
            // Update existing subscription
            existingSubscription.setSubscriptionType(type);
            EventSubscription updated = subscriptionRepository.save(existingSubscription);
            log.info("User {} updated subscription to event {}", userId, eventId);
            return toResponse(updated);
        }

        // Create new subscription
        EventSubscription subscription = new EventSubscription();
        subscription.setUser(user);
        subscription.setEvent(event);
        subscription.setSubscriptionType(type);

        EventSubscription saved = subscriptionRepository.save(subscription);
        log.info("User {} subscribed to event {} with type {}", userId, eventId, type);

        return toResponse(saved);
    }

    /**
     * Unsubscribe from an event
     */
    public void unsubscribeFromEvent(UUID eventId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = principal.getId();

        subscriptionRepository.findByUserIdAndEventId(userId, eventId)
                .ifPresent(subscription -> {
                    subscriptionRepository.delete(subscription);
                    log.info("User {} unsubscribed from event {}", userId, eventId);
                });
    }

    /**
     * Check if user is subscribed to an event
     */
    public boolean isSubscribed(UUID eventId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return false;
        }
        return subscriptionRepository.existsByUserIdAndEventId(principal.getId(), eventId);
    }

    /**
     * Get user's subscribed event IDs
     */
    public List<UUID> getSubscribedEventIds(UUID userId) {
        return subscriptionRepository.findEventIdsByUserId(userId);
    }

    /**
     * Get user's subscribed events
     */
    public Page<EventSubscription> getSubscribedEvents(UUID userId, Pageable pageable) {
        return subscriptionRepository.findByUserId(userId, pageable);
    }

    /**
     * Get event subscribers
     */
    public Page<UserProfileResponse> getEventSubscribers(UUID eventId, Pageable pageable) {
        Page<EventSubscription> subscriptions = subscriptionRepository.findByEventId(eventId, pageable);
        return subscriptions.map(this::toUserProfileResponse);
    }

    /**
     * Get subscriber count for an event
     */
    public long getSubscriberCount(UUID eventId) {
        return subscriptionRepository.countByEventId(eventId);
    }

    private EventSubscriptionResponse toResponse(EventSubscription subscription) {
        EventSubscriptionResponse response = new EventSubscriptionResponse();
        response.setId(subscription.getId());
        response.setUserId(subscription.getUser().getId());
        response.setEventId(subscription.getEvent().getId());
        response.setSubscriptionType(subscription.getSubscriptionType().name());
        response.setCreatedAt(subscription.getCreatedAt());
        return response;
    }

    private UserProfileResponse toUserProfileResponse(EventSubscription subscription) {
        UserAccount user = subscription.getUser();
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        return response;
    }
}
