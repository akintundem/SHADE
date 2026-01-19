package eventplanner.features.ticket.service;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Shared ticketing policy guards for event lifecycle validation.
 */
@Service
public class TicketingPolicyService {

    public void ensureEventOpenForTicketing(Event event) {
        if (event.getAccessType() != EventAccessType.TICKETED) {
            throw new IllegalArgumentException("Event does not support ticket purchases");
        }
        if (event.getEventStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Event has been cancelled");
        }
        if (event.getEventStatus() == EventStatus.REGISTRATION_CLOSED) {
            throw new IllegalArgumentException("Event registration is closed");
        }
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Event has ended");
        }
        if (event.getRegistrationDeadline() != null &&
                LocalDateTime.now(ZoneOffset.UTC).isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException("Registration deadline has passed");
        }
        if (isEventPast(event)) {
            throw new IllegalArgumentException("Event has ended");
        }
    }

    private boolean isEventPast(Event event) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (event.getEndDateTime() != null) {
            return now.isAfter(event.getEndDateTime());
        }
        if (event.getStartDateTime() != null) {
            return now.isAfter(event.getStartDateTime().plusDays(1));
        }
        return false;
    }
}
