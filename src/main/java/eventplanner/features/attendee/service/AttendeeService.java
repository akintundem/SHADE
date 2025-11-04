package eventplanner.features.attendee.service;

import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
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

    public List<Attendee> addAll(List<Attendee> attendees) {
        return repository.saveAll(attendees);
    }

    public List<Attendee> listByEvent(UUID eventId) {
        return repository.findByEventId(eventId);
    }

    public Optional<Attendee> updateRsvp(UUID attendeeId, String status) {
        return repository.findById(attendeeId).map(a -> {
            a.setRsvpStatus(status);
            return repository.save(a);
        });
    }

    public Optional<Attendee> checkIn(UUID attendeeId) {
        return repository.findById(attendeeId).map(a -> {
            a.setCheckedInAt(LocalDateTime.now());
            return repository.save(a);
        });
    }
}


