package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.request.AttendeeInfo;
import eventplanner.features.attendee.dto.request.BulkAttendeeCreateRequest;
import eventplanner.features.attendee.dto.response.AttendeeResponse;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
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
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;

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
    public Page<Attendee> listByEventAndStatus(UUID eventId, Attendee.Status status, Pageable pageable) {
        return repository.findByEventIdAndRsvpStatus(eventId, status, pageable);
    }
    
    /**
     * List attendees filtered by multiple statuses with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> listByEventAndStatuses(UUID eventId, List<Attendee.Status> statuses, Pageable pageable) {
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
            if (updatedAttendee.getRsvpStatus() != null) {
                existing.setRsvpStatus(updatedAttendee.getRsvpStatus());
            }
            if (updatedAttendee.getUser() != null) {
                existing.setUser(updatedAttendee.getUser());
            }
            
            Attendee saved = repository.save(existing);
            log.info("Updated attendee {}", attendeeId);
            return saved;
        });
    }

    /**
     * Update RSVP status
     */
    public Optional<Attendee> updateRsvp(UUID attendeeId, Attendee.Status status) {
        validateStatus(status);
        
        return repository.findById(attendeeId).map(a -> {
            Attendee.Status oldStatus = a.getRsvpStatus();
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
                .userId(attendee.getUser() != null ? attendee.getUser().getId() : null)
                .name(attendee.getName())
                .email(attendee.getEmail())
                .rsvpStatus(attendee.getRsvpStatus())
                .checkedInAt(attendee.getCheckedInAt())
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
    
    private void validateStatus(Attendee.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
    }
    
    private boolean isValidEmail(String email) {
        // Basic email validation
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    /**
     * Create attendees from bulk request, resolving user accounts and validating
     */
    public List<Attendee> createFromBulkRequest(BulkAttendeeCreateRequest request) {
        if (request == null || request.getEventId() == null) {
            throw new IllegalArgumentException("Invalid request: eventId is required");
        }
        
        if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
            throw new IllegalArgumentException("Attendees list cannot be empty");
        }
        
        // Fetch the event
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.getEventId()));
        
        // Map attendee info to Attendee entities, resolving userId or email
        List<Attendee> toSave = request.getAttendees().stream().map(attendeeInfo -> {
            Attendee attendee = new Attendee();
            attendee.setEvent(event);
            
            // If userId is provided, fetch user account and auto-fill info
            if (attendeeInfo.getUserId() != null) {
                UserAccount user = userAccountRepository.findById(attendeeInfo.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with userId: " + attendeeInfo.getUserId()));
                
                // Set user relationship
                attendee.setUser(user);
                
                // Auto-fill from user account (can be overridden)
                attendee.setName(attendeeInfo.getName() != null && !attendeeInfo.getName().trim().isEmpty() 
                    ? attendeeInfo.getName() : user.getName());
                attendee.setEmail(attendeeInfo.getEmail() != null && !attendeeInfo.getEmail().trim().isEmpty()
                    ? attendeeInfo.getEmail() : user.getEmail());
            } else {
                // Use email - validate that either userId or email is provided
                if (attendeeInfo.getEmail() == null || attendeeInfo.getEmail().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Either userId or email must be provided for each attendee");
                }
                
                // Try to resolve userId from email (optional - if user exists, link them)
                Optional<UserAccount> userByEmail = userAccountRepository.findByEmailIgnoreCase(attendeeInfo.getEmail());
                if (userByEmail.isPresent()) {
                    UserAccount user = userByEmail.get();
                    // Link user account
                    attendee.setUser(user);
                    // Auto-fill name if not provided
                    attendee.setName(attendeeInfo.getName() != null && !attendeeInfo.getName().trim().isEmpty()
                        ? attendeeInfo.getName() : user.getName());
                } else {
                    // User doesn't exist - use provided info, no user link
                    attendee.setUser(null);
                    if (attendeeInfo.getName() == null || attendeeInfo.getName().trim().isEmpty()) {
                        throw new IllegalArgumentException(
                            "Name is required when email is provided and user doesn't exist in directory");
                    }
                    attendee.setName(attendeeInfo.getName());
                }
                
                attendee.setEmail(attendeeInfo.getEmail());
            }
            
            return attendee;
        }).collect(Collectors.toList());
        
        return addAll(toSave);
    }
    
    /**
     * Get attendee by ID with event validation
     */
    @Transactional(readOnly = true)
    public Attendee getAttendeeById(UUID attendeeId) {
        return repository.findById(attendeeId)
            .orElseThrow(() -> new IllegalArgumentException("Attendee not found: " + attendeeId));
    }
    
    /**
     * Filter and list attendees with pagination
     */
    @Transactional(readOnly = true)
    public Page<Attendee> filterAttendees(
            UUID eventId,
            String status,
            Boolean checkedIn,
            String search,
            UUID userId,
            String email,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        
        // Validate and normalize pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Max page size
        
        // Create sort
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Apply filters in priority order
        if (StringUtils.hasText(search)) {
            return searchAttendees(eventId, search.trim(), pageable);
        }
        
        if (checkedIn != null) {
            if (checkedIn) {
                return listCheckedIn(eventId, pageable);
            } else {
                return repository.findNotCheckedInByEventId(eventId, pageable);
            }
        }
        
        if (StringUtils.hasText(status)) {
            List<Attendee.Status> statuses = parseStatuses(status);
            if (statuses.isEmpty()) {
                throw new IllegalArgumentException("Invalid status values");
            }
            return listByEventAndStatuses(eventId, statuses, pageable);
        }
        
        if (userId != null) {
            Optional<Attendee> attendeeOpt = repository.findByEventIdAndUserId(eventId, userId);
            if (attendeeOpt.isPresent()) {
                return new PageImpl<>(
                    Collections.singletonList(attendeeOpt.get()), 
                    pageable, 
                    1
                );
            } else {
                return Page.empty(pageable);
            }
        }
        
        if (StringUtils.hasText(email)) {
            Optional<Attendee> attendeeOpt = repository.findByEventIdAndEmail(eventId, email.trim());
            if (attendeeOpt.isPresent()) {
                return new PageImpl<>(
                    Collections.singletonList(attendeeOpt.get()), 
                    pageable, 
                    1
                );
            } else {
                return Page.empty(pageable);
            }
        }
        
        // No filters - return all
        return listByEventPaginated(eventId, pageable);
    }
    
    /**
     * Parse comma-separated status values
     */
    public List<Attendee.Status> parseStatuses(String statusParam) {
        if (!StringUtils.hasText(statusParam)) {
            return Collections.emptyList();
        }
        
        List<Attendee.Status> statuses = new ArrayList<>();
        String[] parts = statusParam.toUpperCase().split(",");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (Attendee.Status.isValid(trimmed)) {
                statuses.add(Attendee.Status.fromString(trimmed));
            }
        }
        
        return statuses;
    }
}


