package ai.eventplanner.event.service;

import ai.eventplanner.common.domain.enums.EventStatus;
import ai.eventplanner.event.dto.request.CreateEventRequest;
import ai.eventplanner.event.dto.request.UpdateEventRequest;
import ai.eventplanner.event.dto.response.EventResponse;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "events", key = "#id")
    public Optional<Event> getById(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "events", key = "#result.id", condition = "#result != null")
    public Event create(CreateEventRequest request, UUID ownerIdFromHeader) {
        Event event = new Event();
        applyCreateRequest(event, request);

        if (ownerIdFromHeader == null) {
            throw new IllegalArgumentException("Owner ID is required to create an event");
        }

        event.setOwnerId(ownerIdFromHeader);

        return repository.save(event);
    }

    @Transactional
    @CacheEvict(cacheNames = "events", key = "#id")
    public Optional<Event> update(UUID id, UpdateEventRequest request) {
        return repository.findById(id).map(event -> {
            applyUpdateRequest(event, request);

            if (request.getVenueId() != null) {
                event.setVenueId(request.getVenueId());
            } else if (request.isVenueCleared()) {
                event.setVenueId(null);
            }

            return event;
        });
    }

    @CacheEvict(cacheNames = "events", key = "#id")
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public EventResponse toResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setEventType(event.getEventType());
        response.setEventStatus(event.getEventStatus());
        response.setStartDateTime(event.getStartDateTime());
        response.setEndDateTime(event.getEndDateTime());
        response.setRegistrationDeadline(event.getRegistrationDeadline());
        response.setCapacity(event.getCapacity());
        response.setCurrentAttendeeCount(event.getCurrentAttendeeCount());
        response.setIsPublic(event.getIsPublic());
        response.setRequiresApproval(event.getRequiresApproval());
        response.setQrCodeEnabled(event.getQrCodeEnabled());
        response.setQrCode(event.getQrCode());
        response.setCoverImageUrl(event.getCoverImageUrl());
        response.setEventWebsiteUrl(event.getEventWebsiteUrl());
        response.setHashtag(event.getHashtag());
        response.setTheme(event.getTheme());
        response.setObjectives(event.getObjectives());
        response.setTargetAudience(event.getTargetAudience());
        response.setSuccessMetrics(event.getSuccessMetrics());
        response.setBrandingGuidelines(event.getBrandingGuidelines());
        response.setVenueRequirements(event.getVenueRequirements());
        response.setTechnicalRequirements(event.getTechnicalRequirements());
        response.setAccessibilityFeatures(event.getAccessibilityFeatures());
        response.setEmergencyPlan(event.getEmergencyPlan());
        response.setBackupPlan(event.getBackupPlan());
        response.setPostEventTasks(event.getPostEventTasks());
        response.setMetadata(event.getMetadata());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setOwnerId(event.getOwnerId());
        response.setVenueId(event.getVenueId());
        return response;
    }

    private void applyCreateRequest(Event event, CreateEventRequest request) {
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setEventStatus(Optional.ofNullable(request.getEventStatus()).orElse(EventStatus.PLANNING));
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setCapacity(request.getCapacity());
        if (request.getCurrentAttendeeCount() != null) {
            event.setCurrentAttendeeCount(request.getCurrentAttendeeCount());
        }
        if (request.getIsPublic() != null) {
            event.setIsPublic(request.getIsPublic());
        }
        if (request.getRequiresApproval() != null) {
            event.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getQrCodeEnabled() != null) {
            event.setQrCodeEnabled(request.getQrCodeEnabled());
        }
        event.setQrCode(request.getQrCode());
        event.setCoverImageUrl(request.getCoverImageUrl());
        event.setEventWebsiteUrl(request.getEventWebsiteUrl());
        event.setHashtag(request.getHashtag());
        event.setTheme(request.getTheme());
        event.setObjectives(request.getObjectives());
        event.setTargetAudience(request.getTargetAudience());
        event.setSuccessMetrics(request.getSuccessMetrics());
        event.setBrandingGuidelines(request.getBrandingGuidelines());
        event.setVenueRequirements(request.getVenueRequirements());
        event.setTechnicalRequirements(request.getTechnicalRequirements());
        event.setAccessibilityFeatures(request.getAccessibilityFeatures());
        event.setEmergencyPlan(request.getEmergencyPlan());
        event.setBackupPlan(request.getBackupPlan());
        event.setPostEventTasks(request.getPostEventTasks());
        event.setMetadata(request.getMetadata());
    }

    private void applyUpdateRequest(Event event, UpdateEventRequest request) {
        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }
        if (request.getEventStatus() != null) {
            event.setEventStatus(request.getEventStatus());
        }
        if (request.getStartDateTime() != null) {
            event.setStartDateTime(request.getStartDateTime());
        }
        if (request.getEndDateTime() != null) {
            event.setEndDateTime(request.getEndDateTime());
        }
        if (request.getRegistrationDeadline() != null) {
            event.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getCapacity() != null) {
            event.setCapacity(request.getCapacity());
        }
        if (request.getCurrentAttendeeCount() != null) {
            event.setCurrentAttendeeCount(request.getCurrentAttendeeCount());
        }
        if (request.getIsPublic() != null) {
            event.setIsPublic(request.getIsPublic());
        }
        if (request.getRequiresApproval() != null) {
            event.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getQrCodeEnabled() != null) {
            event.setQrCodeEnabled(request.getQrCodeEnabled());
        }
        if (request.getQrCode() != null) {
            event.setQrCode(request.getQrCode());
        }
        if (request.getCoverImageUrl() != null) {
            event.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getEventWebsiteUrl() != null) {
            event.setEventWebsiteUrl(request.getEventWebsiteUrl());
        }
        if (request.getHashtag() != null) {
            event.setHashtag(request.getHashtag());
        }
        if (request.getTheme() != null) {
            event.setTheme(request.getTheme());
        }
        if (request.getObjectives() != null) {
            event.setObjectives(request.getObjectives());
        }
        if (request.getTargetAudience() != null) {
            event.setTargetAudience(request.getTargetAudience());
        }
        if (request.getSuccessMetrics() != null) {
            event.setSuccessMetrics(request.getSuccessMetrics());
        }
        if (request.getBrandingGuidelines() != null) {
            event.setBrandingGuidelines(request.getBrandingGuidelines());
        }
        if (request.getVenueRequirements() != null) {
            event.setVenueRequirements(request.getVenueRequirements());
        }
        if (request.getTechnicalRequirements() != null) {
            event.setTechnicalRequirements(request.getTechnicalRequirements());
        }
        if (request.getAccessibilityFeatures() != null) {
            event.setAccessibilityFeatures(request.getAccessibilityFeatures());
        }
        if (request.getEmergencyPlan() != null) {
            event.setEmergencyPlan(request.getEmergencyPlan());
        }
        if (request.getBackupPlan() != null) {
            event.setBackupPlan(request.getBackupPlan());
        }
        if (request.getPostEventTasks() != null) {
            event.setPostEventTasks(request.getPostEventTasks());
        }
        if (request.getMetadata() != null) {
            event.setMetadata(request.getMetadata());
        }
        if (request.getOwnerId() != null) {
            event.setOwnerId(request.getOwnerId());
        }
    }
}
