package ai.eventplanner.timeline.service;

import ai.eventplanner.timeline.model.TimelineItemEntity;
import ai.eventplanner.timeline.repo.TimelineItemRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TimelineService {
    private final TimelineItemRepository repository;

    public TimelineService(TimelineItemRepository repository) { this.repository = repository; }

    public List<TimelineItemEntity> list(UUID eventId) {
        return repository.findByEventIdOrderByScheduledAtAsc(eventId);
    }

    public TimelineItemEntity create(TimelineItemEntity item) {
        validateDependencies(item);
        return repository.save(item);
    }

    public void delete(UUID id) { repository.deleteById(id); }

    private void validateDependencies(TimelineItemEntity item) {
        UUID[] deps = item.getDependencies();
        if (deps == null || deps.length == 0) return;
        Set<UUID> seen = new HashSet<>();
        for (UUID dep : deps) {
            if (dep == null) continue;
            if (dep.equals(item.getId())) throw new IllegalArgumentException("Item cannot depend on itself");
            if (!seen.add(dep)) throw new IllegalArgumentException("Duplicate dependency: " + dep);
        }
    }
}


