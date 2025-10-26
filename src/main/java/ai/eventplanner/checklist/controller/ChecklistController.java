package ai.eventplanner.checklist.controller;

import ai.eventplanner.checklist.dto.request.ChecklistItemCreateRequest;
import ai.eventplanner.checklist.dto.request.ChecklistItemUpdateRequest;
import ai.eventplanner.checklist.dto.request.ChecklistBulkCreateRequest;
import ai.eventplanner.checklist.dto.response.ChecklistItemResponse;
import ai.eventplanner.checklist.dto.response.ChecklistSummaryResponse;
import ai.eventplanner.checklist.dto.request.ChecklistTemplateRequest;
import ai.eventplanner.checklist.service.ChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checklist")
@Tag(name = "Checklist Management", description = "Comprehensive checklist and task management")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    // ==================== CHECKLIST CRUD OPERATIONS ====================

    @GetMapping("/{eventId}")
    @Operation(summary = "Get event checklist", description = "Retrieve all checklist items for an event")
    public ResponseEntity<List<ChecklistItemResponse>> getChecklist(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> checklist = checklistService.getChecklistByEventId(UUID.fromString(eventId));
        return ResponseEntity.ok(checklist);
    }

    @GetMapping("/{eventId}/summary")
    @Operation(summary = "Get checklist summary", description = "Get checklist summary with completion metrics")
    public ResponseEntity<ChecklistSummaryResponse> getChecklistSummary(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        ChecklistSummaryResponse summary = checklistService.getChecklistSummary(UUID.fromString(eventId));
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    @Operation(summary = "Create checklist item", description = "Create a new checklist item")
    public ResponseEntity<ChecklistItemResponse> createChecklistItem(
            @Valid @RequestBody ChecklistItemCreateRequest request) {
        ChecklistItemResponse response = checklistService.createChecklistItem(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple checklist items", description = "Create multiple checklist items in one operation")
    public ResponseEntity<List<ChecklistItemResponse>> createBulkChecklistItems(
            @Valid @RequestBody ChecklistBulkCreateRequest request) {
        List<ChecklistItemResponse> responses = checklistService.createBulkChecklistItems(request);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update checklist item", description = "Update an existing checklist item")
    public ResponseEntity<ChecklistItemResponse> updateChecklistItem(
            @Parameter(description = "Checklist item ID") @PathVariable("itemId") String itemId,
            @Valid @RequestBody ChecklistItemUpdateRequest request) {
        ChecklistItemResponse response = checklistService.updateChecklistItem(UUID.fromString(itemId), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete checklist item", description = "Delete a checklist item")
    public ResponseEntity<Void> deleteChecklistItem(
            @Parameter(description = "Checklist item ID") @PathVariable("itemId") String itemId) {
        checklistService.deleteChecklistItem(UUID.fromString(itemId));
        return ResponseEntity.noContent().build();
    }

    // ==================== CHECKLIST STATUS MANAGEMENT ====================

    @PutMapping("/{itemId}/complete")
    @Operation(summary = "Mark checklist item as complete", description = "Mark a checklist item as completed")
    public ResponseEntity<ChecklistItemResponse> markComplete(
            @Parameter(description = "Checklist item ID") @PathVariable("itemId") String itemId) {
        ChecklistItemResponse response = checklistService.markComplete(UUID.fromString(itemId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}/incomplete")
    @Operation(summary = "Mark checklist item as incomplete", description = "Mark a checklist item as not completed")
    public ResponseEntity<ChecklistItemResponse> markIncomplete(
            @Parameter(description = "Checklist item ID") @PathVariable("itemId") String itemId) {
        ChecklistItemResponse response = checklistService.markIncomplete(UUID.fromString(itemId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}/toggle")
    @Operation(summary = "Toggle checklist item completion", description = "Toggle the completion status of a checklist item")
    public ResponseEntity<ChecklistItemResponse> toggleCompletion(
            @Parameter(description = "Checklist item ID") @PathVariable("itemId") String itemId) {
        ChecklistItemResponse response = checklistService.toggleCompletion(UUID.fromString(itemId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}/assign")
    @Operation(summary = "Assign checklist item", description = "Assign a checklist item to a user")
    public ResponseEntity<ChecklistItemResponse> assignChecklistItem(
            @Parameter(description = "Checklist item ID") @PathVariable("itemId") String itemId,
            @RequestParam UUID assignedTo) {
        ChecklistItemResponse response = checklistService.assignChecklistItem(UUID.fromString(itemId), assignedTo);
        return ResponseEntity.ok(response);
    }

    // ==================== CHECKLIST FILTERING AND SEARCH ====================

    @GetMapping("/{eventId}/filter")
    @Operation(summary = "Filter checklist items", description = "Filter checklist items by various criteria")
    public ResponseEntity<List<ChecklistItemResponse>> filterChecklistItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @RequestParam(required = false) Boolean isCompleted,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) LocalDateTime dueDateFrom,
            @RequestParam(required = false) LocalDateTime dueDateTo) {
        List<ChecklistItemResponse> items = checklistService.filterChecklistItems(
                UUID.fromString(eventId), isCompleted, priority, category, assignedTo, dueDateFrom, dueDateTo);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{eventId}/completed")
    @Operation(summary = "Get completed checklist items", description = "Get all completed checklist items for an event")
    public ResponseEntity<List<ChecklistItemResponse>> getCompletedItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> items = checklistService.getCompletedItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{eventId}/pending")
    @Operation(summary = "Get pending checklist items", description = "Get all pending checklist items for an event")
    public ResponseEntity<List<ChecklistItemResponse>> getPendingItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> items = checklistService.getPendingItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{eventId}/overdue")
    @Operation(summary = "Get overdue checklist items", description = "Get all overdue checklist items for an event")
    public ResponseEntity<List<ChecklistItemResponse>> getOverdueItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> items = checklistService.getOverdueItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{eventId}/due-soon")
    @Operation(summary = "Get items due soon", description = "Get checklist items due within the next 7 days")
    public ResponseEntity<List<ChecklistItemResponse>> getItemsDueSoon(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> items = checklistService.getItemsDueSoon(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    // ==================== CHECKLIST CATEGORIES ====================

    @GetMapping("/{eventId}/categories")
    @Operation(summary = "Get checklist categories", description = "Get all categories used in the event checklist")
    public ResponseEntity<List<String>> getCategories(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<String> categories = checklistService.getCategories(UUID.fromString(eventId));
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{eventId}/category/{category}")
    @Operation(summary = "Get items by category", description = "Get all checklist items in a specific category")
    public ResponseEntity<List<ChecklistItemResponse>> getItemsByCategory(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @Parameter(description = "Category name") @PathVariable("category") String category) {
        List<ChecklistItemResponse> items = checklistService.getItemsByCategory(UUID.fromString(eventId), category);
        return ResponseEntity.ok(items);
    }

    // ==================== CHECKLIST TEMPLATES ====================

    @GetMapping("/templates")
    @Operation(summary = "Get checklist templates", description = "Get available checklist templates by event type")
    public ResponseEntity<List<String>> getChecklistTemplates(
            @RequestParam(required = false) String eventType) {
        List<String> templates = checklistService.getChecklistTemplates(eventType);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{templateName}")
    @Operation(summary = "Get template details", description = "Get details of a specific checklist template")
    public ResponseEntity<List<ChecklistItemResponse>> getTemplateDetails(
            @Parameter(description = "Template name") @PathVariable("templateName") String templateName) {
        List<ChecklistItemResponse> items = checklistService.getTemplateDetails(templateName);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{eventId}/apply-template")
    @Operation(summary = "Apply checklist template", description = "Apply a checklist template to an event")
    public ResponseEntity<List<ChecklistItemResponse>> applyChecklistTemplate(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @Valid @RequestBody ChecklistTemplateRequest request) {
        List<ChecklistItemResponse> items = checklistService.applyChecklistTemplate(
                UUID.fromString(eventId), request);
        return ResponseEntity.ok(items);
    }

    // ==================== BULK OPERATIONS ====================

    @PutMapping("/{eventId}/complete-all")
    @Operation(summary = "Complete all items", description = "Mark all checklist items as completed")
    public ResponseEntity<List<ChecklistItemResponse>> completeAllItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> items = checklistService.completeAllItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    @PutMapping("/{eventId}/incomplete-all")
    @Operation(summary = "Mark all items incomplete", description = "Mark all checklist items as not completed")
    public ResponseEntity<List<ChecklistItemResponse>> incompleteAllItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<ChecklistItemResponse> items = checklistService.incompleteAllItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    @PutMapping("/{eventId}/assign-all")
    @Operation(summary = "Assign all items", description = "Assign all checklist items to a user")
    public ResponseEntity<List<ChecklistItemResponse>> assignAllItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @RequestParam UUID assignedTo) {
        List<ChecklistItemResponse> items = checklistService.assignAllItems(UUID.fromString(eventId), assignedTo);
        return ResponseEntity.ok(items);
    }

    // ==================== CHECKLIST ANALYTICS ====================

    @GetMapping("/{eventId}/analytics")
    @Operation(summary = "Get checklist analytics", description = "Get detailed analytics for checklist completion")
    public ResponseEntity<ChecklistSummaryResponse> getChecklistAnalytics(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        ChecklistSummaryResponse analytics = checklistService.getChecklistAnalytics(UUID.fromString(eventId));
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/{eventId}/progress")
    @Operation(summary = "Get completion progress", description = "Get completion progress over time")
    public ResponseEntity<List<ChecklistSummaryResponse>> getCompletionProgress(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        List<ChecklistSummaryResponse> progress = checklistService.getCompletionProgress(
                UUID.fromString(eventId), startDate, endDate);
        return ResponseEntity.ok(progress);
    }
}
