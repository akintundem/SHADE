package eventplanner.features.attendee.service;

import eventplanner.common.domain.enums.VisibilityLevel;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeInviteRepository;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;


@Service
@Transactional
public class AttendeeInviteService {

    private final AttendeeInviteRepository inviteRepository;
    private final AttendeeRepository attendeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    public AttendeeInviteService(
            AttendeeInviteRepository inviteRepository,
            AttendeeRepository attendeeRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService
    ) {
        this.inviteRepository = inviteRepository;
        this.attendeeRepository = attendeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
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
            throw new IllegalArgumentException("Authentication and user account information are required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        
        // Validate status transitions
        if (status == AttendeeInviteStatus.PENDING) {
            throw new IllegalArgumentException("Cannot set invite status back to PENDING");
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
            throw new IllegalArgumentException("Either inviteId or token must be provided");
        }
        
        // Verify the logged-in user is the one who can respond to this invite
        verifyInviteBelongsToPrincipal(invite, principal);
        
        // Check if invite is already in a final state
        if (invite.getStatus() != AttendeeInviteStatus.PENDING) {
            throw new IllegalArgumentException("Invite is not pending. Current status: " + invite.getStatus());
        }
        
        // Check expiry
        if (isExpired(invite)) {
            invite.setStatus(AttendeeInviteStatus.EXPIRED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            notifyOwnerOfInviteStatus(invite.getEvent(), principal.getUser(), AttendeeInviteStatus.EXPIRED);
            throw new IllegalArgumentException("Invite has expired");
        }
        
        // Handle status update
        if (status == AttendeeInviteStatus.ACCEPTED) {
            Attendee attendee = acceptInviteInternal(invite, principal);
            notifyOwnerOfInviteStatus(invite.getEvent(), principal.getUser(), AttendeeInviteStatus.ACCEPTED);
            return attendee;
        } else {
            // DECLINED, REVOKED, or EXPIRED
            invite.setStatus(status);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            notifyOwnerOfInviteStatus(invite.getEvent(), principal.getUser(), status);
            return null;
        }
    }

    private boolean isExpired(AttendeeInvite invite) {
        return invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt());
    }

    private Attendee acceptInviteInternal(AttendeeInvite invite, UserPrincipal principal) {
        Event event = invite.getEvent();
        if (event == null) {
            throw new IllegalArgumentException("Event not found for invite");
        }
        UUID eventId = event.getId();
        UUID userId = principal.getId();

        // If they already registered somehow, mark invite accepted and return existing attendee.
        Optional<Attendee> existingAttendee = attendeeRepository.findByEventIdAndUserId(eventId, userId);
        if (existingAttendee.isPresent()) {
            finalizeInvite(invite, AttendeeInviteStatus.ACCEPTED);
            return existingAttendee.get();
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
                    throw new IllegalArgumentException("An attendee with this email already exists for this event");
                }

                existing.setUser(user);
                if (existing.getName() == null || existing.getName().trim().isEmpty()) {
                    existing.setName(user.getName());
                }
                existing.setEmail(user.getEmail());
                existing.setRsvpStatus(AttendeeStatus.CONFIRMED);

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
        attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
        
        // Set name and email from user account if available
        if (user.getName() != null) {
            attendee.setName(user.getName());
        } else {
            throw new IllegalArgumentException("User name is required");
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

    private void verifyInviteBelongsToPrincipal(AttendeeInvite invite, UserPrincipal principal) {
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

}
