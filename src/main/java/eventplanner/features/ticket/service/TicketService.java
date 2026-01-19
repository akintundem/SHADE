package eventplanner.features.ticket.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ConflictException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.BulkTicketActionRequest;
import eventplanner.features.ticket.dto.request.ResendTicketRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketRequest;
import eventplanner.features.ticket.dto.request.TransferTicketRequest;
import eventplanner.features.ticket.dto.request.ValidateTicketRequest;
import eventplanner.features.ticket.dto.response.BulkTicketActionResponse;
import eventplanner.features.ticket.dto.response.BulkTicketActionResult;
import eventplanner.features.ticket.dto.response.TicketResponse;
import eventplanner.features.ticket.dto.response.TicketWalletResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.enums.BulkTicketAction;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.AuthValidationUtil;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.features.config.AppProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ticket management operations.
 * Handles ticket issuance, validation, QR code generation, and integration with attendee system.
 */
@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final AttendeeRepository attendeeRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final ExternalServicesProperties externalServicesProperties;
    private final String qrSecretKey;

    private static final int DEFAULT_MAX_TICKETS_PER_PERSON = 5;

    public TicketService(TicketRepository ticketRepository,
                         TicketTypeRepository ticketTypeRepository,
                         AttendeeRepository attendeeRepository,
                         EventRepository eventRepository,
                         UserAccountRepository userAccountRepository,
                         NotificationService notificationService,
                         ExternalServicesProperties externalServicesProperties,
                         AppProperties appProperties) {
        this.ticketRepository = ticketRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.attendeeRepository = attendeeRepository;
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.externalServicesProperties = externalServicesProperties;
        this.qrSecretKey = requireConfigured(appProperties.getTicket().getQrSecret(), "app.ticket.qr-secret");
    }

    private static String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return value;
    }

    /**
     * Check if a ticket type is free (price is null or zero).
     */
    private boolean isFreeTicket(TicketType ticketType) {
        return ticketType.getPriceMinor() == null || ticketType.getPriceMinor() == 0;
    }

    /**
     * Issue tickets to one or more attendees.
     * Accepts a list of ticket requests - each request can issue multiple tickets (quantity) to a single attendee/email.
     */
    public List<Ticket> issueTickets(List<IssueTicketRequest> requests, UserPrincipal principal) {
        return issueTickets(requests, principal, true, false);
    }

    /**
     * Issue tickets with optional control over free-ticket finalization.
     * When finalizeFreeTickets is false, free tickets are reserved and left in PENDING status (used for checkout flows).
     */
    public List<Ticket> issueTickets(List<IssueTicketRequest> requests, UserPrincipal principal, boolean finalizeFreeTickets) {
        return issueTickets(requests, principal, finalizeFreeTickets, false);
    }

    /**
     * Issue tickets with explicit control over free-ticket finalization and paid-ticket issuance.
     * When forceIssue is true, paid tickets are issued immediately and inventory is marked sold.
     */
    public List<Ticket> issueTickets(List<IssueTicketRequest> requests, UserPrincipal principal,
                                     boolean finalizeFreeTickets, boolean forceIssue) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one ticket request is required");
        }

        List<Ticket> allTickets = new ArrayList<>();
        UUID eventId = requests.get(0).getEventId();

        // Process each request
        for (IssueTicketRequest request : requests) {
            // Validate event ID consistency
            if (!request.getEventId().equals(eventId)) {
                throw new IllegalArgumentException("All ticket requests must be for the same event");
            }

            List<Ticket> tickets = issueSingleTicketRequest(request, principal, finalizeFreeTickets, forceIssue);
            allTickets.addAll(tickets);
        }

        return allTickets;
    }

    /**
     * Issue one or more tickets to an attendee (internal method).
     */
    private List<Ticket> issueSingleTicketRequest(IssueTicketRequest request, UserPrincipal principal, boolean finalizeFreeTickets) {
        return issueSingleTicketRequest(request, principal, finalizeFreeTickets, false);
    }

    private List<Ticket> issueSingleTicketRequest(IssueTicketRequest request, UserPrincipal principal,
                                                  boolean finalizeFreeTickets, boolean forceIssue) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        // Fetch entities
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + request.getTicketTypeId()));
        if (ticketType.getEvent() == null || !ticketType.getEvent().getId().equals(request.getEventId())) {
            throw new IllegalArgumentException("Ticket type does not belong to event");
        }

        if (request.getAttendeeId() == null && (request.getOwnerEmail() == null || request.getOwnerEmail().isBlank())) {
            if (principal != null && principal.getUser() != null) {
                String email = principal.getUser().getEmail();
                String name = principal.getUser().getName();
                request.setOwnerEmail(email);
                request.setOwnerName(name != null && !name.isBlank() ? name : email);
            }
        }

        // Validate that either attendeeId or email/name is provided
        if (request.getAttendeeId() == null && (request.getOwnerEmail() == null || request.getOwnerName() == null)) {
            throw new IllegalArgumentException("Either attendeeId or both ownerEmail and ownerName must be provided");
        }

        Attendee attendee = null;
        if (request.getAttendeeId() != null) {
            attendee = attendeeRepository.findById(request.getAttendeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendee not found: " + request.getAttendeeId()));
        }

        boolean freeTicket = isFreeTicket(ticketType);
        boolean shouldIssue = (freeTicket && finalizeFreeTickets) || forceIssue;

        // Validate ticket type availability
        if (!ticketType.canPurchase(request.getQuantity())) {
            throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT, 
                "Not enough tickets available");
        }

        // Check max tickets per person (null means unlimited, otherwise enforce configured; fall back to default only if explicitly set to 0)
        Integer configuredMax = ticketType.getMaxTicketsPerPerson();
        int maxTicketsPerPerson = configuredMax == null ? Integer.MAX_VALUE : configuredMax;
        if (configuredMax != null && configuredMax <= 0) {
            maxTicketsPerPerson = DEFAULT_MAX_TICKETS_PER_PERSON;
        }
        
        // Check limit for attendee-based tickets
        if (attendee != null) {
            long existingTickets = ticketRepository.findByAttendeeIdAndEventId(
                attendee.getId(), request.getEventId()).size();
            if (existingTickets + request.getQuantity() > maxTicketsPerPerson) {
                throw new ApiException(ErrorCode.MAX_TICKETS_EXCEEDED,
                    "Cannot issue more than " + maxTicketsPerPerson + " tickets per person");
            }
        }

        // Check limit for email-based tickets
        if (attendee == null && request.getOwnerEmail() != null) {
            long existingTickets = ticketRepository.findByOwnerEmailAndEventId(
                request.getOwnerEmail(), request.getEventId()).size();
            if (existingTickets + request.getQuantity() > maxTicketsPerPerson) {
                throw new ApiException(ErrorCode.MAX_TICKETS_EXCEEDED,
                    "Cannot issue more than " + maxTicketsPerPerson + " tickets per person");
            }
        }

        if (shouldIssue) {
            incrementSoldOrThrow(ticketType.getId(), request.getQuantity());
        } else {
            reserveTicketsOrThrow(ticketType.getId(), request.getQuantity());
        }

        UserAccount issuedBy = principal != null && principal.getId() != null
            ? userAccountRepository.findById(principal.getId()).orElse(null)
            : null;

        // Create tickets (QR code and ticket number are generated in createTicket)
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            Ticket ticket = createTicket(event, ticketType, attendee, request.getOwnerEmail(), request.getOwnerName(), issuedBy);
            tickets.add(ticket);
        }

        // Save all tickets
        List<Ticket> savedTickets = ticketRepository.saveAll(tickets);

        // For free tickets, update attendee RSVP status (only if attendee exists)
        if (freeTicket && finalizeFreeTickets && attendee != null) {
            attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
            attendeeRepository.save(attendee);
        }

        if (shouldIssue) {
            for (Ticket ticket : savedTickets) {
                ticket.issue(issuedBy);
            }
            savedTickets = ticketRepository.saveAll(savedTickets);
        }

        // Send notifications if requested
        if (Boolean.TRUE.equals(request.getSendEmail()) || Boolean.TRUE.equals(request.getSendPushNotification())) {
            sendTicketNotifications(event, savedTickets, request.getSendEmail(), request.getSendPushNotification());
        }

        return savedTickets;
    }


    /**
     * Validate a ticket via QR code.
     */
    public Ticket validateTicket(ValidateTicketRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        // Find ticket by QR code data
        Ticket ticket = ticketRepository.findByQrCodeData(request.getQrCodeData())
            .orElseThrow(() -> new BadRequestException("Invalid QR code"));

        // Verify event exists and matches
        if (ticket.getEvent() == null || ticket.getEvent().getId() == null) {
            throw new BadRequestException("Ticket is not associated with an event");
        }
        if (!ticket.getEvent().getId().equals(request.getEventId())) {
            throw new BadRequestException("Ticket does not belong to this event");
        }

        // Check if already validated
        if (ticket.getStatus() == TicketStatus.VALIDATED) {
            throw new ConflictException("Ticket has already been validated");
        }

        // Check if cancelled
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BadRequestException("Ticket has been cancelled");
        }

        // Check if pending ticket has expired (15 minute window)
        if (ticket.getStatus() == TicketStatus.PENDING && ticket.isPendingExpired()) {
            throw new BadRequestException(
                "Pending reservation has expired (15 minute window exceeded)");
        }

        // Check if expired (past event date)
        if (ticket.isExpired()) {
            throw new BadRequestException("Ticket has expired");
        }

        if (ticket.getStatus() != TicketStatus.ISSUED) {
            throw new ConflictException("Ticket is not in a valid state for validation");
        }

        // Validate ticket
        UserAccount validatedBy = principal != null && principal.getId() != null
            ? userAccountRepository.findById(principal.getId()).orElse(null)
            : null;

        try {
            ticket.validate(validatedBy);
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
        ticketRepository.save(ticket);

        return ticket;
    }

    /**
     * Cancel a ticket.
     */
    public Ticket cancelTicket(UUID ticketId, UserPrincipal principal) {
        return cancelTicket(ticketId, null, principal);
    }

    /**
     * Cancel a ticket with an optional reason.
     */
    public Ticket cancelTicket(UUID ticketId, String reason, UserPrincipal principal) {
        // Load ticket with relationships to avoid lazy loading issues
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
        
        // Ensure relationships are loaded by accessing them (triggers lazy load if needed)
        // This prevents LazyInitializationException when converting to response
        if (ticket.getEvent() != null) {
            ticket.getEvent().getId(); // Trigger lazy load
        }
        if (ticket.getTicketType() != null) {
            ticket.getTicketType().getId(); // Trigger lazy load
        }

        if (ticket.getStatus() == TicketStatus.VALIDATED) {
            throw new ConflictException("Cannot cancel a validated ticket");
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new ConflictException("Cannot cancel a refunded ticket");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ConflictException("Ticket is already cancelled");
        }

        TicketStatus previousStatus = ticket.getStatus();
        try {
            ticket.cancel(reason);
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
        
        Ticket saved;
        try {
            saved = ticketRepository.save(ticket);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.TICKET_CANCEL_SAVE_FAILED, 
                "Failed to save ticket cancellation", e);
        }

        // Release reserved quantity if it was reserved
        if (previousStatus == TicketStatus.PENDING && ticket.getTicketType() != null && ticket.getTicketType().getId() != null) {
            try {
                ticketTypeRepository.decrementQuantityReserved(ticket.getTicketType().getId(), 1);
            } catch (Exception e) {
                // Don't fail the cancellation if quantity update fails
            }
        }

        return saved;
    }

    /**
     * Refund a ticket.
     */
    public Ticket refundTicket(UUID ticketId, String reason, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.PENDING) {
            throw new ConflictException("Cannot refund a pending ticket");
        }

        TicketStatus previousStatus = ticket.getStatus();
        try {
            ticket.refund(reason);
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }

        Ticket saved;
        try {
            saved = ticketRepository.save(ticket);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.TICKET_CANCEL_SAVE_FAILED,
                "Failed to save ticket refund", e);
        }

        if (previousStatus == TicketStatus.ISSUED && ticket.getTicketType() != null) {
            try {
                int updated = ticketTypeRepository.decrementQuantitySold(ticket.getTicketType().getId(), 1);
                if (updated == 0) {
                    throw new ApiException(ErrorCode.TICKET_TYPE_NOT_AVAILABLE,
                        "Failed to update ticket inventory for refund");
                }
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                // Best-effort; do not fail refund if inventory update fails
            }
        }

        return saved;
    }

    /**
     * Update ticket holder details for email-based tickets.
     */
    public Ticket updateTicket(UUID ticketId, UpdateTicketRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new ConflictException("Cannot update a cancelled or refunded ticket");
        }
        if (ticket.getStatus() == TicketStatus.VALIDATED) {
            throw new ConflictException("Cannot update a validated ticket");
        }
        if (ticket.getAttendee() != null) {
            throw new IllegalArgumentException("Use transfer to update attendee-based tickets");
        }

        String ownerEmail = request.getOwnerEmail() != null
            ? AuthValidationUtil.normalizeEmail(request.getOwnerEmail())
            : null;
        String ownerName = request.getOwnerName() != null ? request.getOwnerName().trim() : null;

        if ((ownerEmail == null || ownerEmail.isBlank()) && (ownerName == null || ownerName.isBlank())) {
            throw new IllegalArgumentException("At least one field must be provided");
        }
        if (ownerEmail != null && (ownerName == null || ownerName.isBlank()) &&
            (ticket.getOwnerName() == null || ticket.getOwnerName().isBlank())) {
            throw new IllegalArgumentException("Owner name is required when updating owner email");
        }

        if (ownerEmail != null) {
            ticket.setOwnerEmail(ownerEmail);
        }
        if (ownerName != null && !ownerName.isBlank()) {
            ticket.setOwnerName(ownerName);
        }

        return ticketRepository.save(ticket);
    }

    /**
     * Transfer ticket ownership to another attendee or email address.
     */
    public Ticket transferTicket(UUID ticketId, TransferTicketRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new ConflictException("Cannot transfer a cancelled or refunded ticket");
        }
        if (ticket.getStatus() == TicketStatus.VALIDATED) {
            throw new ConflictException("Cannot transfer a validated ticket");
        }
        if (ticket.getStatus() == TicketStatus.PENDING) {
            throw new ConflictException("Cannot transfer a pending ticket");
        }

        if (request.getNewAttendeeId() == null &&
            (request.getNewOwnerEmail() == null || request.getNewOwnerName() == null)) {
            throw new IllegalArgumentException("New attendeeId or ownerEmail/ownerName is required");
        }

        Attendee newAttendee = null;
        String newOwnerEmail = null;
        String newOwnerName = null;

        if (request.getNewAttendeeId() != null) {
            newAttendee = attendeeRepository.findById(request.getNewAttendeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendee not found: " + request.getNewAttendeeId()));
            if (ticket.getEvent() == null || newAttendee.getEvent() == null ||
                !ticket.getEvent().getId().equals(newAttendee.getEvent().getId())) {
                throw new IllegalArgumentException("Attendee does not belong to this event");
            }
            enforceTransferLimit(ticket, newAttendee.getId(), null);
            ticket.setAttendee(newAttendee);
            ticket.setOwnerEmail(null);
            ticket.setOwnerName(null);
        } else {
            newOwnerEmail = AuthValidationUtil.normalizeEmail(request.getNewOwnerEmail());
            newOwnerName = request.getNewOwnerName() != null ? request.getNewOwnerName().trim() : null;
            if (newOwnerName == null || newOwnerName.isBlank()) {
                throw new IllegalArgumentException("New owner name is required");
            }
            enforceTransferLimit(ticket, null, newOwnerEmail);
            ticket.setAttendee(null);
            ticket.setOwnerEmail(newOwnerEmail);
            ticket.setOwnerName(newOwnerName);
        }

        Ticket saved = ticketRepository.save(ticket);

        if (Boolean.TRUE.equals(request.getSendEmail()) || Boolean.TRUE.equals(request.getSendPush())) {
            Event event = saved.getEvent();
            if (event != null) {
                sendTicketNotifications(event, List.of(saved), request.getSendEmail(), request.getSendPush());
            }
        }

        return saved;
    }

    /**
     * Resend ticket notifications.
     */
    public Ticket resendTicket(UUID ticketId, ResendTicketRequest request, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.PENDING) {
            throw new ConflictException("Ticket has not been issued yet");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new ConflictException("Cannot resend a cancelled or refunded ticket");
        }

        ResendTicketRequest resolved = request != null ? request : new ResendTicketRequest();
        boolean sendEmail = Boolean.TRUE.equals(resolved.getSendEmail());
        boolean sendPush = Boolean.TRUE.equals(resolved.getSendPush());
        if (!sendEmail && !sendPush) {
            return ticket;
        }

        Event event = ticket.getEvent();
        if (event == null) {
            throw new BadRequestException("Ticket is not associated with an event");
        }

        sendTicketNotifications(event, List.of(ticket), sendEmail, sendPush);
        return ticket;
    }

    /**
     * Run a bulk action against multiple tickets.
     */
    public BulkTicketActionResponse bulkAction(BulkTicketActionRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getTicketIds() == null || request.getTicketIds().isEmpty()) {
            throw new IllegalArgumentException("At least one ticket ID is required");
        }
        if (request.getEventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
        }

        List<Ticket> found = ticketRepository.findAllById(request.getTicketIds());
        Map<UUID, Ticket> ticketsById = found.stream()
            .collect(Collectors.toMap(Ticket::getId, t -> t));

        List<BulkTicketActionResult> results = new ArrayList<>();
        int successCount = 0;

        for (UUID ticketId : request.getTicketIds()) {
            Ticket ticket = ticketsById.get(ticketId);
            if (ticket == null) {
                results.add(BulkTicketActionResult.builder()
                    .ticketId(ticketId)
                    .success(false)
                    .message("Ticket not found")
                    .build());
                continue;
            }

            if (ticket.getEvent() == null || ticket.getEvent().getId() == null ||
                !ticket.getEvent().getId().equals(request.getEventId())) {
                results.add(BulkTicketActionResult.builder()
                    .ticketId(ticketId)
                    .success(false)
                    .message("Ticket does not belong to event")
                    .status(ticket.getStatus())
                    .build());
                continue;
            }

            try {
                Ticket updated;
                switch (request.getAction()) {
                    case CANCEL:
                        updated = cancelTicket(ticketId, request.getReason(), principal);
                        break;
                    case REFUND:
                        updated = refundTicket(ticketId, request.getReason(), principal);
                        break;
                    case RESEND:
                        ResendTicketRequest resend = new ResendTicketRequest(
                            request.getSendEmail(), request.getSendPush());
                        updated = resendTicket(ticketId, resend, principal);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported action: " + request.getAction());
                }

                results.add(BulkTicketActionResult.builder()
                    .ticketId(ticketId)
                    .success(true)
                    .status(updated.getStatus())
                    .message("Success")
                    .build());
                successCount++;
            } catch (Exception e) {
                results.add(BulkTicketActionResult.builder()
                    .ticketId(ticketId)
                    .success(false)
                    .status(ticket.getStatus())
                    .message(e.getMessage())
                    .build());
            }
        }

        return BulkTicketActionResponse.builder()
            .eventId(request.getEventId())
            .action(request.getAction())
            .total(results.size())
            .successCount(successCount)
            .failureCount(results.size() - successCount)
            .results(results)
            .build();
    }

    /**
     * Get wallet-ready data for a ticket.
     */
    @Transactional(readOnly = true)
    public TicketWalletResponse getTicketWallet(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        return buildWalletResponse(ticket);
    }

    /**
     * Get tickets for an event with pagination and filtering.
     * Supports filtering by ticketId (for single ticket lookup), status, and ticketTypeId.
     */
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketsByEventId(UUID eventId, UUID ticketId, TicketStatus status, 
                                                     UUID ticketTypeId, Pageable pageable) {
        // If ticketId is provided, return single ticket in a Page wrapper
        if (ticketId != null) {
            Ticket ticket = ticketRepository.findByIdAndEventId(ticketId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
            return new org.springframework.data.domain.PageImpl<>(
                List.of(TicketResponse.from(ticket)), pageable, 1);
        }

        // Otherwise, return filtered list
        Page<Ticket> tickets = ticketRepository.findByEventIdWithFilters(
            eventId, status, ticketTypeId, pageable);

        return tickets.map(TicketResponse::from);
    }

    /**
     * Get tickets for an attendee.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByAttendeeId(UUID attendeeId) {
        List<Ticket> tickets = ticketRepository.findByAttendeeId(attendeeId);
        return tickets.stream()
            .map(TicketResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Generate unique ticket number.
     */
    private String generateTicketNumber(UUID eventId, UUID ticketId) {
        // Use short UUID format: EVT-{eventIdShort}-{ticketIdShort}
        String eventShort = eventId.toString().substring(0, 8).toUpperCase();
        String ticketShort = ticketId.toString().substring(0, 8).toUpperCase();
        return "EVT-" + eventShort + "-" + ticketShort;
    }

    /**
     * Create a new ticket entity.
     * Supports both attendee-based tickets and email-only tickets.
     */
    private Ticket createTicket(Event event, TicketType ticketType, Attendee attendee, 
                                String ownerEmail, String ownerName, UserAccount issuedBy) {
        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setTicketType(ticketType);
        ticket.setAttendee(attendee);
        
        // Set email/name for email-only tickets
        if (attendee == null) {
            ticket.setOwnerEmail(ownerEmail);
            ticket.setOwnerName(ownerName);
        }
        
        ticket.setStatus(TicketStatus.PENDING);
        
        // Generate a unique ID for ticket number and QR code generation
        UUID ticketId = UUID.randomUUID();
        
        // Generate ticket number using the pre-generated ID
        String ticketNumber = generateTicketNumber(event.getId(), ticketId);
        ticket.setTicketNumber(ticketNumber);
        
        // Generate QR code data before save (required by @NotBlank constraint)
        String qrData = generateQrCodeDataForNewTicket(ticketId, ticketNumber, event.getId());
        ticket.setQrCodeData(qrData);
        
        return ticket;
    }
    
    /**
     * Generate QR code data for a new ticket (before it has been saved).
     */
    private String generateQrCodeDataForNewTicket(UUID ticketId, String ticketNumber, UUID eventId) {
        try {
            String data = String.format("ticket:%s:%s:%s",
                ticketId.toString(),
                ticketNumber,
                eventId.toString());

            // Generate hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hashInput = ticketId.toString() + ticketNumber + eventId.toString() + qrSecretKey;
            byte[] hashBytes = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(hashBytes).substring(0, 8);

            return data + ":" + hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate QR code hash", e);
        }
    }

    /**
     * Send ticket notifications.
     */
    private void sendTicketNotifications(Event event, List<Ticket> tickets, 
                                        Boolean sendEmail, Boolean sendPushNotification) {
        if (tickets.isEmpty()) {
            return;
        }

        // Group tickets by attendee (handle both attendee-based and email-based tickets)
        Map<String, List<Ticket>> ticketsByRecipient = tickets.stream()
            .collect(Collectors.groupingBy(t -> {
                if (t.getAttendee() != null) {
                    return "attendee:" + t.getAttendee().getId();
                } else if (t.getOwnerEmail() != null) {
                    return "email:" + t.getOwnerEmail();
                }
                return "unknown";
            }));

        for (Map.Entry<String, List<Ticket>> entry : ticketsByRecipient.entrySet()) {
            Ticket firstTicket = entry.getValue().get(0);
            Attendee attendee = firstTicket.getAttendee();
            String email = attendee != null ? attendee.getEmail() : firstTicket.getOwnerEmail();
            
            try {
                if (Boolean.TRUE.equals(sendEmail) && email != null) {
                    Map<String, Object> templateVars = new java.util.HashMap<>();
                    templateVars.put("eventName", event.getName());
                    templateVars.put("eventId", event.getId().toString());
                    templateVars.put("ticketCount", entry.getValue().size());
                    String attendeeName = attendee != null && attendee.getName() != null && !attendee.getName().isBlank()
                            ? attendee.getName()
                            : "there";
                    templateVars.put("attendeeName", attendeeName);
                    if (event.getEventWebsiteUrl() != null) {
                        templateVars.put("ticketsUrl", event.getEventWebsiteUrl());
                    }
                    
                    notificationService.send(NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(email)
                        .subject("Your tickets for: " + event.getName())
                        .templateId("ticket-confirmation")
                        .templateVariables(templateVars)
                        .eventId(event.getId())
                        .from(externalServicesProperties.getEmail().getFromEvents())
                        .build());
                }
                
                if (Boolean.TRUE.equals(sendPushNotification) && attendee != null && attendee.getUser() != null
                        && attendee.getUser().getId() != null) {
                    Map<String, Object> pushData = new java.util.HashMap<>();
                    pushData.put("body", "Your tickets for: " + event.getName());
                    pushData.put("eventId", event.getId().toString());
                    pushData.put("ticketCount", entry.getValue().size());
                    if (event.getEventWebsiteUrl() != null) {
                        pushData.put("ticketsUrl", event.getEventWebsiteUrl());
                    }
                    
                    notificationService.send(NotificationRequest.builder()
                        .type(CommunicationType.PUSH_NOTIFICATION)
                        .to(attendee.getUser().getId().toString())
                        .subject("Tickets confirmed")
                        .templateVariables(pushData)
                        .eventId(event.getId())
                        .build());
                }
            } catch (Exception e) {
            }
        }
    }

    private void enforceTransferLimit(Ticket ticket, UUID attendeeId, String ownerEmail) {
        if (ticket == null || ticket.getTicketType() == null || ticket.getEvent() == null) {
            return;
        }
        Integer configuredMax = ticket.getTicketType().getMaxTicketsPerPerson();
        int maxTicketsPerPerson = configuredMax == null ? Integer.MAX_VALUE : configuredMax;
        if (configuredMax != null && configuredMax <= 0) {
            maxTicketsPerPerson = DEFAULT_MAX_TICKETS_PER_PERSON;
        }
        if (maxTicketsPerPerson == Integer.MAX_VALUE) {
            return;
        }

        UUID eventId = ticket.getEvent().getId();
        long existingTickets = 0;
        if (attendeeId != null) {
            existingTickets = ticketRepository.findByAttendeeIdAndEventId(attendeeId, eventId).size();
            if (ticket.getAttendee() != null && attendeeId.equals(ticket.getAttendee().getId())) {
                existingTickets = Math.max(0, existingTickets - 1);
            }
        } else if (ownerEmail != null) {
            existingTickets = ticketRepository.findByOwnerEmailAndEventId(ownerEmail, eventId).size();
            if (ticket.getOwnerEmail() != null &&
                ownerEmail.equalsIgnoreCase(ticket.getOwnerEmail())) {
                existingTickets = Math.max(0, existingTickets - 1);
            }
        }

        if (existingTickets + 1 > maxTicketsPerPerson) {
            throw new ApiException(ErrorCode.MAX_TICKETS_EXCEEDED,
                "Cannot issue more than " + maxTicketsPerPerson + " tickets per person");
        }
    }

    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private void reserveTicketsOrThrow(UUID ticketTypeId, int quantity) {
        TicketType lockedType = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));

        if (!lockedType.canPurchase(quantity)) {
            throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT,
                "Not enough tickets available");
        }

        ticketTypeRepository.incrementQuantityReserved(ticketTypeId, quantity);
    }

    private void incrementSoldOrThrow(UUID ticketTypeId, int quantity) {
        TicketType lockedType = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));

        if (!lockedType.canPurchase(quantity)) {
            throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT,
                "Not enough tickets available");
        }

        int updated = ticketTypeRepository.incrementQuantitySold(ticketTypeId, quantity);
        if (updated == 0) {
            throw new ApiException(ErrorCode.TICKET_TYPE_NOT_AVAILABLE,
                "Failed to update ticket inventory");
        }
    }

    private TicketWalletResponse buildWalletResponse(Ticket ticket) {
        if (ticket == null || ticket.getEvent() == null) {
            return TicketWalletResponse.builder()
                .available(Boolean.TRUE)
                .barcodeMessage(ticket != null ? ticket.getQrCodeData() : null)
                .build();
        }

        String venueAddress = null;
        String venueCity = null;
        String venueState = null;
        String venueCountry = null;
        if (ticket.getEvent().getVenue() != null) {
            venueAddress = ticket.getEvent().getVenue().getAddress();
            venueCity = ticket.getEvent().getVenue().getCity();
            venueState = ticket.getEvent().getVenue().getState();
            venueCountry = ticket.getEvent().getVenue().getCountry();
        }

        return TicketWalletResponse.builder()
            .available(Boolean.TRUE)
            .ticketNumber(ticket.getTicketNumber())
            .ticketTypeName(ticket.getTicketType() != null ? ticket.getTicketType().getName() : null)
            .eventName(ticket.getEvent().getName())
            .eventStartDateTime(ticket.getEvent().getStartDateTime())
            .eventEndDateTime(ticket.getEvent().getEndDateTime())
            .venueAddress(venueAddress)
            .venueCity(venueCity)
            .venueState(venueState)
            .venueCountry(venueCountry)
            .barcodeMessage(ticket.getQrCodeData())
            .build();
    }
}
