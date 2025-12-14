package eventplanner.features.collaboration.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.RegistrationStatus;
import eventplanner.features.collaboration.entity.EventCollaboratorInvite;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.enums.CollaboratorInviteStatus;
import eventplanner.features.collaboration.repository.EventCollaboratorInviteRepository;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.dto.request.CreateCollaboratorInviteRequest;
import eventplanner.features.event.dto.response.CollaboratorInviteResponse;
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
public class EventCollaboratorInviteService {

    private static final int DEFAULT_INVITE_TTL_DAYS = 14;

    private final EventCollaboratorInviteRepository inviteRepository;
    private final EventUserRepository eventUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    private final String appBaseUrl;

    public EventCollaboratorInviteService(
            EventCollaboratorInviteRepository inviteRepository,
            EventUserRepository eventUserRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.inviteRepository = inviteRepository;
        this.eventUserRepository = eventUserRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.appBaseUrl = appBaseUrl;
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

        // Prevent duplicate pending invites
        if (inviteeUserId != null) {
            Optional<EventCollaboratorInvite> existingPending = inviteRepository
                    .findFirstByEventIdAndInviteeUserIdAndStatusOrderByCreatedAtDesc(eventId, inviteeUserId, CollaboratorInviteStatus.PENDING);
            if (existingPending.isPresent()) {
                return toResponse(existingPending.get());
            }
        } else if (inviteeEmail != null) {
            Optional<EventCollaboratorInvite> existingPending = inviteRepository
                    .findFirstByEventIdAndInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(eventId, inviteeEmail, CollaboratorInviteStatus.PENDING);
            if (existingPending.isPresent()) {
                return toResponse(existingPending.get());
            }
        }

        EventCollaboratorInvite invite = new EventCollaboratorInvite();
        invite.setEventId(eventId);
        invite.setInviterUserId(inviter.getId());
        invite.setInviteeUserId(inviteeUserId);
        invite.setInviteeEmail(inviteeEmail);
        invite.setRole(request.getRole());
        invite.setStatus(CollaboratorInviteStatus.PENDING);
        invite.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(DEFAULT_INVITE_TTL_DAYS));
        invite.setMessage(safeTrim(request.getMessage()));

        // If we have an email, create a token-based acceptance flow.
        String rawToken = null;
        if (inviteeEmail != null) {
            rawToken = SecureTokenUtil.generateSecureToken();
            invite.setTokenHash(TokenHashUtil.sha256(rawToken));
        }

        EventCollaboratorInvite saved = inviteRepository.save(invite);

        // Notify invitee via push (in-app) if possible
        if (Boolean.TRUE.equals(request.getSendPush()) && saved.getInviteeUserId() != null) {
            sendPushInvite(saved);
        }

        // Notify invitee via email if requested and email is present
        if (Boolean.TRUE.equals(request.getSendEmail()) && saved.getInviteeEmail() != null) {
            sendEmailInvite(saved, rawToken, inviter.getUser());
        }

        return toResponse(saved);
    }

    public Page<CollaboratorInviteResponse> listEventInvites(UUID eventId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return inviteRepository.findByEventId(eventId, PageRequest.of(safePage, safeSize))
                .map(this::toResponse);
    }

    public List<CollaboratorInviteResponse> listMyPendingInvites(UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        UUID userId = principal.getId();
        String email = principal.getUser() != null ? principal.getUser().getEmail() : null;
        return inviteRepository.findIncomingInvites(userId, email, CollaboratorInviteStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EventUser acceptInviteById(UUID inviteId, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        EventCollaboratorInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        return acceptInvite(invite, principal);
    }

    public EventUser acceptInviteByToken(String token, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token is required");
        }
        String tokenHash = TokenHashUtil.sha256(token.trim());
        EventCollaboratorInvite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        return acceptInvite(invite, principal);
    }

    public void declineInvite(UUID inviteId, UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        EventCollaboratorInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

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

        UUID eventId = invite.getEventId();
        UUID userId = principal.getId();

        // If they already joined somehow, mark invite accepted and return existing membership.
        Optional<EventUser> existingMembership = eventUserRepository.findByEventIdAndUserId(eventId, userId);
        if (existingMembership.isPresent()) {
            invite.setStatus(CollaboratorInviteStatus.ACCEPTED);
            invite.setRespondedAt(LocalDateTime.now(ZoneOffset.UTC));
            inviteRepository.save(invite);
            return existingMembership.get();
        }

        EventUser membership = new EventUser();
        membership.setEventId(eventId);
        membership.setUserId(userId);
        membership.setEmail(principal.getUser() != null ? principal.getUser().getEmail() : invite.getInviteeEmail());
        membership.setName(principal.getUser() != null ? principal.getUser().getName() : null);
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
        UUID principalId = principal.getId();
        String principalEmail = principal.getUser() != null ? principal.getUser().getEmail() : null;

        boolean matchesUserId = invite.getInviteeUserId() != null && invite.getInviteeUserId().equals(principalId);
        boolean matchesEmail = invite.getInviteeEmail() != null
                && principalEmail != null
                && invite.getInviteeEmail().equalsIgnoreCase(principalEmail);

        if (!matchesUserId && !matchesEmail) {
            throw new IllegalArgumentException("Invite does not belong to authenticated user");
        }
    }

    private void sendPushInvite(EventCollaboratorInvite invite) {
        Map<String, Object> data = new HashMap<>();
        data.put("body", "You have been invited to collaborate on an event.");
        data.put("inviteId", invite.getId().toString());
        data.put("eventId", invite.getEventId().toString());
        data.put("role", invite.getRole() != null ? invite.getRole().name() : null);

        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.PUSH_NOTIFICATION)
                .to(invite.getInviteeUserId().toString())
                .subject("Event collaboration invite")
                .templateVariables(data)
                .eventId(invite.getEventId())
                .build());
    }

    private void sendEmailInvite(EventCollaboratorInvite invite, String rawToken, UserAccount inviterUser) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("inviterName", inviterUser != null ? inviterUser.getName() : "Someone");
        variables.put("eventId", invite.getEventId().toString());
        variables.put("role", invite.getRole() != null ? invite.getRole().name() : null);
        if (invite.getMessage() != null) {
            variables.put("customMessage", invite.getMessage());
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
                .eventId(invite.getEventId())
                .build());
    }

    private CollaboratorInviteResponse toResponse(EventCollaboratorInvite invite) {
        CollaboratorInviteResponse res = new CollaboratorInviteResponse();
        res.setInviteId(invite.getId());
        res.setEventId(invite.getEventId());
        res.setInviterUserId(invite.getInviterUserId());
        res.setInviteeUserId(invite.getInviteeUserId());
        res.setInviteeEmail(invite.getInviteeEmail());
        res.setRole(invite.getRole());
        res.setStatus(invite.getStatus());
        res.setExpiresAt(invite.getExpiresAt());
        res.setRespondedAt(invite.getRespondedAt());
        res.setMessage(invite.getMessage());
        res.setCreatedAt(invite.getCreatedAt());
        res.setUpdatedAt(invite.getUpdatedAt());
        return res;
    }
}

