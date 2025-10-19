package ai.eventplanner.attendee.service;

import ai.eventplanner.attendee.model.AttendeeEntity;
import ai.eventplanner.attendee.repo.AttendeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendeeService {
    private final AttendeeRepository repository;

    public AttendeeService(AttendeeRepository repository) {
        this.repository = repository;
    }

    public List<AttendeeEntity> addAll(List<AttendeeEntity> attendees) {
        return repository.saveAll(attendees);
    }

    public List<AttendeeEntity> listByEvent(UUID eventId) {
        return repository.findByEventId(eventId);
    }

    public Optional<AttendeeEntity> updateRsvp(UUID attendeeId, String status) {
        return repository.findById(attendeeId).map(a -> {
            a.setRsvpStatus(status);
            return repository.save(a);
        });
    }

    public Optional<AttendeeEntity> checkIn(UUID attendeeId) {
        return repository.findById(attendeeId).map(a -> {
            a.setCheckedInAt(LocalDateTime.now());
            return repository.save(a);
        });
    }
}


