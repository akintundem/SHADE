package eventplanner.features.event.service;

import eventplanner.common.domain.enums.EventAccessType;
import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.UserEventAccessStatus;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.dto.response.UserEventContext;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service to compute user-specific context for events.
 * This determines the user's relationship with an event and what actions they can take.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class UserEventContextService {

    private static final Set<EventUserType> COLLABORATOR_ROLES = EnumSet.of(
            EventUserType.ORGANIZER, 
            EventUserType.COORDINATOR, 
            EventUserType.ADMIN,
            EventUserType.COLLABORATOR
    );

    private final EventUserRepository eventUserRepository;
    private final TicketRepository ticketRepository;
    private final AttendeeRepository attendeeRepository;

    public UserEventContextService(EventUserRepository eventUserRepository,
                                   TicketRepository ticketRepository,
                                   AttendeeRepository attendeeRepository) {
        this.eventUserRepository = eventUserRepository;
        this.ticketRepository = ticketRepository;
        this.attendeeRepository = attendeeRepository;
    }

    /**
     * Build the user's context for an event.
     * This is the main entry point for computing user-specific event information.
     *
     * @param event The event entity (must not be null - caller should validate)
     * @param principal The authenticated user (must not be null - no context for anonymous users)
     * @return UserEventContext with all computed information, or null if event/principal is null
     */
    public UserEventContext buildContext(Event event, UserPrincipal principal) {
        // No context to build if event or principal is null
        if (event == null || principal == null || principal.getId() == null) {
            return null;
        }

        UserEventContext.UserEventContextBuilder builder = UserEventContext.builder();

        UUID userId = principal.getId();
        UUID eventId = event.getId();

        // Check ownership
        boolean isOwner = event.getOwner() != null && event.getOwner().getId().equals(userId);
        builder.isOwner(isOwner);

        // Check collaborator status
        Optional<EventUser> eventUser = eventUserRepository.findByEventIdAndUserId(eventId, userId);
        boolean isCollaborator = eventUser.isPresent() && COLLABORATOR_ROLES.contains(eventUser.get().getUserType());
        builder.isCollaborator(isCollaborator);
        if (eventUser.isPresent()) {
            builder.eventRole(eventUser.get().getUserType().name());
        }

        // Owner or collaborator always has full access
        if (isOwner) {
            return buildOwnerContext(builder, event);
        }
        if (isCollaborator) {
            return buildCollaboratorContext(builder, event, eventUser.get());
        }

        // Determine access based on event access type
        EventAccessType accessType = event.getAccessType() != null ? event.getAccessType() : EventAccessType.OPEN;

        switch (accessType) {
            case OPEN:
                return buildOpenEventContext(builder, event, userId);
            case RSVP_REQUIRED:
                return buildRsvpRequiredContext(builder, event, userId);
            case INVITE_ONLY:
                return buildInviteOnlyContext(builder, event, userId);
            case TICKETED:
                return buildTicketedEventContext(builder, event, userId, principal.getUsername());
            default:
                return buildOpenEventContext(builder, event, userId);
        }
    }

    private UserEventContext buildOwnerContext(UserEventContext.UserEventContextBuilder builder, Event event) {
        return builder
                .accessStatus(UserEventAccessStatus.OWNER)
                .hasAccess(true)
                .accessMessage("You are the owner of this event")
                .canViewFeeds(true)
                .canBuyTicket(false)
                .canRsvp(false)
                .canRespondToInvite(false)
                .canExpressInterest(false)
                .primaryAction("MANAGE_EVENT")
                .primaryActionLabel("Manage Event")
                .build();
    }

    private UserEventContext buildCollaboratorContext(UserEventContext.UserEventContextBuilder builder, Event event, EventUser eventUser) {
        return builder
                .accessStatus(UserEventAccessStatus.COLLABORATOR)
                .hasAccess(true)
                .accessMessage("You are a " + eventUser.getUserType().name().toLowerCase() + " for this event")
                .canViewFeeds(true)
                .canBuyTicket(false)
                .canRsvp(false)
                .canRespondToInvite(false)
                .canExpressInterest(false)
                .primaryAction("VIEW_FEEDS")
                .primaryActionLabel("View Event")
                .build();
    }

    private UserEventContext buildOpenEventContext(UserEventContext.UserEventContextBuilder builder, Event event, UUID userId) {
        // Check if user has RSVP'd
        Optional<Attendee> attendee = attendeeRepository.findByEventIdAndUserId(event.getId(), userId);
        boolean hasRsvp = attendee.isPresent();

        builder.hasRsvp(hasRsvp);
        if (hasRsvp) {
            Attendee att = attendee.get();
            builder.rsvpStatus(att.getRsvpStatus() != null ? att.getRsvpStatus().name() : null);
            builder.rsvpAt(att.getCreatedAt());
            builder.hasCheckedIn(att.isCheckedIn());
            builder.checkedInAt(att.getCheckedInAt());
        }

        // Open events always allow access
        builder.hasAccess(true)
                .accessMessage(hasRsvp ? "You have RSVP'd to this event" : "This is an open event")
                .accessStatus(hasRsvp ? UserEventAccessStatus.OPEN_RSVP_CONFIRMED : UserEventAccessStatus.OPEN_NOT_REGISTERED)
                .canViewFeeds(true)
                .canBuyTicket(false)
                .canRsvp(!hasRsvp && !isEventCompleted(event))
                .canRespondToInvite(false)
                .canExpressInterest(false);

        if (hasRsvp) {
            builder.primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (!isEventCompleted(event)) {
            builder.primaryAction("RSVP")
                    .primaryActionLabel("RSVP to Event");
        } else {
            builder.primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        }

        return builder.build();
    }

    private UserEventContext buildRsvpRequiredContext(UserEventContext.UserEventContextBuilder builder, Event event, UUID userId) {
        Optional<Attendee> attendee = attendeeRepository.findByEventIdAndUserId(event.getId(), userId);
        boolean hasRsvp = attendee.isPresent();
        boolean isConfirmed = hasRsvp && attendee.get().getRsvpStatus() == AttendeeStatus.CONFIRMED;
        boolean isPending = hasRsvp && attendee.get().getRsvpStatus() == AttendeeStatus.PENDING;
        boolean isDeclined = hasRsvp && attendee.get().getRsvpStatus() == AttendeeStatus.DECLINED;
        boolean isEventCompleted = isEventCompleted(event);

        builder.hasRsvp(hasRsvp);
        if (hasRsvp) {
            Attendee att = attendee.get();
            builder.rsvpStatus(att.getRsvpStatus() != null ? att.getRsvpStatus().name() : null);
            builder.rsvpAt(att.getCreatedAt());
            builder.hasCheckedIn(att.isCheckedIn());
            builder.checkedInAt(att.getCheckedInAt());
        }

        // Check if feeds are public after event
        if (isEventCompleted && Boolean.TRUE.equals(event.getFeedsPublicAfterEvent())) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.EVENT_ENDED_PUBLIC)
                    .accessMessage("Event has ended - content is now public")
                    .canViewFeeds(true)
                    .canRsvp(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (isConfirmed) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.RSVP_CONFIRMED)
                    .accessMessage("Your RSVP is confirmed")
                    .canViewFeeds(true)
                    .canRsvp(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (isPending) {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.RSVP_PENDING_APPROVAL)
                    .accessMessage("Your RSVP is pending approval")
                    .canViewFeeds(false)
                    .canRsvp(false)
                    .primaryAction("WAITING")
                    .primaryActionLabel("Awaiting Approval");
        } else if (isDeclined) {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.RSVP_DECLINED)
                    .accessMessage("Your RSVP was declined")
                    .canViewFeeds(false)
                    .canRsvp(false)
                    .primaryAction("NONE")
                    .primaryActionLabel("Access Denied");
        } else {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.RSVP_NOT_REGISTERED)
                    .accessMessage("RSVP required to access this event")
                    .canViewFeeds(false)
                    .canRsvp(!isEventCompleted)
                    .primaryAction("RSVP")
                    .primaryActionLabel("RSVP to Access");
        }

        builder.canBuyTicket(false)
                .canRespondToInvite(false)
                .canExpressInterest(!hasRsvp && !isEventCompleted);

        return builder.build();
    }

    private UserEventContext buildInviteOnlyContext(UserEventContext.UserEventContextBuilder builder, Event event, UUID userId) {
        // Check for attendee invite (simplified - you might have a separate invite table)
        Optional<Attendee> attendee = attendeeRepository.findByEventIdAndUserId(event.getId(), userId);
        boolean hasInvite = attendee.isPresent();
        boolean isAccepted = hasInvite && attendee.get().getRsvpStatus() == AttendeeStatus.CONFIRMED;
        boolean isPending = hasInvite && attendee.get().getRsvpStatus() == AttendeeStatus.PENDING;
        boolean isDeclined = hasInvite && attendee.get().getRsvpStatus() == AttendeeStatus.DECLINED;
        boolean isEventCompleted = isEventCompleted(event);

        builder.hasInvite(hasInvite);
        if (hasInvite) {
            Attendee att = attendee.get();
            builder.inviteStatus(att.getRsvpStatus() != null ? att.getRsvpStatus().name() : null);
            builder.invitedAt(att.getCreatedAt());
            builder.hasCheckedIn(att.isCheckedIn());
            builder.checkedInAt(att.getCheckedInAt());
        }

        // Check if feeds are public after event
        if (isEventCompleted && Boolean.TRUE.equals(event.getFeedsPublicAfterEvent())) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.EVENT_ENDED_PUBLIC)
                    .accessMessage("Event has ended - content is now public")
                    .canViewFeeds(true)
                    .canRespondToInvite(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (isAccepted) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.INVITE_ACCEPTED)
                    .accessMessage("You have accepted the invitation")
                    .canViewFeeds(true)
                    .canRespondToInvite(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (isPending) {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.INVITE_PENDING_RESPONSE)
                    .accessMessage("You have been invited to this event")
                    .canViewFeeds(false)
                    .canRespondToInvite(true)
                    .primaryAction("RESPOND_TO_INVITE")
                    .primaryActionLabel("Respond to Invite");
        } else if (isDeclined) {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.INVITE_DECLINED)
                    .accessMessage("You declined this invitation")
                    .canViewFeeds(false)
                    .canRespondToInvite(false)
                    .primaryAction("NONE")
                    .primaryActionLabel("Invitation Declined");
        } else {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.INVITE_NOT_INVITED)
                    .accessMessage("This is an invite-only event")
                    .canViewFeeds(false)
                    .canRespondToInvite(false)
                    .primaryAction("EXPRESS_INTEREST")
                    .primaryActionLabel("Express Interest");
        }

        builder.canBuyTicket(false)
                .canRsvp(false)
                .canExpressInterest(!hasInvite);

        return builder.build();
    }

    private UserEventContext buildTicketedEventContext(UserEventContext.UserEventContextBuilder builder, 
                                                        Event event, UUID userId, String userEmail) {
        // Find tickets for this user
        List<Ticket> userTickets = ticketRepository.findValidTicketsByUserId(event.getId(), userId);
        
        // Also check by email
        if (userTickets.isEmpty() && userEmail != null) {
            userTickets = ticketRepository.findByOwnerEmailAndEventId(userEmail, event.getId());
            // Filter to valid tickets
            userTickets = userTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.ISSUED || t.getStatus() == TicketStatus.VALIDATED)
                    .toList();
        }

        boolean hasTicket = !userTickets.isEmpty();
        boolean hasValidTicket = userTickets.stream()
                .anyMatch(t -> t.getStatus() == TicketStatus.ISSUED || t.getStatus() == TicketStatus.VALIDATED);
        boolean hasValidatedTicket = userTickets.stream()
                .anyMatch(t -> t.getStatus() == TicketStatus.VALIDATED);
        boolean hasPendingTicket = userTickets.stream()
                .anyMatch(t -> t.getStatus() == TicketStatus.PENDING);
        boolean isEventCompleted = isEventCompleted(event);

        builder.hasTicket(hasTicket)
                .hasValidTicket(hasValidTicket)
                .ticketCount(userTickets.size());

        // Set primary ticket info
        if (hasTicket) {
            Ticket primaryTicket = userTickets.get(0);
            builder.primaryTicketId(primaryTicket.getId())
                    .primaryTicketNumber(primaryTicket.getTicketNumber())
                    .primaryTicketStatus(primaryTicket.getStatus().name())
                    .ticketIssuedAt(primaryTicket.getIssuedAt());
            
            if (primaryTicket.getTicketType() != null) {
                builder.primaryTicketTypeName(primaryTicket.getTicketType().getName());
                
                // Payment info
                BigDecimal price = primaryTicket.getTicketType().getPrice();
                builder.requiresPayment(price != null && price.compareTo(BigDecimal.ZERO) > 0)
                        .hasPaid(primaryTicket.getStatus() == TicketStatus.ISSUED || 
                                primaryTicket.getStatus() == TicketStatus.VALIDATED)
                        .paymentCurrency(primaryTicket.getTicketType().getCurrency());
            }

            // Check-in via validated ticket
            if (hasValidatedTicket) {
                builder.hasCheckedIn(true)
                        .checkedInAt(primaryTicket.getValidatedAt());
            }
        }

        // Check if feeds are public after event
        if (isEventCompleted && Boolean.TRUE.equals(event.getFeedsPublicAfterEvent())) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.EVENT_ENDED_PUBLIC)
                    .accessMessage("Event has ended - content is now public")
                    .canViewFeeds(true)
                    .canBuyTicket(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (hasValidatedTicket) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.TICKET_VALIDATED)
                    .accessMessage("Your ticket has been validated")
                    .canViewFeeds(true)
                    .canBuyTicket(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (hasValidTicket) {
            builder.hasAccess(true)
                    .accessStatus(UserEventAccessStatus.TICKET_PURCHASED)
                    .accessMessage("You have a valid ticket")
                    .canViewFeeds(true)
                    .canBuyTicket(false)
                    .primaryAction("VIEW_FEEDS")
                    .primaryActionLabel("View Event");
        } else if (hasPendingTicket) {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.TICKET_PENDING)
                    .accessMessage("Complete payment to access this event")
                    .canViewFeeds(false)
                    .canBuyTicket(false) // Already has pending ticket
                    .primaryAction("COMPLETE_PAYMENT")
                    .primaryActionLabel("Complete Payment");
        } else if (isEventCompleted) {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.EVENT_ENDED_NO_ACCESS)
                    .accessMessage("Event has ended")
                    .canViewFeeds(false)
                    .canBuyTicket(false)
                    .primaryAction("NONE")
                    .primaryActionLabel("Event Ended");
        } else {
            builder.hasAccess(false)
                    .accessStatus(UserEventAccessStatus.TICKET_NOT_PURCHASED)
                    .accessMessage("Purchase a ticket to access this event")
                    .canViewFeeds(false)
                    .canBuyTicket(true)
                    .primaryAction("BUY_TICKET")
                    .primaryActionLabel("Buy Ticket");
        }

        builder.canRsvp(false)
                .canRespondToInvite(false)
                .canExpressInterest(false);

        return builder.build();
    }

    /**
     * Check if an event is completed (past end date or COMPLETED status).
     */
    private boolean isEventCompleted(Event event) {
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            return true;
        }
        if (event.getEndDateTime() != null) {
            return LocalDateTime.now().isAfter(event.getEndDateTime());
        }
        if (event.getStartDateTime() != null) {
            return LocalDateTime.now().isAfter(event.getStartDateTime().plusDays(1));
        }
        return false;
    }
}

