package eventplanner.features.ticket.service;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.common.exception.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Shared ticketing policy guards for event lifecycle validation.
 */
@Service
public class TicketingPolicyService {

    private final Clock clock;

    public TicketingPolicyService(Clock clock) {
        this.clock = clock != null ? clock : java.time.Clock.systemUTC();
    }

    public void ensureEventOpenForTicketing(Event event) {
        if (event.getAccessType() != EventAccessType.TICKETED) {
            throw new BadRequestException("Event does not support ticket purchases");
        }
        if (event.getEventStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event has been cancelled");
        }
        if (event.getEventStatus() == EventStatus.REGISTRATION_CLOSED) {
            throw new BadRequestException("Event registration is closed");
        }
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            throw new BadRequestException("Event has ended");
        }
        if (event.getRegistrationDeadline() != null &&
                LocalDateTime.now(clock).isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed");
        }
        if (isEventPast(event)) {
            throw new BadRequestException("Event has ended");
        }
    }

    private boolean isEventPast(Event event) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (event.getEndDateTime() != null) {
            return now.isAfter(event.getEndDateTime());
        }
        if (event.getStartDateTime() != null) {
            return now.isAfter(event.getStartDateTime().plusDays(1));
        }
        return false;
    }
}
