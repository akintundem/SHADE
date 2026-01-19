package eventplanner.features.collaboration.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.features.event.enums.EventUserType;
import eventplanner.features.collaboration.enums.RegistrationStatus;
import eventplanner.features.collaboration.entity.EventCollaboratorInvite;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.enums.CollaboratorInviteStatus;
import eventplanner.features.collaboration.repository.EventCollaboratorInviteRepository;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.dto.request.CreateCollaboratorInviteRequest;
import eventplanner.features.event.dto.response.CollaboratorInviteResponse;
import eventplanner.common.util.UserAccountUtil;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.util.TokenUtil;
import eventplanner.features.config.AppProperties;
import eventplanner.common.config.ExternalServicesProperties;
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
public class EventCollaboratorInviteService {

    private static final int DEFAULT_INVITE_TTL_DAYS = 14;

    private final EventCollaboratorInviteRepository inviteRepository;
    private final EventUserRepository eventUserRepository;
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final ExternalServicesProperties externalServicesProperties;

    private final String appBaseUrl;

    public EventCollaboratorInviteService(
            EventCollaboratorInviteRepository inviteRepository,
            EventUserRepository eventUserRepository,
            EventRepository eventRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService,
            ExternalServicesProperties externalServicesProperties,
            AppProperties appProperties
    ) {
        this.inviteRepository = inviteRepository;
        this.eventUserRepository = eventUserRepository;
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.externalServicesProperties = externalServicesProperties;
        this.appBaseUrl = requireConfigured(appProperties.getBaseUrl(), "app.base-url");
    }

    public CollaboratorInviteResponse createInvite(UUID eventId, UserPrincipal inviter, CreateCollaboratorInviteRequest request) {
        if (inviter == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("role is required");
        }

        UUID inviteeUserId = request.getInviteeUserId();
        String inviteeEmail = safeTrim(request.getInviteeEmail());
        if ((inviteeUserId == null) && (inviteeEmail == null || inviteeEmail.isBlank())) {
            throw new IllegalArgumentException("Either inviteeUserId or inviteeEmail must be provided");
        }
        if (inviteeUserId == null && inviteeEmail != null && Boolean.FALSE.equals(request.getSendEmail())) {
            throw new IllegalArgumentException("Email delivery is required when inviting by email");
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

        // Prevent inviting someone already on the event
        if (inviteeUserId != null) {
            if (eventUserRepository.findByEventIdAndUserId(eventId, inviteeUserId).isPresent()) {
                throw new IllegalArgumentException("User is already a collaborator on this event");
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
                .orElse(null); // Invitee account may be absent when inviting by email only
        }
        
        // Reuse existing pending invite if present, otherwise create new
        EventCollaboratorInvite invite = null;
        String rawToken = null;
        if (inviteeUserId != null) {
            invite = inviteRepository
                    .findFirstByEventIdAndInviteeUserIdAndStatusOrderByCreatedAtDesc(eventId, inviteeUserId, CollaboratorInviteStatus.PENDING)
                    .orElse(null);
        } else if (inviteeEmail != null) {
            invite = inviteRepository
                    .findFirstByEventIdAndInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(eventId, inviteeEmail, CollaboratorInviteStatus.PENDING)
                    .orElse(null);
        }

        if (invite == null) {
            invite = new EventCollaboratorInvite();
            invite.setEvent(event);
        }

        invite.setInviter(inviterUser);
        invite.setEvent(event);
        if (inviteeUser != null) {
            invite.setInvitee(inviteeUser);
        }
        invite.setInviteeEmail(inviteeEmail);
        invite.setRole(request.getRole());
        invite.setStatus(CollaboratorInviteStatus.PENDING);
        invite.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(DEFAULT_INVITE_TTL_DAYS));
        invite.setMessage(safeTrim(request.getMessage()));

        // Reset status/response for refreshed invites
        invite.setStatus(CollaboratorInviteStatus.PENDING);
        invite.setRespondedAt(null);

        // If we have an email, create a token-based acceptance flow.
        if (inviteeEmail != null) {
            rawToken = TokenUtil.generateToken();
            invite.setTokenHash(TokenUtil.hashToken(rawToken));
        } else {
            invite.setTokenHash(null);
        }

        EventCollaboratorInvite saved = inviteRepository.save(invite);

        // Notify invitee via push (in-app) if possible
        if (Boolean.TRUE.equals(request.getSendPush()) && saved.getInvitee() != null) {
            sendPushInvite(saved);
        }

        // Notify invitee via email if requested and email is present
        if (Boolean.TRUE.equals(request.getSendEmail()) && saved.getInviteeEmail() != null && rawToken != null) {
            sendEmailInvite(saved, rawToken, inviter.getUser());
        }

        return CollaboratorInviteResponse.from(saved);
    }

    public Page<CollaboratorInviteResponse> listEventInvites(UUID eventId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return inviteRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize))
                .map(CollaboratorInviteResponse::from);
    }

    public List<CollaboratorInviteResponse> listMyPendingInvites(UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        UUID userId = principal.getId();
        String email = principal.getUser() != null ? principal.getUser().getEmail() : null;
        return inviteRepository.findIncomingInvites(userId, email, CollaboratorInviteStatus.PENDING)
                .stream()
                .map(CollaboratorInviteResponse::from)
                .toList();
    }

    public EventUser acceptInviteById(UUID inviteId, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
        }
        
        EventCollaboratorInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        // Verify the logged-in user is the one who can accept this invite
        verifyInviteBelongsToPrincipal(invite, principal);
        return acceptInvite(invite, principal);
    }

    public EventUser acceptInviteByToken(String token, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (principal.getUser() == null) {
            throw new IllegalArgumentException("User account information is required");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token is required");
        }
        
        String tokenHash = TokenUtil.hashToken(token.trim());
        EventCollaboratorInvite invite = inviteRepository.findByTokenHash(tokenHash)
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
        EventCollaboratorInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

        // Verify the logged-in user is the one who can decline this invite
        verifyInviteBelongsToPrincipal(invite, principal);

        if (invite.getStatus() != CollaboratorInviteStatus.PENDING) {
            throw new IllegalArgumentException("Invite is not pending");
        }
        if (invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt())) {
            invite.setStatus(CollaboratorInviteStatus.EXPIRED);
        } else {
            invite.setStatus(CollaboratorInviteStatus.DECLINED);
        }
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);
    }

    public void revokeInvite(UUID inviteId) {
        EventCollaboratorInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

        if (invite.getStatus() != CollaboratorInviteStatus.PENDING) {
            throw new IllegalArgumentException("Only pending invites can be revoked");
        }

        invite.setStatus(CollaboratorInviteStatus.REVOKED);
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);
    }

    private EventUser acceptInvite(EventCollaboratorInvite invite, UserPrincipal principal) {
        // Double-check that the principal matches the invite (defense in depth)
        // This is already verified in acceptInviteById/acceptInviteByToken, but we check again here
        verifyInviteBelongsToPrincipal(invite, principal);

        if (invite.getStatus() != CollaboratorInviteStatus.PENDING) {
            throw new IllegalArgumentException("Invite is not pending");
        }

        // Expiry check
        if (invite.getExpiresAt() != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(invite.getExpiresAt())) {
            invite.setStatus(CollaboratorInviteStatus.EXPIRED);
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

        // If they already joined somehow, mark invite accepted and return existing membership.
        Optional<EventUser> existingMembership = eventUserRepository.findByEventIdAndUserId(eventId, userId);
        if (existingMembership.isPresent()) {
            invite.setStatus(CollaboratorInviteStatus.ACCEPTED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            return existingMembership.get();
        }

        // Fetch UserAccount for membership
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        EventUser membership = new EventUser();
        membership.setEvent(event);
        membership.setUser(user);
        membership.setUserType(invite.getRole() != null ? invite.getRole() : EventUserType.COLLABORATOR);
        membership.setRegistrationStatus(RegistrationStatus.CONFIRMED);
        membership.setRegistrationDate(LocalDateTime.now(ZoneOffset.UTC));

        EventUser savedMembership = eventUserRepository.save(membership);

        invite.setStatus(CollaboratorInviteStatus.ACCEPTED);
        invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
        inviteRepository.save(invite);

        return savedMembership;
    }

    private void verifyInviteBelongsToPrincipal(EventCollaboratorInvite invite, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        UUID principalId = principal.getId();
        String principalEmail = principal.getUser() != null ? principal.getUser().getEmail() : null;

        if (principalId == null) {
            throw new IllegalArgumentException("User ID is required");
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

    private void sendPushInvite(EventCollaboratorInvite invite) {
        Event event = invite.getEvent();
        if (event == null) {
            return; // Cannot send notification without event
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("body", "You have been invited to collaborate on an event.");
        data.put("inviteId", invite.getId().toString());
        data.put("eventId", event.getId().toString());
        data.put("role", invite.getRole() != null ? invite.getRole().name() : null);

        UUID inviteeUserId = invite.getInvitee() != null ? invite.getInvitee().getId() : null;
        if (inviteeUserId == null) {
            return; // Cannot send push without user ID
        }

        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.PUSH_NOTIFICATION)
                .to(inviteeUserId.toString())
                .subject("Event collaboration invite")
                .templateVariables(data)
                .eventId(event.getId())
                .build());
    }

    private void sendEmailInvite(EventCollaboratorInvite invite, String rawToken, UserAccount inviterUser) {
        Event event = invite.getEvent();
        if (event == null) {
            return; // Cannot send notification without event
        }
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("inviterName", inviterUser != null ? inviterUser.getName() : "Someone");
        variables.put("inviteeName", invite.getInvitee() != null && invite.getInvitee().getName() != null
                ? invite.getInvitee().getName()
                : invite.getInviteeEmail());
        variables.put("eventName", event.getName());
        variables.put("eventId", event.getId().toString());
        variables.put("role", invite.getRole() != null ? invite.getRole().name() : null);
        if (invite.getMessage() != null) {
            variables.put("message", invite.getMessage());
        }

        String acceptUrl = appBaseUrl + "/api/v1/collaborator-invites/accept?token=" + rawToken;
        variables.put("acceptUrl", acceptUrl);

        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.EMAIL)
                .to(invite.getInviteeEmail())
                .subject("You're invited to collaborate on an event")
                // Reuse existing template to avoid missing template failures.
                .templateId("event-invitation")
                .templateVariables(variables)
                .eventId(event.getId())
                .from(externalServicesProperties.getEmail().getFromEvents())
                .build());
    }

    private static String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
        return value;
    }

}
