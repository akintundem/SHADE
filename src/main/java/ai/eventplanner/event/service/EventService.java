package ai.eventplanner.event.service;

import ai.eventplanner.event.model.EventEntity;
import ai.eventplanner.event.repo.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "events", key = "#id")
    public Optional<EventEntity> getById(UUID id) {
        return repository.findById(id);
    }

    @CacheEvict(cacheNames = "events", key = "#result.id", condition = "#result != null")
    public EventEntity create(EventEntity entity) {
        EventEntity saved = repository.save(entity);
        // In monolithic mode, we can directly call services instead of using message queues
        return saved;
    }

    @CacheEvict(cacheNames = "events", key = "#id")
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}


