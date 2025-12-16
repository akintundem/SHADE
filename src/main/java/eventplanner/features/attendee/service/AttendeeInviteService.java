package eventplanner.features.attendee.service;

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
import eventplanner.security.util.TokenHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class AttendeeInviteService {

    private final AttendeeInviteRepository inviteRepository;
    private final AttendeeRepository attendeeRepository;
    private final UserAccountRepository userAccountRepository;

    public AttendeeInviteService(
            AttendeeInviteRepository inviteRepository,
            AttendeeRepository attendeeRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.inviteRepository = inviteRepository;
        this.attendeeRepository = attendeeRepository;
        this.userAccountRepository = userAccountRepository;
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
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
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
            String tokenHash = TokenHashUtil.sha256(token.trim());
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
        if (invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt())) {
            invite.setStatus(AttendeeInviteStatus.EXPIRED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            throw new IllegalArgumentException("Invite has expired");
        }
        
        // Handle status update
        if (status == AttendeeInviteStatus.ACCEPTED) {
            return acceptInviteInternal(invite, principal);
        } else {
            // DECLINED, REVOKED, or EXPIRED
            invite.setStatus(status);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            return null;
        }
    }

    private Attendee acceptInviteInternal(AttendeeInvite invite, UserPrincipal principal) {
        // Double-check that the principal matches the invite (defense in depth)
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
        attendee.setRsvpStatus(AttendeeStatus.CONFIRMED);
        
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

}
