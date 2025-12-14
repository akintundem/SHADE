package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.entity.AttendeeStatus;
import eventplanner.features.attendee.repository.AttendeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for attendee management with CRUD operations, pagination, and validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendeeService {
    
    private final AttendeeRepository repository;

    /**
     * Create a single attendee
     */
    public Attendee create(Attendee attendee) {
        validateAttendee(attendee);
        
        // Check for duplicate email in the same event
        if (attendee.getEmail() != null && !attendee.getEmail().isEmpty() && attendee.getEvent() != null) {
            UUID eventId = attendee.getEvent().getId();
            Optional<Attendee> existing = repository.findByEventIdAndEmail(eventId, attendee.getEmail());
            if (existing.isPresent()) {
                log.warn("Duplicate attendee detected for email {} in event {}", attendee.getEmail(), eventId);
                throw new IllegalArgumentException("An attendee with this email already exists for this event");
            }
        }
        
        Attendee saved = repository.save(attendee);
        log.info("Created attendee {} for event {}", saved.getId(), saved.getEvent() != null ? saved.getEvent().getId() : null);
        return saved;
    }
    
    /**
     * Bulk create attendees
     */
    public List<Attendee> addAll(List<Attendee> attendees) {
        // Validate all attendees
        attendees.forEach(this::validateAttendee);
        
        List<Attendee> saved = repository.saveAll(attendees);
        log.info("Created {} attendees", saved.size());
        return saved;
    }
    
    /**
     * Get attendee by ID
     */
    @Transactional(readOnly = true)
    public Optional<Attendee> getById(UUID attendeeId) {
        return repository.findById(attendeeId);
    }
    
    /**
     * Get attendee by ID and event ID (for authorization)
     */
    @Transactional(readOnly = true)
    public Optional<Attendee> getByIdAndEventId(UUID attendeeId, UUID eventId) {
        return repository.findByIdAndEventId(attendeeId, eventId);
    }
    
    /**
     * List all attendees for an event (no pagination)
     */
    @Transactional(readOnly = true)
    public List<Attendee> listByEvent(UUID eventId) {
        return repository.findByEventId(eventId);
    }
    
    /**
     * List attendees with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> listByEventPaginated(UUID eventId, Pageable pageable) {
        return repository.findByEventId(eventId, pageable);
    }
    
    /**
     * List attendees filtered by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> listByEventAndStatus(UUID eventId, AttendeeStatus status, Pageable pageable) {
        return repository.findByEventIdAndRsvpStatus(eventId, status, pageable);
    }
    
    /**
     * List attendees filtered by multiple statuses with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> listByEventAndStatuses(UUID eventId, List<AttendeeStatus> statuses, Pageable pageable) {
        return repository.findByEventIdAndRsvpStatusIn(eventId, statuses, pageable);
    }
    
    /**
     * List checked-in attendees with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> listCheckedIn(UUID eventId, Pageable pageable) {
        return repository.findCheckedInByEventId(eventId, pageable);
    }
    
    /**
     * Search attendees by name or email with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> searchAttendees(UUID eventId, String search, Pageable pageable) {
        return repository.searchByEventIdAndEmailOrName(eventId, search, pageable);
    }
    
    /**
     * Update attendee
     */
    public Optional<Attendee> update(UUID attendeeId, Attendee updatedAttendee) {
        return repository.findById(attendeeId).map(existing -> {
            // Update fields
            if (updatedAttendee.getName() != null) {
                existing.setName(updatedAttendee.getName());
            }
            if (updatedAttendee.getEmail() != null) {
                existing.setEmail(updatedAttendee.getEmail());
            }
            if (updatedAttendee.getPhone() != null) {
                existing.setPhone(updatedAttendee.getPhone());
            }
            if (updatedAttendee.getRsvpStatus() != null) {
                existing.setRsvpStatus(updatedAttendee.getRsvpStatus());
            }
            if (updatedAttendee.getEmailConsent() != null) {
                existing.setEmailConsent(updatedAttendee.getEmailConsent());
            }
            if (updatedAttendee.getSmsConsent() != null) {
                existing.setSmsConsent(updatedAttendee.getSmsConsent());
            }
            if (updatedAttendee.getDataProcessingConsent() != null) {
                existing.setDataProcessingConsent(updatedAttendee.getDataProcessingConsent());
            }
            
            Attendee saved = repository.save(existing);
            log.info("Updated attendee {}", attendeeId);
            return saved;
        });
    }

    /**
     * Update RSVP status
     */
    public Optional<Attendee> updateRsvp(UUID attendeeId, AttendeeStatus status) {
        validateStatus(status);
        
        return repository.findById(attendeeId).map(a -> {
            AttendeeStatus oldStatus = a.getRsvpStatus();
            a.setRsvpStatus(status);
            Attendee saved = repository.save(a);
            log.info("Updated RSVP status for attendee {} from {} to {}", attendeeId, oldStatus, status);
            return saved;
        });
    }

    /**
     * Check-in attendee
     */
    public Optional<Attendee> checkIn(UUID attendeeId) {
        return repository.findById(attendeeId).map(a -> {
            if (a.getCheckedInAt() != null) {
                log.warn("Attendee {} already checked in at {}", attendeeId, a.getCheckedInAt());
                return a; // Already checked in - idempotent
            }
            
            a.setCheckedInAt(LocalDateTime.now());
            Attendee saved = repository.save(a);
            log.info("Checked in attendee {}", attendeeId);
            return saved;
        });
    }
    
    /**
     * Delete attendee
     */
    public boolean delete(UUID attendeeId) {
        if (repository.existsById(attendeeId)) {
            repository.deleteById(attendeeId);
            log.info("Deleted attendee {}", attendeeId);
            return true;
        }
        return false;
    }
    
    /**
     * Get attendee count for an event
     */
    @Transactional(readOnly = true)
    public long countByEvent(UUID eventId) {
        return repository.countByEventId(eventId);
    }
    
    /**
     * Get checked-in count for an event
     */
    @Transactional(readOnly = true)
    public long countCheckedIn(UUID eventId) {
        return repository.countCheckedInByEventId(eventId);
    }
    
    /**
     * Convert Attendee entity to AttendeeResponse DTO
     * Note: isCheckedIn is computed dynamically from checkedInAt in the response
     */
    public AttendeeResponse toResponse(Attendee attendee) {
        return AttendeeResponse.builder()
                .id(attendee.getId())
                .eventId(attendee.getEvent() != null ? attendee.getEvent().getId() : null)
                .name(attendee.getName())
                .email(attendee.getEmail())
                .phone(attendee.getPhone())
                .rsvpStatus(attendee.getRsvpStatus())
                .checkedInAt(attendee.getCheckedInAt())
                .emailConsent(attendee.getEmailConsent())
                .smsConsent(attendee.getSmsConsent())
                .dataProcessingConsent(attendee.getDataProcessingConsent())
                .createdAt(attendee.getCreatedAt())
                .updatedAt(attendee.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert page of attendees to page of responses
     */
    public Page<AttendeeResponse> toResponsePage(Page<Attendee> attendees) {
        return attendees.map(this::toResponse);
    }
    
    /**
     * Convert list of attendees to list of responses
     */
    public List<AttendeeResponse> toResponseList(List<Attendee> attendees) {
        return attendees.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    // Validation methods
    
    private void validateAttendee(Attendee attendee) {
        if (attendee.getEvent() == null) {
            throw new IllegalArgumentException("Event is required");
        }
        if (attendee.getName() == null || attendee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Attendee name is required");
        }
        // Email is optional but should be valid if provided
        if (attendee.getEmail() != null && !attendee.getEmail().isEmpty()) {
            if (!isValidEmail(attendee.getEmail())) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
    }
    
    private void validateStatus(AttendeeStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
    }
    
    private boolean isValidEmail(String email) {
        // Basic email validation
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}


