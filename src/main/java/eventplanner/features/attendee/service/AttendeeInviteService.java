package eventplanner.features.attendee.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.util.UserAccountUtil;
import eventplanner.features.attendee.dto.request.CreateAttendeeInviteRequest;
import eventplanner.features.attendee.dto.response.AttendeeInviteResponse;
import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.repository.AttendeeInviteRepository;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.SecureTokenUtil;
import eventplanner.security.util.TokenHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;

@Service
@Transactional
public class AttendeeInviteService {

    private static final int DEFAULT_INVITE_TTL_DAYS = 14;

    private final AttendeeInviteRepository inviteRepository;
    private final AttendeeRepository attendeeRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    private final String appBaseUrl;

    public AttendeeInviteService(
            AttendeeInviteRepository inviteRepository,
            AttendeeRepository attendeeRepository,
            EventRepository eventRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.inviteRepository = inviteRepository;
        this.attendeeRepository = attendeeRepository;
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.appBaseUrl = appBaseUrl;
    }

    public AttendeeInviteResponse createInvite(UUID eventId, UserPrincipal inviter, CreateAttendeeInviteRequest request) {
        if (inviter == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        UUID inviteeUserId = request.getInviteeUserId();
        String inviteeEmail = safeTrim(request.getInviteeEmail());
        if ((inviteeUserId == null) && (inviteeEmail == null || inviteeEmail.isBlank())) {
            throw new IllegalArgumentException("Either inviteeUserId or inviteeEmail must be provided");
        }

        // Normalize email
        if (inviteeEmail != null && !inviteeEmail.isBlank()) {
            inviteeEmail = normalizeEmail(inviteeEmail);
        } else {
            inviteeEmail = null;
        }

        // Resolve userId from email (optional)
        if (inviteeUserId == null && inviteeEmail != null) {
            Optional<UserAccount> existing = userAccountRepository.findByEmailIgnoreCase(inviteeEmail);
            if (existing.isPresent()) {
                inviteeUserId = existing.get().getId();
            }
        }

        // Prevent inviting someone already registered for the event
        if (inviteeUserId != null) {
            if (attendeeRepository.findByEventIdAndUserId(eventId, inviteeUserId).isPresent()) {
                throw new IllegalArgumentException("User is already registered for this event");
            }
        }

        // Prevent duplicate pending invites
        if (inviteeUserId != null) {
            Optional<AttendeeInvite> existingPending = inviteRepository
                    .findFirstByEventIdAndInviteeUserIdAndStatusOrderByCreatedAtDesc(eventId, inviteeUserId, AttendeeInviteStatus.PENDING);
            if (existingPending.isPresent()) {
                return toResponse(existingPending.get());
            }
        } else if (inviteeEmail != null) {
            Optional<AttendeeInvite> existingPending = inviteRepository
                    .findFirstByEventIdAndInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(eventId, inviteeEmail, AttendeeInviteStatus.PENDING);
            if (existingPending.isPresent()) {
                return toResponse(existingPending.get());
            }
        }

        // Fetch Event entity
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        
        // Get managed UserAccount entity for inviter (required for JPA relationship)
        UserAccount inviterUser = UserAccountUtil.getManagedUserAccountOrThrow(
            inviter, 
            userAccountRepository, 
            "Inviter user not found"
        );
        
        // Fetch invitee UserAccount if userId is provided
        UserAccount inviteeUser = null;
        if (inviteeUserId != null) {
            inviteeUser = userAccountRepository.findById(inviteeUserId)
                .orElse(null); // Optional - may not exist yet if invite is by email
        }
        
        AttendeeInvite invite = new AttendeeInvite();
        invite.setEvent(event);
        invite.setInviter(inviterUser);
        if (inviteeUser != null) {
            invite.setInvitee(inviteeUser);
        }
        invite.setInviteeEmail(inviteeEmail);
        invite.setStatus(AttendeeInviteStatus.PENDING);
        invite.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(DEFAULT_INVITE_TTL_DAYS));
        invite.setMessage(safeTrim(request.getMessage()));

        // If we have an email, create a token-based acceptance flow.
        String rawToken = null;
        if (inviteeEmail != null) {
            rawToken = SecureTokenUtil.generateSecureToken();
            invite.setTokenHash(TokenHashUtil.sha256(rawToken));
        }

        AttendeeInvite saved = inviteRepository.save(invite);

        // Notify invitee via push (in-app) if possible
        if (Boolean.TRUE.equals(request.getSendPush()) && saved.getInvitee() != null) {
            sendPushInvite(saved);
        }

        // Notify invitee via email if requested and email is present
        if (Boolean.TRUE.equals(request.getSendEmail()) && saved.getInviteeEmail() != null) {
            sendEmailInvite(saved, rawToken, inviter.getUser());
        }

        return toResponse(saved);
    }

    public Page<AttendeeInviteResponse> listEventInvites(UUID eventId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return inviteRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize))
                .map(this::toResponse);
    }

    public List<AttendeeInviteResponse> listMyPendingInvites(UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        UUID userId = principal.getId();
        String email = principal.getUser() != null ? principal.getUser().getEmail() : null;
        return inviteRepository.findIncomingInvites(userId, email, AttendeeInviteStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Attendee acceptInviteById(UUID inviteId, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
        }
        AttendeeInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        // Verify the logged-in user is the one who can accept this invite
        verifyInviteBelongsToPrincipal(invite, principal);
        return acceptInvite(invite, principal);
    }

    public Attendee acceptInviteByToken(String token, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token is required");
        }
        String tokenHash = TokenHashUtil.sha256(token.trim());
        AttendeeInvite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        // Verify the logged-in user is the one who can accept this invite (even with token)
        verifyInviteBelongsToPrincipal(invite, principal);
        return acceptInvite(invite, principal);
    }

    public void declineInvite(UUID inviteId, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
        }
        AttendeeInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

        // Verify the logged-in user is the one who can decline this invite
        verifyInviteBelongsToPrincipal(invite, principal);

        if (invite.getStatus() != AttendeeInviteStatus.PENDING) {
            throw new IllegalArgumentException("Invite is not pending");
        }
        if (invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt())) {
            invite.setStatus(AttendeeInviteStatus.EXPIRED);
        } else {
            invite.setStatus(AttendeeInviteStatus.DECLINED);
        }
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);
    }

    public void revokeInvite(UUID inviteId) {
        AttendeeInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

        if (invite.getStatus() != AttendeeInviteStatus.PENDING) {
            throw new IllegalArgumentException("Only pending invites can be revoked");
        }

        invite.setStatus(AttendeeInviteStatus.REVOKED);
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);
    }

    private Attendee acceptInvite(AttendeeInvite invite, UserPrincipal principal) {
        // Double-check that the principal matches the invite (defense in depth)
        // This is already verified in acceptInviteById/acceptInviteByToken, but we check again here
        verifyInviteBelongsToPrincipal(invite, principal);

        if (invite.getStatus() != AttendeeInviteStatus.PENDING) {
            throw new IllegalArgumentException("Invite is not pending");
        }

        // Expiry check
        if (invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt())) {
            invite.setStatus(AttendeeInviteStatus.EXPIRED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            throw new IllegalArgumentException("Invite has expired");
        }

        Event event = invite.getEvent();
        if (event == null) {
            throw new IllegalArgumentException("Event not found for invite");
        }
        UUID eventId = event.getId();
        UUID userId = principal.getId();

        // If they already registered somehow, mark invite accepted and return existing attendee.
        Optional<Attendee> existingAttendee = attendeeRepository.findByEventIdAndUserId(eventId, userId);
        if (existingAttendee.isPresent()) {
            invite.setStatus(AttendeeInviteStatus.ACCEPTED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            return existingAttendee.get();
        }

        // Fetch UserAccount for attendee
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Attendee attendee = new Attendee();
        attendee.setEvent(event);
        attendee.setUser(user);
        attendee.setRsvpStatus(Attendee.Status.CONFIRMED);
        
        // Set name and email from user account if available
        if (user.getName() != null) {
            attendee.setName(user.getName());
        } else {
            throw new IllegalArgumentException("User name is required");
        }
        if (user.getEmail() != null) {
            attendee.setEmail(user.getEmail());
        }

        Attendee savedAttendee = attendeeRepository.save(attendee);

        invite.setStatus(AttendeeInviteStatus.ACCEPTED);
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);

        return savedAttendee;
    }

    private void verifyInviteBelongsToPrincipal(AttendeeInvite invite, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
        }

        UUID principalId = principal.getId();
        String principalEmail = principal.getUser().getEmail();

        if (principalId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (principalEmail == null || principalEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is required");
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
            throw new IllegalArgumentException(errorMessage);
        }

    }

    private void sendPushInvite(AttendeeInvite invite) {
        Event event = invite.getEvent();
        if (event == null) {
            return; // Cannot send notification without event
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("body", "You have been invited to attend an event.");
        data.put("inviteId", invite.getId().toString());
        data.put("eventId", event.getId().toString());

        UUID inviteeUserId = invite.getInvitee() != null ? invite.getInvitee().getId() : null;
        if (inviteeUserId == null) {
            return; // Cannot send push without user ID
        }

        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.PUSH_NOTIFICATION)
                .to(inviteeUserId.toString())
                .subject("Event attendance invite")
                .templateVariables(data)
                .eventId(event.getId())
                .build());
    }

    private void sendEmailInvite(AttendeeInvite invite, String rawToken, UserAccount inviterUser) {
        Event event = invite.getEvent();
        if (event == null) {
            return; // Cannot send notification without event
        }
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("inviterName", inviterUser != null ? inviterUser.getName() : "Someone");
        variables.put("eventId", event.getId().toString());
        if (invite.getMessage() != null) {
            variables.put("customMessage", invite.getMessage());
        }

        String acceptUrl = appBaseUrl + "/api/v1/attendee-invites/accept?token=" + rawToken;
        variables.put("acceptUrl", acceptUrl);

        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.EMAIL)
                .to(invite.getInviteeEmail())
                .subject("You're invited to attend an event")
                // Reuse existing template to avoid missing template failures.
                .templateId("event-invitation")
                .templateVariables(variables)
                .eventId(event.getId())
                .build());
    }

    private AttendeeInviteResponse toResponse(AttendeeInvite invite) {
        AttendeeInviteResponse res = new AttendeeInviteResponse();
        res.setInviteId(invite.getId());
        res.setEventId(invite.getEvent() != null ? invite.getEvent().getId() : null);
        res.setInviterUserId(invite.getInviter() != null ? invite.getInviter().getId() : null);
        res.setInviteeUserId(invite.getInvitee() != null ? invite.getInvitee().getId() : null);
        res.setInviteeEmail(invite.getInviteeEmail());
        res.setStatus(invite.getStatus());
        res.setExpiresAt(invite.getExpiresAt());
        res.setRespondedAt(invite.getRespondedAt());
        res.setMessage(invite.getMessage());
        res.setCreatedAt(invite.getCreatedAt());
        res.setUpdatedAt(invite.getUpdatedAt());
        return res;
    }
}
