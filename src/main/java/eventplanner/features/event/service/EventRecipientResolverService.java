package eventplanner.features.event.service;

import eventplanner.common.domain.enums.EventUserType;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.enums.RecipientType;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to resolve recipients for bulk notifications and reminders
 * Optimized for large-scale bulk sending
 * For SPECIFIC_PERSON, looks up users via public directory using user_id
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRecipientResolverService {

    private final EventUserRepository eventUserRepository;
    private final AttendeeRepository attendeeRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Resolve all recipients based on recipient types
     * 
     * @param eventId The event ID
     * @param recipientTypes List of recipient types to resolve
     * @param specificUserIds Specific user IDs if SPECIFIC_PERSON is included
     * @param specificEmails Specific emails if SPECIFIC_PERSON is included
     * @return RecipientInfo containing user IDs and emails
     */
    public RecipientInfo resolveRecipients(UUID eventId, 
                                           List<RecipientType> recipientTypes,
                                           List<UUID> specificUserIds,
                                           List<String> specificEmails) {
        if (recipientTypes == null || recipientTypes.isEmpty()) {
            // Backward compatibility: if no recipient types specified, use specific recipients
            return new RecipientInfo(
                specificUserIds != null ? new HashSet<>(specificUserIds) : new HashSet<>(),
                specificEmails != null ? new HashSet<>(specificEmails) : new HashSet<>()
            );
        }

        Set<UUID> allUserIds = new HashSet<>();
        Set<String> allEmails = new HashSet<>();

        for (RecipientType type : recipientTypes) {
            switch (type) {
                case ALL_COLLABORATORS:
                    RecipientInfo collaborators = resolveCollaborators(eventId);
                    allUserIds.addAll(collaborators.getUserIds());
                    allEmails.addAll(collaborators.getEmails());
                    break;
                case ALL_GUESTS:
                    RecipientInfo guests = resolveGuests(eventId);
                    allUserIds.addAll(guests.getUserIds());
                    allEmails.addAll(guests.getEmails());
                    break;
                case SPECIFIC_PERSON:
                    RecipientInfo specificPersons = resolveSpecificPersons(specificUserIds, specificEmails);
                    allUserIds.addAll(specificPersons.getUserIds());
                    allEmails.addAll(specificPersons.getEmails());
                    break;
            }
        }

        log.info("Resolved {} user IDs and {} emails for event {}", 
                allUserIds.size(), allEmails.size(), eventId);
        
        return new RecipientInfo(allUserIds, allEmails);
    }

    /**
     * Resolve all collaborators for an event
     * Includes ORGANIZER, COORDINATOR, COLLABORATOR, STAFF, etc.
     */
    private RecipientInfo resolveCollaborators(UUID eventId) {
        List<EventUserType> collaboratorTypes = Arrays.asList(
            EventUserType.ORGANIZER,
            EventUserType.COORDINATOR,
            EventUserType.COLLABORATOR,
            EventUserType.STAFF,
            EventUserType.SPEAKER,
            EventUserType.MEDIA,
            EventUserType.ADMIN
        );
        
        List<EventUser> collaborators = eventUserRepository.findByEventIdAndUserTypeIn(eventId, collaboratorTypes);
        
        Set<UUID> userIds = collaborators.stream()
            .map(eu -> eu.getUser() != null ? eu.getUser().getId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Set<String> emails = collaborators.stream()
            .map(eu -> eu.getUser() != null ? eu.getUser().getEmail() : null)
            .filter(Objects::nonNull)
            .filter(email -> !email.isBlank())
            .collect(Collectors.toSet());
        
        return new RecipientInfo(userIds, emails);
    }

    /**
     * Resolve all guests/attendees for an event
     */
    private RecipientInfo resolveGuests(UUID eventId) {
        List<Attendee> attendees = attendeeRepository.findByEventId(eventId);
        
        Set<UUID> userIds = new HashSet<>(); // Many attendees are email-only and lack linked user accounts
        
        Set<String> emails = attendees.stream()
            .map(Attendee::getEmail)
            .filter(Objects::nonNull)
            .filter(email -> !email.isBlank())
            .collect(Collectors.toSet());
        
        return new RecipientInfo(userIds, emails);
    }

    /**
     * Resolve specific persons by looking them up via public directory using user_id
     * Searches the public directory (UserAccountRepository) to find users and get their email addresses
     */
    private RecipientInfo resolveSpecificPersons(List<UUID> specificUserIds, List<String> specificEmails) {
        Set<UUID> userIds = new HashSet<>();
        Set<String> emails = new HashSet<>();

        // Add provided emails directly
        if (specificEmails != null) {
            emails.addAll(specificEmails.stream()
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet()));
        }

        // Look up users from public directory using user IDs
        if (specificUserIds != null && !specificUserIds.isEmpty()) {
            for (UUID userId : specificUserIds) {
                try {
                    Optional<UserAccount> userOpt = userAccountRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        UserAccount user = userOpt.get();
                        userIds.add(userId);
                        
                        // Get email from user account for sending notifications
                        if (user.getEmail() != null && !user.getEmail().isBlank()) {
                            emails.add(user.getEmail());
                        }
                        
                        log.debug("Resolved user {} from public directory: {}", userId, user.getEmail());
                    } else {
                        log.warn("User {} not found in public directory", userId);
                    }
                } catch (Exception e) {
                    log.error("Error looking up user {} in public directory: {}", userId, e.getMessage());
                }
            }
        }

        return new RecipientInfo(userIds, emails);
    }

    /**
     * Data class to hold resolved recipient information
     */
    public static class RecipientInfo {
        private final Set<UUID> userIds;
        private final Set<String> emails;

        public RecipientInfo(Set<UUID> userIds, Set<String> emails) {
            this.userIds = userIds != null ? userIds : new HashSet<>();
            this.emails = emails != null ? emails : new HashSet<>();
        }

        public Set<UUID> getUserIds() {
            return userIds;
        }

        public Set<String> getEmails() {
            return emails;
        }

        public int getTotalCount() {
            return userIds.size() + emails.size();
        }
    }
}
