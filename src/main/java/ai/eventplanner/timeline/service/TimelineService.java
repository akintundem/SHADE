package ai.eventplanner.timeline.service;

import ai.eventplanner.timeline.dto.TimelineItemCreateRequest;
import ai.eventplanner.timeline.dto.request.TimelineItemUpdateRequest;
import ai.eventplanner.timeline.dto.request.TimelineBulkCreateRequest;
import ai.eventplanner.timeline.dto.request.WorkbackRequest;
import ai.eventplanner.timeline.dto.response.TimelineItemResponse;
import ai.eventplanner.timeline.dto.response.TimelineSummaryResponse;
import ai.eventplanner.timeline.dto.response.WorkbackMilestone;
import ai.eventplanner.timeline.model.TimelineItemEntity;
import ai.eventplanner.timeline.repo.TimelineItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimelineService {
    private final TimelineItemRepository repository;

    public TimelineService(TimelineItemRepository repository) { 
        this.repository = repository; 
    }

    // ==================== BASIC CRUD OPERATIONS ====================

    public List<TimelineItemResponse> getTimelineByEventId(UUID eventId) {
        List<TimelineItemEntity> entities = repository.findByEventIdOrderByScheduledAtAsc(eventId);
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TimelineItemResponse createTimelineItem(TimelineItemCreateRequest request) {
        TimelineItemEntity entity = new TimelineItemEntity();
        entity.setEventId(request.getEventId());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setScheduledAt(request.getScheduledAt());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setItemType(request.getItemType());
        entity.setPriority(request.getPriority());
        entity.setLocation(request.getLocation());
        entity.setAssignedTo(request.getAssignedTo());
        entity.setDependencies(request.getDependencies());
        entity.setSetupTimeMinutes(request.getSetupTimeMinutes());
        entity.setTeardownTimeMinutes(request.getTeardownTimeMinutes());
        entity.setResourcesRequired(request.getResourcesRequired());
        entity.setNotes(request.getNotes());
        
        validateDependencies(entity);
        TimelineItemEntity saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public List<TimelineItemResponse> createBulkTimelineItems(TimelineBulkCreateRequest request) {
        List<TimelineItemEntity> entities = new ArrayList<>();
        
        for (TimelineItemCreateRequest itemRequest : request.getItems()) {
            TimelineItemEntity entity = new TimelineItemEntity();
            entity.setEventId(itemRequest.getEventId());
            entity.setTitle(itemRequest.getTitle());
            entity.setDescription(itemRequest.getDescription());
            entity.setScheduledAt(itemRequest.getScheduledAt());
            entity.setDurationMinutes(itemRequest.getDurationMinutes());
            entity.setItemType(itemRequest.getItemType());
            entity.setPriority(itemRequest.getPriority());
            entity.setLocation(itemRequest.getLocation());
            entity.setAssignedTo(itemRequest.getAssignedTo());
            entity.setDependencies(itemRequest.getDependencies());
            entity.setSetupTimeMinutes(itemRequest.getSetupTimeMinutes());
            entity.setTeardownTimeMinutes(itemRequest.getTeardownTimeMinutes());
            entity.setResourcesRequired(itemRequest.getResourcesRequired());
            entity.setNotes(itemRequest.getNotes());
            
            validateDependencies(entity);
            entities.add(entity);
        }
        
        List<TimelineItemEntity> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TimelineItemResponse updateTimelineItem(UUID itemId, TimelineItemUpdateRequest request) {
        TimelineItemEntity entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Timeline item not found"));
        
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getScheduledAt() != null) entity.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) entity.setDurationMinutes(request.getDurationMinutes());
        if (request.getItemType() != null) entity.setItemType(request.getItemType());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getLocation() != null) entity.setLocation(request.getLocation());
        if (request.getAssignedTo() != null) entity.setAssignedTo(request.getAssignedTo());
        if (request.getDependencies() != null) entity.setDependencies(request.getDependencies());
        if (request.getSetupTimeMinutes() != null) entity.setSetupTimeMinutes(request.getSetupTimeMinutes());
        if (request.getTeardownTimeMinutes() != null) entity.setTeardownTimeMinutes(request.getTeardownTimeMinutes());
        if (request.getResourcesRequired() != null) entity.setResourcesRequired(request.getResourcesRequired());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());
        
        validateDependencies(entity);
        TimelineItemEntity saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public void deleteTimelineItem(UUID itemId) {
        // Check if this item is a dependency for other items
        List<TimelineItemEntity> dependentItems = repository.findByDependenciesContaining(itemId);
        if (!dependentItems.isEmpty()) {
            throw new RuntimeException("Cannot delete timeline item that has dependencies");
        }
        repository.deleteById(itemId);
    }

    // ==================== STATUS MANAGEMENT ====================

    public TimelineItemResponse updateStatus(UUID itemId, String status) {
        TimelineItemEntity entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Timeline item not found"));
        
        entity.setStatus(ai.eventplanner.common.domain.enums.Status.valueOf(status.toUpperCase()));
        if (ai.eventplanner.common.domain.enums.Status.valueOf(status.toUpperCase()) == ai.eventplanner.common.domain.enums.Status.COMPLETED) {
            entity.setCompletedAt(LocalDateTime.now());
        }
        
        TimelineItemEntity saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public TimelineItemResponse markComplete(UUID itemId) {
        return updateStatus(itemId, "COMPLETED");
    }

    public TimelineItemResponse assignTimelineItem(UUID itemId, UUID assignedTo) {
        TimelineItemEntity entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Timeline item not found"));
        
        entity.setAssignedTo(assignedTo);
        TimelineItemEntity saved = repository.save(entity);
        return convertToResponse(saved);
    }

    // ==================== FILTERING AND SEARCH ====================

    public List<TimelineItemResponse> filterTimelineItems(UUID eventId, String status, String itemType, 
            String priority, UUID assignedTo, LocalDateTime startDate, LocalDateTime endDate) {
        List<TimelineItemEntity> entities = repository.findByEventIdOrderByScheduledAtAsc(eventId);
        
        return entities.stream()
                .filter(entity -> status == null || entity.getStatus().toString().equalsIgnoreCase(status))
                .filter(entity -> itemType == null || entity.getItemType().toString().equalsIgnoreCase(itemType))
                .filter(entity -> priority == null || entity.getPriority().equalsIgnoreCase(priority))
                .filter(entity -> assignedTo == null || assignedTo.equals(entity.getAssignedTo()))
                .filter(entity -> startDate == null || entity.getScheduledAt().isAfter(startDate) || entity.getScheduledAt().isEqual(startDate))
                .filter(entity -> endDate == null || entity.getScheduledAt().isBefore(endDate) || entity.getScheduledAt().isEqual(endDate))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TimelineItemResponse> getUpcomingItems(UUID eventId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekFromNow = now.plusDays(7);
        
        return repository.findByEventIdAndScheduledAtBetweenOrderByScheduledAtAsc(eventId, now, weekFromNow)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TimelineItemResponse> getOverdueItems(UUID eventId) {
        LocalDateTime now = LocalDateTime.now();
        
        return repository.findByEventIdAndScheduledAtBeforeAndStatusNotOrderByScheduledAtAsc(eventId, now, ai.eventplanner.common.domain.enums.Status.COMPLETED)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== DEPENDENCY MANAGEMENT ====================

    public List<TimelineItemResponse> getDependencies(UUID itemId) {
        TimelineItemEntity entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Timeline item not found"));
        
        if (entity.getDependencies() == null || entity.getDependencies().isEmpty()) {
            return Collections.emptyList();
        }
        
        return entity.getDependencies().stream()
                .map(depId -> repository.findById(depId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TimelineItemResponse addDependency(UUID itemId, UUID dependencyId) {
        TimelineItemEntity entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Timeline item not found"));
        
        repository.findById(dependencyId)
                .orElseThrow(() -> new RuntimeException("Dependency timeline item not found"));
        
        // Check for circular dependencies
        if (hasCircularDependency(itemId, dependencyId)) {
            throw new RuntimeException("Circular dependency detected");
        }
        
        List<UUID> currentDeps = entity.getDependencies();
        if (currentDeps == null) {
            currentDeps = new ArrayList<>();
        }
        
        if (!currentDeps.contains(dependencyId)) {
            currentDeps.add(dependencyId);
            entity.setDependencies(currentDeps);
        }
        
        TimelineItemEntity saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public TimelineItemResponse removeDependency(UUID itemId, UUID dependencyId) {
        TimelineItemEntity entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Timeline item not found"));
        
        List<UUID> currentDeps = entity.getDependencies();
        if (currentDeps == null) {
            return convertToResponse(entity);
        }
        
        currentDeps.remove(dependencyId);
        entity.setDependencies(currentDeps);
        
        TimelineItemEntity saved = repository.save(entity);
        return convertToResponse(saved);
    }

    // ==================== WORKBACK SCHEDULING ====================

    public List<WorkbackMilestone> generateWorkbackSchedule(UUID eventId, WorkbackRequest request) {
        LocalDate eventDate = LocalDate.parse(request.getEventDate());
        
        List<WorkbackMilestone> milestones = new ArrayList<>();
        
        // Generate milestones based on event type
        String eventType = request.getEventType();
        if ("WEDDING".equalsIgnoreCase(eventType)) {
            milestones.addAll(generateWeddingMilestones(eventDate));
        } else if ("CORPORATE".equalsIgnoreCase(eventType)) {
            milestones.addAll(generateCorporateMilestones(eventDate));
        } else {
            milestones.addAll(generateGenericMilestones(eventDate));
        }
        
        return milestones;
    }

    public List<TimelineItemResponse> applyWorkbackSchedule(UUID eventId, WorkbackRequest request) {
        List<WorkbackMilestone> milestones = generateWorkbackSchedule(eventId, request);
        List<TimelineItemEntity> entities = new ArrayList<>();
        
        for (WorkbackMilestone milestone : milestones) {
            TimelineItemEntity entity = new TimelineItemEntity();
            entity.setEventId(eventId);
            entity.setTitle(milestone.getTitle());
            entity.setDescription(milestone.getDescription());
            entity.setScheduledAt(milestone.getDueDate().atStartOfDay());
            entity.setDurationMinutes(60); // Default 1 hour
            entity.setPriority(milestone.getPriority());
            entity.setStatus(ai.eventplanner.common.domain.enums.Status.PENDING);
            entities.add(entity);
        }
        
        List<TimelineItemEntity> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== TEMPLATES ====================

    public List<String> getTimelineTemplates(String eventType) {
        if ("WEDDING".equalsIgnoreCase(eventType)) {
            return Arrays.asList("Traditional Wedding", "Modern Wedding", "Destination Wedding");
        } else if ("CORPORATE".equalsIgnoreCase(eventType)) {
            return Arrays.asList("Conference", "Workshop", "Team Building", "Product Launch");
        } else {
            return Arrays.asList("Generic Event", "Party", "Meeting", "Celebration");
        }
    }

    public List<TimelineItemResponse> applyTimelineTemplate(UUID eventId, String templateName) {
        // This would typically load from a template database or configuration
        // For now, return empty list as templates would be implemented separately
        return Collections.emptyList();
    }

    // ==================== SUMMARY AND ANALYTICS ====================

    public TimelineSummaryResponse getTimelineSummary(UUID eventId) {
        List<TimelineItemEntity> items = repository.findByEventIdOrderByScheduledAtAsc(eventId);
        
        long totalItems = items.size();
        long completedItems = items.stream()
                .filter(item -> item.getStatus() == ai.eventplanner.common.domain.enums.Status.COMPLETED)
                .count();
        long pendingItems = items.stream()
                .filter(item -> item.getStatus() == ai.eventplanner.common.domain.enums.Status.PENDING)
                .count();
        long overdueItems = items.stream()
                .filter(item -> item.getScheduledAt().isBefore(LocalDateTime.now()) && item.getStatus() != ai.eventplanner.common.domain.enums.Status.COMPLETED)
                .count();
        
        double completionRate = totalItems > 0 ? (double) completedItems / totalItems * 100 : 0;
        
        TimelineSummaryResponse summary = new TimelineSummaryResponse();
        summary.setTotalItems((int) totalItems);
        summary.setCompletedItems((int) completedItems);
        summary.setPendingItems((int) pendingItems);
        summary.setOverdueItems((int) overdueItems);
        summary.setCompletionRate(completionRate);
        summary.setEventId(eventId);
        
        return summary;
    }

    // ==================== HELPER METHODS ====================

    private void validateDependencies(TimelineItemEntity item) {
        List<UUID> deps = item.getDependencies();
        if (deps == null || deps.isEmpty()) return;
        
        Set<UUID> seen = new HashSet<>();
        for (UUID dep : deps) {
            if (dep == null) continue;
            if (dep.equals(item.getId())) throw new IllegalArgumentException("Item cannot depend on itself");
            if (!seen.add(dep)) throw new IllegalArgumentException("Duplicate dependency: " + dep);
        }
    }

    private boolean hasCircularDependency(UUID itemId, UUID dependencyId) {
        if (itemId.equals(dependencyId)) return true;
        
        TimelineItemEntity dependency = repository.findById(dependencyId).orElse(null);
        if (dependency == null || dependency.getDependencies() == null || dependency.getDependencies().isEmpty()) return false;
        
        for (UUID dep : dependency.getDependencies()) {
            if (hasCircularDependency(itemId, dep)) return true;
        }
        
        return false;
    }

    private TimelineItemResponse convertToResponse(TimelineItemEntity entity) {
        TimelineItemResponse response = new TimelineItemResponse();
        response.setId(entity.getId());
        response.setEventId(entity.getEventId());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setScheduledAt(entity.getScheduledAt());
        response.setDurationMinutes(entity.getDurationMinutes());
        response.setEndTime(entity.getEndTime());
        response.setItemType(entity.getItemType());
        response.setStatus(entity.getStatus());
        response.setPriority(entity.getPriority());
        response.setLocation(entity.getLocation());
        response.setAssignedTo(entity.getAssignedTo());
        response.setDependencies(entity.getDependencies());
        response.setSetupTimeMinutes(entity.getSetupTimeMinutes());
        response.setTeardownTimeMinutes(entity.getTeardownTimeMinutes());
        response.setResourcesRequired(entity.getResourcesRequired());
        response.setNotes(entity.getNotes());
        response.setCompletedAt(entity.getCompletedAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private List<WorkbackMilestone> generateWeddingMilestones(LocalDate eventDate) {
        List<WorkbackMilestone> milestones = new ArrayList<>();
        
        milestones.add(createMilestone("Book Venue", -365, eventDate, "critical", "venue"));
        milestones.add(createMilestone("Hire Photographer", -300, eventDate, "high", "vendor"));
        milestones.add(createMilestone("Order Wedding Dress", -180, eventDate, "high", "attire"));
        milestones.add(createMilestone("Send Save-the-Dates", -120, eventDate, "high", "communication"));
        milestones.add(createMilestone("Book Caterer", -90, eventDate, "critical", "catering"));
        milestones.add(createMilestone("Order Wedding Rings", -60, eventDate, "medium", "attire"));
        milestones.add(createMilestone("Send Invitations", -45, eventDate, "high", "communication"));
        milestones.add(createMilestone("Final Fitting", -30, eventDate, "high", "attire"));
        milestones.add(createMilestone("Finalize Menu", -14, eventDate, "high", "catering"));
        milestones.add(createMilestone("Rehearsal Dinner", -1, eventDate, "high", "rehearsal"));
        
        return milestones;
    }

    private List<WorkbackMilestone> generateCorporateMilestones(LocalDate eventDate) {
        List<WorkbackMilestone> milestones = new ArrayList<>();
        
        milestones.add(createMilestone("Book Venue", -90, eventDate, "critical", "venue"));
        milestones.add(createMilestone("Create Event Website", -60, eventDate, "high", "marketing"));
        milestones.add(createMilestone("Send Invitations", -45, eventDate, "high", "communication"));
        milestones.add(createMilestone("Book Catering", -30, eventDate, "critical", "catering"));
        milestones.add(createMilestone("Finalize Agenda", -14, eventDate, "high", "planning"));
        milestones.add(createMilestone("Setup Equipment", -1, eventDate, "critical", "setup"));
        
        return milestones;
    }

    private List<WorkbackMilestone> generateGenericMilestones(LocalDate eventDate) {
        List<WorkbackMilestone> milestones = new ArrayList<>();
        
        milestones.add(createMilestone("Book Venue", -60, eventDate, "high", "venue"));
        milestones.add(createMilestone("Send Invitations", -30, eventDate, "high", "communication"));
        milestones.add(createMilestone("Finalize Details", -14, eventDate, "high", "planning"));
        milestones.add(createMilestone("Setup Event", -1, eventDate, "critical", "setup"));
        
        return milestones;
    }

    private WorkbackMilestone createMilestone(String title, int dueInDays, LocalDate eventDate, String priority, String category) {
        WorkbackMilestone milestone = new WorkbackMilestone();
        milestone.setTitle(title);
        milestone.setDueInDays(dueInDays);
        milestone.setDueDate(eventDate.plusDays(dueInDays));
        milestone.setPriority(priority);
        milestone.setCategory(category);
        milestone.setDescription("Generated milestone for " + title);
        return milestone;
    }
}


