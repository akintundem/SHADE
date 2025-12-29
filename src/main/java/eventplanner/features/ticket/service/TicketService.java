package eventplanner.features.ticket.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.exception.ApiException;
import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.ValidateTicketRequest;
import eventplanner.features.ticket.dto.response.TicketResponse;
import eventplanner.features.ticket.dto.response.TicketWalletResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final AttendeeRepository attendeeRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    @Value("${app.ticket.qr-secret:default-secret-key-change-in-production}")
    private String qrSecretKey;

    private static final int DEFAULT_MAX_TICKETS_PER_PERSON = 5;

    /**
     * Check if a ticket type is free (price is null or zero).
     */
    private boolean isFreeTicket(TicketType ticketType) {
        return ticketType.getPrice() == null || 
               ticketType.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0;
    }

    /**
     * Issue tickets to one or more attendees.
     * Accepts a list of ticket requests - each request can issue multiple tickets (quantity) to a single attendee/email.
     */
    public List<Ticket> issueTickets(List<IssueTicketRequest> requests, UserPrincipal principal) {
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

            List<Ticket> tickets = issueSingleTicketRequest(request, principal);
            allTickets.addAll(tickets);
        }

        return allTickets;
    }

    /**
     * Issue one or more tickets to an attendee (internal method).
     */
    private List<Ticket> issueSingleTicketRequest(IssueTicketRequest request, UserPrincipal principal) {
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

        // Validate that either attendeeId or email/name is provided
        if (request.getAttendeeId() == null && (request.getOwnerEmail() == null || request.getOwnerName() == null)) {
            throw new IllegalArgumentException("Either attendeeId or both ownerEmail and ownerName must be provided");
        }

        Attendee attendee = null;
        if (request.getAttendeeId() != null) {
            attendee = attendeeRepository.findById(request.getAttendeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendee not found: " + request.getAttendeeId()));
        }

        // Validate ticket type availability
        if (!ticketType.canPurchase(request.getQuantity())) {
            throw new ApiException("TICKET_TYPE_SOLD_OUT", 
                "Not enough tickets available. Remaining: " + ticketType.getQuantityRemaining(), 409);
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
                throw new ApiException("MAX_TICKETS_EXCEEDED",
                    "Cannot issue more than " + maxTicketsPerPerson + " tickets per person. " +
                    "You already have " + existingTickets + " ticket(s) for this event.", 400);
            }
        }

        // Check limit for email-based tickets
        if (attendee == null && request.getOwnerEmail() != null) {
            long existingTickets = ticketRepository.findByOwnerEmailAndEventId(
                request.getOwnerEmail(), request.getEventId()).size();
            if (existingTickets + request.getQuantity() > maxTicketsPerPerson) {
                throw new ApiException("MAX_TICKETS_EXCEEDED",
                    "Cannot issue more than " + maxTicketsPerPerson + " tickets per person. " +
                    "This email already has " + existingTickets + " ticket(s) for this event.", 400);
            }
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

        // Update ticket type quantities
        if (isFreeTicket(ticketType)) {
            // Free ticket (price is null or zero) - immediately mark as sold
            ticketTypeRepository.incrementQuantitySold(request.getTicketTypeId(), request.getQuantity());
        } else {
            // Paid ticket - reserve for now (will be moved to sold when payment completes)
            ticketTypeRepository.incrementQuantityReserved(request.getTicketTypeId(), request.getQuantity());
        }

        // For free tickets, update attendee RSVP status (only if attendee exists)
        if (isFreeTicket(ticketType) && attendee != null) {
            attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
            attendeeRepository.save(attendee);
        }

        // Issue tickets (set status to ISSUED for free tickets, PENDING for paid)
        for (Ticket ticket : savedTickets) {
            if (isFreeTicket(ticketType)) {
                ticket.issue(issuedBy);
            }
        }
        
        // Save any status changes
        if (isFreeTicket(ticketType)) {
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
            .orElseThrow(() -> new ApiException("INVALID_QR_CODE", "Invalid QR code", 400));

        // Verify event matches
        if (!ticket.getEvent().getId().equals(request.getEventId())) {
            throw new ApiException("INVALID_QR_CODE", "Ticket does not belong to this event", 400);
        }

        // Check if already validated
        if (ticket.getStatus() == TicketStatus.VALIDATED) {
            throw new ApiException("TICKET_ALREADY_VALIDATED", "Ticket has already been validated", 409);
        }

        // Check if cancelled
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ApiException("TICKET_CANCELLED", "Ticket has been cancelled", 400);
        }

        // Check if pending ticket has expired (15 minute window)
        if (ticket.getStatus() == TicketStatus.PENDING && ticket.isPendingExpired()) {
            throw new ApiException("TICKET_EXPIRED", 
                "Pending reservation has expired (15 minute window exceeded)", 400);
        }

        // Check if expired (past event date)
        if (ticket.isExpired()) {
            throw new ApiException("TICKET_EXPIRED", "Ticket has expired", 400);
        }

        // Validate ticket
        UserAccount validatedBy = principal != null && principal.getId() != null
            ? userAccountRepository.findById(principal.getId()).orElse(null)
            : null;

        ticket.validate(validatedBy);
        ticketRepository.save(ticket);

        return ticket;
    }

    /**
     * Cancel a ticket.
     */
    public Ticket cancelTicket(UUID ticketId, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.VALIDATED) {
            throw new ApiException("TICKET_ALREADY_VALIDATED", 
                "Cannot cancel a validated ticket", 409);
        }

        ticket.cancel(null);
        Ticket saved = ticketRepository.save(ticket);

        // Release reserved quantity if it was reserved
        if (ticket.getTicketType().getPrice() != null && ticket.getStatus() == TicketStatus.PENDING) {
            ticketTypeRepository.decrementQuantityReserved(ticket.getTicketType().getId(), 1);
        }

        return saved;
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
                    Map<String, Object> templateVars = Map.of(
                        "eventName", event.getName(),
                        "eventId", event.getId().toString(),
                        "ticketCount", entry.getValue().size()
                    );
                    
                    notificationService.send(NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(email)
                        .subject("Your tickets for: " + event.getName())
                        .templateId("ticket-confirmation")
                        .templateVariables(templateVars)
                        .eventId(event.getId())
                        .build());
                }
            } catch (Exception e) {
            }
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
