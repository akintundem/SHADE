package eventplanner.features.attendee.service;

import eventplanner.security.auth.enums.VisibilityLevel;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.common.util.UserAccountUtil;
import eventplanner.features.attendee.dto.request.CreateAttendeeInviteRequest;
import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeInviteRepository;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.config.AppProperties;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.AuthValidationUtil;
import eventplanner.security.util.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@Transactional
public class AttendeeInviteService {

    private static final int DEFAULT_INVITE_TTL_DAYS = 14;

    private final AttendeeInviteRepository inviteRepository;
    private final AttendeeRepository attendeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final ExternalServicesProperties externalServicesProperties;
    private final String appBaseUrl;

    public AttendeeInviteService(
            AttendeeInviteRepository inviteRepository,
            AttendeeRepository attendeeRepository,
            UserAccountRepository userAccountRepository,
            EventRepository eventRepository,
            NotificationService notificationService,
            ExternalServicesProperties externalServicesProperties,
            AppProperties appProperties
    ) {
        this.inviteRepository = inviteRepository;
        this.attendeeRepository = attendeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
        this.externalServicesProperties = externalServicesProperties;
        this.appBaseUrl = requireConfigured(appProperties.getBaseUrl(), "app.base-url");
    }

    public AttendeeInvite createInvite(UUID eventId, UserPrincipal inviter, CreateAttendeeInviteRequest request) {
        if (inviter == null) {
            throw new BadRequestException("Authentication required");
        }
        if (request == null) {
            throw new BadRequestException("Request is required");
        }

        UUID inviteeUserId = request.getInviteeUserId();
        String inviteeEmail = AuthValidationUtil.safeTrim(request.getInviteeEmail());
        if (inviteeUserId == null && (inviteeEmail == null || inviteeEmail.isBlank())) {
            throw new BadRequestException("Either inviteeUserId or inviteeEmail must be provided");
        }
        if (inviteeUserId == null && inviteeEmail != null && Boolean.FALSE.equals(request.getSendEmail())) {
            throw new BadRequestException("Email delivery is required when inviting by email");
        }

        if (inviteeEmail != null && !inviteeEmail.isBlank()) {
            inviteeEmail = AuthValidationUtil.normalizeEmail(inviteeEmail);
        } else {
            inviteeEmail = null;
        }

        if (inviteeUserId == null && inviteeEmail != null) {
            Optional<UserAccount> existing = userAccountRepository.findByEmailIgnoreCase(inviteeEmail);
            if (existing.isPresent()) {
                inviteeUserId = existing.get().getId();
            }
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ensureInvitesAllowed(event);

        if (isAtCapacity(event)) {
            throw new BadRequestException("Event is at capacity");
        }

        UserAccount inviterUser = UserAccountUtil.getManagedUserAccountOrThrow(
                inviter, userAccountRepository, "Inviter user not found");

        if (inviteeUserId != null && event.getOwner() != null && inviteeUserId.equals(event.getOwner().getId())) {
            throw new BadRequestException("Event owner cannot be invited as an attendee");
        }
        if (inviteeEmail != null && event.getOwner() != null && event.getOwner().getEmail() != null &&
                inviteeEmail.equalsIgnoreCase(event.getOwner().getEmail())) {
            throw new BadRequestException("Event owner cannot be invited as an attendee");
        }

        if (inviteeUserId != null &&
                attendeeRepository.findByEventIdAndUserId(eventId, inviteeUserId).isPresent()) {
            throw new BadRequestException("User is already an attendee for this event");
        }
        if (inviteeEmail != null &&
                attendeeRepository.findByEventIdAndEmailIgnoreCase(eventId, inviteeEmail).isPresent()) {
            throw new BadRequestException("Email is already an attendee for this event");
        }

        UserAccount inviteeUser = null;
        if (inviteeUserId != null) {
            inviteeUser = userAccountRepository.findById(inviteeUserId).orElse(null);
            if (inviteeUser == null) {
                throw new BadRequestException("Invitee user not found: " + inviteeUserId);
            }
            if (inviteeEmail != null && inviteeUser.getEmail() != null &&
                    !inviteeEmail.equalsIgnoreCase(inviteeUser.getEmail())) {
                throw new BadRequestException("inviteeEmail does not match inviteeUserId");
            }
        }

        AttendeeInvite invite = null;
        if (inviteeUserId != null) {
            invite = inviteRepository
                    .findFirstByEventIdAndInviteeIdAndStatusOrderByCreatedAtDesc(eventId, inviteeUserId, AttendeeInviteStatus.PENDING)
                    .orElse(null);
        } else if (inviteeEmail != null) {
            invite = inviteRepository
                    .findFirstByEventIdAndInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(eventId, inviteeEmail, AttendeeInviteStatus.PENDING)
                    .orElse(null);
        }

        if (invite == null) {
            invite = new AttendeeInvite();
            invite.setEvent(event);
        }

        invite.setInviter(inviterUser);
        invite.setEvent(event);
        invite.setInvitee(inviteeUser);
        invite.setInviteeEmail(inviteeEmail);
        invite.setMessage(AuthValidationUtil.safeTrim(request.getMessage()));
        invite.setStatus(AttendeeInviteStatus.PENDING);
        invite.setRespondedAt(null);
        invite.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(DEFAULT_INVITE_TTL_DAYS));

        String rawToken = null;
        if (inviteeEmail != null) {
            rawToken = TokenUtil.generateToken();
            invite.setTokenHash(TokenUtil.hashToken(rawToken));
        } else {
            invite.setTokenHash(null);
        }

        AttendeeInvite saved = inviteRepository.save(invite);

        if (Boolean.TRUE.equals(request.getSendPush()) && saved.getInvitee() != null) {
            sendPushInvite(saved);
        }
        if (Boolean.TRUE.equals(request.getSendEmail()) && saved.getInviteeEmail() != null && rawToken != null) {
            sendEmailInvite(saved, rawToken, inviterUser);
        }

        return saved;
    }

    public List<AttendeeInvite> createInvitesBulk(UUID eventId, UserPrincipal inviter, List<CreateAttendeeInviteRequest> requests) {
        if (inviter == null) {
            throw new BadRequestException("Authentication required");
        }
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one invite request is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ensureInvitesAllowed(event);

        Set<UUID> seenUserIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        List<AttendeeInvite> results = new java.util.ArrayList<>();
        for (CreateAttendeeInviteRequest request : requests) {
            if (request == null) {
                throw new BadRequestException("Invite request cannot be null");
            }

            UUID inviteeUserId = request.getInviteeUserId();
            String inviteeEmail = AuthValidationUtil.safeTrim(request.getInviteeEmail());
            if (inviteeEmail != null && !inviteeEmail.isBlank()) {
                inviteeEmail = AuthValidationUtil.normalizeEmail(inviteeEmail);
            } else {
                inviteeEmail = null;
            }

            if (inviteeUserId == null && (inviteeEmail == null || inviteeEmail.isBlank())) {
                throw new BadRequestException("Either inviteeUserId or inviteeEmail must be provided");
            }
            if (inviteeUserId != null && !seenUserIds.add(inviteeUserId)) {
                throw new BadRequestException("Duplicate invitee userId in request: " + inviteeUserId);
            }
            if (inviteeEmail != null && !seenEmails.add(inviteeEmail)) {
                throw new BadRequestException("Duplicate invitee email in request: " + inviteeEmail);
            }
        }

        for (CreateAttendeeInviteRequest request : requests) {
            results.add(createInvite(eventId, inviter, request));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public AttendeeInvite getInviteById(UUID eventId, UUID inviteId) {
        if (eventId == null || inviteId == null) {
            throw new BadRequestException("Event ID and invite ID are required");
        }
        return inviteRepository.findByIdAndEventId(inviteId, eventId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found: " + inviteId));
    }

    @Transactional(readOnly = true)
    public Page<AttendeeInvite> listEventInvites(UUID eventId, AttendeeInviteStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (status != null) {
            return inviteRepository.findByEventIdAndStatus(eventId, status, PageRequest.of(safePage, safeSize));
        }
        return inviteRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize));
    }

    public void revokeInvite(UUID eventId, UUID inviteId) {
        AttendeeInvite invite = getInviteById(eventId, inviteId);

        if (invite.getStatus() != AttendeeInviteStatus.PENDING) {
            throw new BadRequestException("Only pending invites can be revoked");
        }

        invite.setStatus(AttendeeInviteStatus.REVOKED);
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);
    }

    public AttendeeInvite resendInvite(UUID eventId, UUID inviteId, Boolean sendEmail, Boolean sendPush) {
        AttendeeInvite invite = getInviteById(eventId, inviteId);
        AttendeeInviteStatus status = invite.getStatus();

        if (status == AttendeeInviteStatus.ACCEPTED || status == AttendeeInviteStatus.REVOKED) {
            throw new BadRequestException("Only pending or expired invites can be resent");
        }
        if (status == AttendeeInviteStatus.DECLINED) {
            throw new BadRequestException("Declined invites cannot be resent");
        }

        invite.setStatus(AttendeeInviteStatus.PENDING);
        invite.setRespondedAt(null);
        invite.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(DEFAULT_INVITE_TTL_DAYS));

        String rawToken = null;
        if (invite.getInviteeEmail() != null) {
            rawToken = TokenUtil.generateToken();
            invite.setTokenHash(TokenUtil.hashToken(rawToken));
        } else {
            invite.setTokenHash(null);
        }

        AttendeeInvite saved = inviteRepository.save(invite);

        if (Boolean.TRUE.equals(sendPush) && saved.getInvitee() != null) {
            sendPushInvite(saved);
        }
        if (Boolean.TRUE.equals(sendEmail) && saved.getInviteeEmail() != null && rawToken != null) {
            sendEmailInvite(saved, rawToken, saved.getInviter());
        }

        return saved;
    }


    /**
     * Update invite RSVP status by inviteId or token.
     * @param inviteId Optional invite ID (if provided, token is ignored)
     * @param token Optional token (used if inviteId is not provided)
     * @param status Status to set (any valid AttendeeInviteStatus)
     * @param principal Authenticated user
     * @return Created attendee if status is ACCEPTED, null otherwise
     */
    public Attendee updateInviteStatus(UUID inviteId, String token, AttendeeInviteStatus status, UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new BadRequestException("Authentication and user account information are required");
        }
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        
        // Validate status transitions
        if (status == AttendeeInviteStatus.PENDING) {
            throw new BadRequestException("Cannot set invite status back to PENDING");
        }
        
        AttendeeInvite invite;
        if (inviteId != null) {
            invite = inviteRepository.findById(inviteId)
                    .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        } else if (token != null && !token.trim().isEmpty()) {
            String tokenHash = TokenUtil.hashToken(token.trim());
            invite = inviteRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        } else {
            throw new BadRequestException("Either inviteId or token must be provided");
        }
        
        // Verify the logged-in user is the one who can respond to this invite
        verifyInviteBelongsToPrincipal(invite, principal);
        
        // Check if invite is already in a final state
        if (invite.getStatus() != AttendeeInviteStatus.PENDING) {
            throw new BadRequestException("Invite is not pending. Current status: " + invite.getStatus());
        }
        
        // Check expiry
        if (isExpired(invite)) {
            invite.setStatus(AttendeeInviteStatus.EXPIRED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            notifyOwnerOfInviteStatus(invite.getEvent(), principal.getUser(), AttendeeInviteStatus.EXPIRED);
            notifyInviterOfInviteStatus(invite, principal.getUser(), AttendeeInviteStatus.EXPIRED);
            throw new BadRequestException("Invite has expired");
        }
        
        // Handle status update
        if (status == AttendeeInviteStatus.ACCEPTED) {
            Attendee attendee = acceptInviteInternal(invite, principal);
            notifyOwnerOfInviteStatus(invite.getEvent(), principal.getUser(), AttendeeInviteStatus.ACCEPTED);
            notifyInviterOfInviteStatus(invite, principal.getUser(), AttendeeInviteStatus.ACCEPTED);
            return attendee;
        } else {
            // DECLINED, REVOKED, or EXPIRED
            invite.setStatus(status);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            notifyOwnerOfInviteStatus(invite.getEvent(), principal.getUser(), status);
            notifyInviterOfInviteStatus(invite, principal.getUser(), status);
            return null;
        }
    }

    private boolean isExpired(AttendeeInvite invite) {
        return invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt());
    }

    private Attendee acceptInviteInternal(AttendeeInvite invite, UserPrincipal principal) {
        Event event = invite.getEvent();
        if (event == null) {
            throw new BadRequestException("Event not found for invite");
        }
        UUID eventId = event.getId();
        UUID userId = principal.getId();

        // If they already registered somehow, mark invite accepted and return existing attendee.
        Optional<Attendee> existingAttendee = attendeeRepository.findByEventIdAndUserId(eventId, userId);
        if (existingAttendee.isPresent()) {
            finalizeInvite(invite, AttendeeInviteStatus.ACCEPTED);
            return existingAttendee.get();
        }

        if (isAtCapacity(event)) {
            throw new BadRequestException("Event is at capacity");
        }

        // Fetch UserAccount for attendee
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check for an email-only attendee and upgrade it instead of creating duplicates
        if (user.getEmail() != null) {
            Optional<Attendee> existingByEmail = attendeeRepository.findByEventIdAndEmailIgnoreCase(eventId, user.getEmail());
            if (existingByEmail.isPresent()) {
                Attendee existing = existingByEmail.get();

                if (existing.getUser() != null && !existing.getUser().getId().equals(userId)) {
                    throw new BadRequestException("An attendee with this email already exists for this event");
                }

                existing.setUser(user);
                if (existing.getName() == null || existing.getName().trim().isEmpty()) {
                    existing.setName(user.getName());
                }
                existing.setEmail(user.getEmail());
                existing.setRsvpStatus(
                    Boolean.TRUE.equals(event.getRequiresApproval()) 
                        ? AttendeeStatus.PENDING 
                        : AttendeeStatus.CONFIRMED);

                if (existing.getParticipationVisibility() == null && user.getSettings() != null
                        && user.getSettings().getEventParticipationVisibility() != null) {
                    existing.setParticipationVisibility(user.getSettings().getEventParticipationVisibility());
                }

                Attendee savedExisting = attendeeRepository.save(existing);
                finalizeInvite(invite, AttendeeInviteStatus.ACCEPTED);
                return savedExisting;
            }
        }
        
        Attendee attendee = new Attendee();
        attendee.setEvent(event);
        attendee.setUser(user);
        attendee.setRsvpStatus(
            Boolean.TRUE.equals(event.getRequiresApproval()) 
                ? AttendeeStatus.PENDING 
                : AttendeeStatus.CONFIRMED);
        
        // Set name and email from user account if available
        if (user.getName() != null) {
            attendee.setName(user.getName());
        } else {
            throw new BadRequestException("User name is required");
        }
        attendee.setEmail(user.getEmail());

        // Set participation visibility from user's default setting
        VisibilityLevel visibility = VisibilityLevel.PUBLIC; // Default fallback
        if (user.getSettings() != null && user.getSettings().getEventParticipationVisibility() != null) {
            visibility = user.getSettings().getEventParticipationVisibility();
        }
        attendee.setParticipationVisibility(visibility);

        Attendee savedAttendee = attendeeRepository.save(attendee);
        finalizeInvite(invite, AttendeeInviteStatus.ACCEPTED);

        return savedAttendee;
    }

    private void finalizeInvite(AttendeeInvite invite, AttendeeInviteStatus status) {
        invite.setStatus(status);
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);
    }
    
    private void notifyOwnerOfInviteStatus(Event event, UserAccount invitee, AttendeeInviteStatus status) {
        try {
            if (event == null || event.getOwner() == null || event.getOwner().getId() == null) {
                return;
            }
            UUID ownerId = event.getOwner().getId();
            if (invitee != null && invitee.getId() != null && invitee.getId().equals(ownerId)) {
                return; // Do not notify self
            }
            String inviteeName = invitee != null
                    ? (invitee.getName() != null ? invitee.getName() : invitee.getEmail())
                    : "Guest";
            String body = String.format("%s %s the invite for %s",
                    inviteeName != null ? inviteeName : "Guest",
                    status.name().toLowerCase(),
                    event.getName());
            
            HashMap<String, Object> data = new HashMap<>();
            data.put("body", body);
            if (event.getId() != null) {
                data.put("eventId", event.getId().toString());
            }
            data.put("inviteStatus", status.name());
            
            notificationService.send(NotificationRequest.builder()
                    .type(CommunicationType.PUSH_NOTIFICATION)
                    .to(ownerId.toString())
                    .subject("Invite " + status.name().toLowerCase())
                    .templateVariables(data)
                    .eventId(event.getId())
                    .build());
        } catch (Exception e) {
            // Best-effort notification
        }
    }

    private void notifyInviterOfInviteStatus(AttendeeInvite invite, UserAccount invitee, AttendeeInviteStatus status) {
        try {
            if (invite == null || invite.getInviter() == null) {
                return;
            }
            UserAccount inviter = invite.getInviter();
            if (inviter.getEmail() == null || inviter.getEmail().isBlank()) {
                return;
            }
            if (invitee != null && invitee.getId() != null && invitee.getId().equals(inviter.getId())) {
                return;
            }
            Event event = invite.getEvent();
            if (event == null) {
                return;
            }

            String inviteeName = invitee != null
                    ? (invitee.getName() != null ? invitee.getName() : invitee.getEmail())
                    : "Guest";
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("inviterName", inviter.getName() != null ? inviter.getName() : "Organizer");
            vars.put("inviteeName", inviteeName);
            vars.put("eventName", event.getName());
            vars.put("eventId", event.getId() != null ? event.getId().toString() : null);
            vars.put("status", status.name());
            String eventUrl = event.getEventWebsiteUrl() != null
                    ? event.getEventWebsiteUrl()
                    : appBaseUrl + "/events/" + (event.getId() != null ? event.getId().toString() : "");
            vars.put("eventUrl", eventUrl);

            notificationService.send(NotificationRequest.builder()
                    .type(CommunicationType.EMAIL)
                    .to(inviter.getEmail())
                    .subject("Invite " + status.name().toLowerCase() + " for " + event.getName())
                    .templateId("attendee-invite-response")
                    .templateVariables(vars)
                    .eventId(event.getId())
                    .from(externalServicesProperties.getEmail().getFromEvents())
                    .build());
        } catch (Exception e) {
            // Best-effort notification
        }
    }

    private void sendPushInvite(AttendeeInvite invite) {
        Event event = invite.getEvent();
        if (event == null || invite.getInvitee() == null || invite.getInvitee().getId() == null) {
            return;
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("body", "You have been invited to: " + event.getName());
        data.put("inviteId", invite.getId() != null ? invite.getId().toString() : null);
        data.put("eventId", event.getId() != null ? event.getId().toString() : null);

        // CRITICAL: Event invites must be delivered
        notificationService.sendOrThrow(NotificationRequest.builder()
                .type(CommunicationType.PUSH_NOTIFICATION)
                .to(invite.getInvitee().getId().toString())
                .subject("Event invite")
                .templateVariables(data)
                .eventId(event.getId())
                .build());
    }

    private void sendEmailInvite(AttendeeInvite invite, String rawToken, UserAccount inviterUser) {
        Event event = invite.getEvent();
        if (event == null || invite.getInviteeEmail() == null || rawToken == null) {
            return;
        }

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("inviterName", inviterUser != null && inviterUser.getName() != null
                ? inviterUser.getName()
                : "Someone");
        variables.put("inviteeName", invite.getInvitee() != null && invite.getInvitee().getName() != null
                ? invite.getInvitee().getName()
                : invite.getInviteeEmail());
        variables.put("eventName", event.getName());
        variables.put("eventId", event.getId() != null ? event.getId().toString() : null);
        if (invite.getMessage() != null) {
            variables.put("message", invite.getMessage());
        }

        String acceptUrl = appBaseUrl + "/api/v1/attendees/invites?token=" + rawToken + "&status=accepted";
        variables.put("acceptUrl", acceptUrl);

        // CRITICAL: Event invitation emails must be delivered
        notificationService.sendOrThrow(NotificationRequest.builder()
                .type(CommunicationType.EMAIL)
                .to(invite.getInviteeEmail())
                .subject("You're invited to: " + event.getName())
                .templateId("attendee-invite")
                .templateVariables(variables)
                .eventId(event.getId())
                .from(externalServicesProperties.getEmail().getFromEvents())
                .build());
    }

    private boolean isAtCapacity(Event event) {
        if (event == null) {
            return false;
        }
        Integer capacity = event.getCapacity();
        if (capacity == null || capacity <= 0) {
            return false;
        }
        Integer current = event.getCurrentAttendeeCount();
        if (current == null) {
            current = 0;
        }
        return current >= capacity;
    }

    private void ensureInvitesAllowed(Event event) {
        if (event == null || event.getAccessType() == null) {
            return;
        }
        switch (event.getAccessType()) {
            case INVITE_ONLY:
            case TICKETED:
                return;
            default:
                throw new BadRequestException("Invitations are only supported for invite-only or ticketed events");
        }
    }

    private void verifyInviteBelongsToPrincipal(AttendeeInvite invite, UserPrincipal principal) {
        UUID principalId = principal.getId();
        String principalEmail = principal.getUser().getEmail();

        if (principalId == null) {
            throw new BadRequestException("User ID is required");
        }
        if (principalEmail == null || principalEmail.trim().isEmpty()) {
            throw new BadRequestException("User email is required");
        }

        UUID inviteeUserId = invite.getInvitee() != null ? invite.getInvitee().getId() : null;
        String inviteeEmail = invite.getInviteeEmail();

        // Check if invite was sent to a specific user ID
        boolean matchesUserId = inviteeUserId != null && inviteeUserId.equals(principalId);
        
        // Check if invite was sent to an email (normalize for comparison)
        boolean matchesEmail = inviteeEmail != null 
                && !inviteeEmail.trim().isEmpty()
                && principalEmail != null
                && inviteeEmail.trim().equalsIgnoreCase(principalEmail.trim());

        // The invite must match either by userId OR by email
        if (!matchesUserId && !matchesEmail) {
            String errorMessage = String.format(
                "Invite does not belong to authenticated user. Invite is for userId=%s or email=%s, but authenticated user is userId=%s, email=%s",
                inviteeUserId != null ? inviteeUserId.toString() : "null",
                inviteeEmail != null ? inviteeEmail : "null",
                principalId.toString(),
                principalEmail
            );
            throw new BadRequestException(errorMessage);
        }
    }

    private static String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return value;
    }

}
