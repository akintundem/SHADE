package ai.eventplanner.checklist.service;

import ai.eventplanner.checklist.dto.request.ChecklistItemCreateRequest;
import ai.eventplanner.checklist.dto.request.ChecklistItemUpdateRequest;
import ai.eventplanner.checklist.dto.request.ChecklistBulkCreateRequest;
import ai.eventplanner.checklist.dto.request.ChecklistTemplateRequest;
import ai.eventplanner.checklist.dto.response.ChecklistItemResponse;
import ai.eventplanner.checklist.dto.response.ChecklistSummaryResponse;
import ai.eventplanner.checklist.entity.EventChecklist;
import ai.eventplanner.checklist.repository.EventChecklistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChecklistService {
    private final EventChecklistRepository repository;

    public ChecklistService(EventChecklistRepository repository) {
        this.repository = repository;
    }

    // ==================== BASIC CRUD OPERATIONS ====================

    public List<ChecklistItemResponse> getChecklistByEventId(UUID eventId) {
        List<EventChecklist> entities = repository.findByEventIdOrderByCreatedAtAsc(eventId);
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ChecklistItemResponse createChecklistItem(ChecklistItemCreateRequest request) {
        EventChecklist entity = new EventChecklist();
        entity.setEventId(request.getEventId());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setIsCompleted(request.getIsCompleted() != null ? request.getIsCompleted() : false);
        entity.setDueDate(request.getDueDate());
        entity.setPriority(request.getPriority());
        entity.setAssignedTo(request.getAssignedTo());
        entity.setCategory(request.getCategory());
        
        EventChecklist saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public List<ChecklistItemResponse> createBulkChecklistItems(ChecklistBulkCreateRequest request) {
        List<EventChecklist> entities = new ArrayList<>();
        
        for (ChecklistItemCreateRequest itemRequest : request.getItems()) {
            EventChecklist entity = new EventChecklist();
            entity.setEventId(itemRequest.getEventId());
            entity.setTitle(itemRequest.getTitle());
            entity.setDescription(itemRequest.getDescription());
            entity.setIsCompleted(itemRequest.getIsCompleted() != null ? itemRequest.getIsCompleted() : false);
            entity.setDueDate(itemRequest.getDueDate());
            entity.setPriority(itemRequest.getPriority());
            entity.setAssignedTo(itemRequest.getAssignedTo());
            entity.setCategory(itemRequest.getCategory());
            entities.add(entity);
        }
        
        List<EventChecklist> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ChecklistItemResponse updateChecklistItem(UUID itemId, ChecklistItemUpdateRequest request) {
        EventChecklist entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getIsCompleted() != null) entity.setIsCompleted(request.getIsCompleted());
        if (request.getDueDate() != null) entity.setDueDate(request.getDueDate());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getAssignedTo() != null) entity.setAssignedTo(request.getAssignedTo());
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
        
        EventChecklist saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public void deleteChecklistItem(UUID itemId) {
        repository.deleteById(itemId);
    }

    // ==================== STATUS MANAGEMENT ====================

    public ChecklistItemResponse markComplete(UUID itemId) {
        EventChecklist entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        
        entity.setIsCompleted(true);
        EventChecklist saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public ChecklistItemResponse markIncomplete(UUID itemId) {
        EventChecklist entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        
        entity.setIsCompleted(false);
        EventChecklist saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public ChecklistItemResponse toggleCompletion(UUID itemId) {
        EventChecklist entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        
        entity.setIsCompleted(!entity.getIsCompleted());
        EventChecklist saved = repository.save(entity);
        return convertToResponse(saved);
    }

    public ChecklistItemResponse assignChecklistItem(UUID itemId, UUID assignedTo) {
        EventChecklist entity = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        
        entity.setAssignedTo(assignedTo);
        EventChecklist saved = repository.save(entity);
        return convertToResponse(saved);
    }

    // ==================== FILTERING AND SEARCH ====================

    public List<ChecklistItemResponse> filterChecklistItems(UUID eventId, Boolean isCompleted, String priority, 
            String category, UUID assignedTo, LocalDateTime dueDateFrom, LocalDateTime dueDateTo) {
        List<EventChecklist> entities = repository.findByEventIdOrderByCreatedAtAsc(eventId);
        
        return entities.stream()
                .filter(entity -> isCompleted == null || entity.getIsCompleted().equals(isCompleted))
                .filter(entity -> priority == null || entity.getPriority().equalsIgnoreCase(priority))
                .filter(entity -> category == null || entity.getCategory().equalsIgnoreCase(category))
                .filter(entity -> assignedTo == null || assignedTo.equals(entity.getAssignedTo()))
                .filter(entity -> dueDateFrom == null || entity.getDueDate() == null || 
                        entity.getDueDate().isAfter(dueDateFrom) || entity.getDueDate().isEqual(dueDateFrom))
                .filter(entity -> dueDateTo == null || entity.getDueDate() == null || 
                        entity.getDueDate().isBefore(dueDateTo) || entity.getDueDate().isEqual(dueDateTo))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemResponse> getCompletedItems(UUID eventId) {
        return repository.findByEventIdAndIsCompletedTrueOrderByCreatedAtAsc(eventId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemResponse> getPendingItems(UUID eventId) {
        return repository.findByEventIdAndIsCompletedFalseOrderByCreatedAtAsc(eventId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemResponse> getOverdueItems(UUID eventId) {
        LocalDateTime now = LocalDateTime.now();
        return repository.findByEventIdAndIsCompletedFalseAndDueDateBeforeOrderByDueDateAsc(eventId, now)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemResponse> getItemsDueSoon(UUID eventId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekFromNow = now.plusDays(7);
        
        return repository.findByEventIdAndIsCompletedFalseAndDueDateBetweenOrderByDueDateAsc(eventId, now, weekFromNow)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== CATEGORY MANAGEMENT ====================

    public List<String> getCategories(UUID eventId) {
        return repository.findDistinctCategoriesByEventId(eventId);
    }

    public List<ChecklistItemResponse> getItemsByCategory(UUID eventId, String category) {
        return repository.findByEventIdAndCategoryOrderByCreatedAtAsc(eventId, category)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== TEMPLATES ====================

    public List<String> getChecklistTemplates(String eventType) {
        if ("WEDDING".equalsIgnoreCase(eventType)) {
            return Arrays.asList("Traditional Wedding", "Modern Wedding", "Destination Wedding", "Small Wedding");
        } else if ("CORPORATE".equalsIgnoreCase(eventType)) {
            return Arrays.asList("Conference", "Workshop", "Team Building", "Product Launch", "Board Meeting");
        } else if ("BIRTHDAY".equalsIgnoreCase(eventType)) {
            return Arrays.asList("Kids Birthday", "Adult Birthday", "Milestone Birthday", "Surprise Party");
        } else {
            return Arrays.asList("Generic Event", "Party", "Meeting", "Celebration", "Gathering");
        }
    }

    public List<ChecklistItemResponse> getTemplateDetails(String templateName) {
        // This would typically load from a template database or configuration
        // For now, return sample template items based on template name
        return generateTemplateItems(templateName);
    }

    public List<ChecklistItemResponse> applyChecklistTemplate(UUID eventId, ChecklistTemplateRequest request) {
        List<ChecklistItemResponse> templateItems = getTemplateDetails(request.getTemplateName());
        List<EventChecklist> entities = new ArrayList<>();
        
        for (ChecklistItemResponse templateItem : templateItems) {
            EventChecklist entity = new EventChecklist();
            entity.setEventId(eventId);
            entity.setTitle(templateItem.getTitle());
            entity.setDescription(templateItem.getDescription());
            entity.setIsCompleted(false);
            entity.setDueDate(templateItem.getDueDate());
            entity.setPriority(templateItem.getPriority());
            entity.setAssignedTo(templateItem.getAssignedTo());
            entity.setCategory(templateItem.getCategory());
            entities.add(entity);
        }
        
        List<EventChecklist> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== BULK OPERATIONS ====================

    public List<ChecklistItemResponse> completeAllItems(UUID eventId) {
        List<EventChecklist> entities = repository.findByEventIdAndIsCompletedFalse(eventId);
        entities.forEach(entity -> entity.setIsCompleted(true));
        List<EventChecklist> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemResponse> incompleteAllItems(UUID eventId) {
        List<EventChecklist> entities = repository.findByEventIdAndIsCompletedTrue(eventId);
        entities.forEach(entity -> entity.setIsCompleted(false));
        List<EventChecklist> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemResponse> assignAllItems(UUID eventId, UUID assignedTo) {
        List<EventChecklist> entities = repository.findByEventId(eventId);
        entities.forEach(entity -> entity.setAssignedTo(assignedTo));
        List<EventChecklist> saved = repository.saveAll(entities);
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== ANALYTICS ====================

    public ChecklistSummaryResponse getChecklistSummary(UUID eventId) {
        List<EventChecklist> items = repository.findByEventIdOrderByCreatedAtAsc(eventId);
        
        long totalItems = items.size();
        long completedItems = items.stream()
                .filter(EventChecklist::getIsCompleted)
                .count();
        long pendingItems = totalItems - completedItems;
        long overdueItems = items.stream()
                .filter(item -> !item.getIsCompleted() && item.getDueDate() != null && item.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        
        double completionRate = totalItems > 0 ? (double) completedItems / totalItems * 100 : 0;
        
        ChecklistSummaryResponse summary = new ChecklistSummaryResponse();
        summary.setTotalItems((int) totalItems);
        summary.setCompletedItems((int) completedItems);
        summary.setPendingItems((int) pendingItems);
        summary.setOverdueItems((int) overdueItems);
        summary.setCompletionRate(completionRate);
        summary.setEventId(eventId);
        
        return summary;
    }

    public ChecklistSummaryResponse getChecklistAnalytics(UUID eventId) {
        return getChecklistSummary(eventId);
    }

    public List<ChecklistSummaryResponse> getCompletionProgress(UUID eventId, LocalDateTime startDate, LocalDateTime endDate) {
        // This would typically track completion progress over time
        // For now, return current summary
        ChecklistSummaryResponse currentSummary = getChecklistSummary(eventId);
        return Arrays.asList(currentSummary);
    }

    // ==================== HELPER METHODS ====================

    private ChecklistItemResponse convertToResponse(EventChecklist entity) {
        ChecklistItemResponse response = new ChecklistItemResponse();
        response.setId(entity.getId());
        response.setEventId(entity.getEventId());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setIsCompleted(entity.getIsCompleted());
        response.setDueDate(entity.getDueDate());
        response.setPriority(entity.getPriority());
        response.setAssignedTo(entity.getAssignedTo());
        response.setCategory(entity.getCategory());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private List<ChecklistItemResponse> generateTemplateItems(String templateName) {
        List<ChecklistItemResponse> items = new ArrayList<>();
        
        if ("Traditional Wedding".equalsIgnoreCase(templateName)) {
            items.add(createTemplateItem("Book Venue", "Secure wedding venue", "HIGH", "venue"));
            items.add(createTemplateItem("Hire Photographer", "Book wedding photographer", "HIGH", "vendor"));
            items.add(createTemplateItem("Order Wedding Dress", "Purchase wedding dress", "HIGH", "attire"));
            items.add(createTemplateItem("Send Save-the-Dates", "Send save-the-date cards", "HIGH", "communication"));
            items.add(createTemplateItem("Book Caterer", "Hire wedding caterer", "CRITICAL", "catering"));
            items.add(createTemplateItem("Order Wedding Rings", "Purchase wedding rings", "MEDIUM", "attire"));
            items.add(createTemplateItem("Send Invitations", "Send wedding invitations", "HIGH", "communication"));
            items.add(createTemplateItem("Final Fitting", "Final dress fitting", "HIGH", "attire"));
            items.add(createTemplateItem("Finalize Menu", "Confirm catering menu", "HIGH", "catering"));
            items.add(createTemplateItem("Rehearsal Dinner", "Plan rehearsal dinner", "HIGH", "rehearsal"));
        } else if ("Conference".equalsIgnoreCase(templateName)) {
            items.add(createTemplateItem("Book Venue", "Secure conference venue", "CRITICAL", "venue"));
            items.add(createTemplateItem("Create Event Website", "Build conference website", "HIGH", "marketing"));
            items.add(createTemplateItem("Send Invitations", "Send conference invitations", "HIGH", "communication"));
            items.add(createTemplateItem("Book Catering", "Arrange conference catering", "CRITICAL", "catering"));
            items.add(createTemplateItem("Finalize Agenda", "Complete conference agenda", "HIGH", "planning"));
            items.add(createTemplateItem("Setup Equipment", "Setup AV and technical equipment", "CRITICAL", "setup"));
        } else {
            // Generic template
            items.add(createTemplateItem("Book Venue", "Secure event venue", "HIGH", "venue"));
            items.add(createTemplateItem("Send Invitations", "Send event invitations", "HIGH", "communication"));
            items.add(createTemplateItem("Finalize Details", "Complete event planning", "HIGH", "planning"));
            items.add(createTemplateItem("Setup Event", "Prepare event space", "CRITICAL", "setup"));
        }
        
        return items;
    }

    private ChecklistItemResponse createTemplateItem(String title, String description, String priority, String category) {
        ChecklistItemResponse item = new ChecklistItemResponse();
        item.setTitle(title);
        item.setDescription(description);
        item.setPriority(priority);
        item.setCategory(category);
        item.setIsCompleted(false);
        return item;
    }
}
