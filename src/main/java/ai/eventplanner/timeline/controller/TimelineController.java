package ai.eventplanner.timeline.controller;

import ai.eventplanner.timeline.dto.TimelineItemCreateRequest;
import ai.eventplanner.timeline.dto.request.WorkbackRequest;
import ai.eventplanner.timeline.dto.response.WorkbackMilestone;
import ai.eventplanner.timeline.dto.response.TimelineItemResponse;
import ai.eventplanner.timeline.dto.request.TimelineItemUpdateRequest;
import ai.eventplanner.timeline.dto.request.TimelineBulkCreateRequest;
import ai.eventplanner.timeline.dto.response.TimelineSummaryResponse;
import ai.eventplanner.timeline.model.TimelineItemEntity;
import ai.eventplanner.timeline.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timeline")
@Tag(name = "Timeline Management", description = "Comprehensive timeline and run-of-show management")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    // ==================== TIMELINE CRUD OPERATIONS ====================

    @GetMapping("/{eventId}")
    @Operation(summary = "Get event timeline", description = "Retrieve all timeline items for an event ordered by scheduled time")
    public ResponseEntity<List<TimelineItemResponse>> getTimeline(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<TimelineItemResponse> timeline = timelineService.getTimelineByEventId(UUID.fromString(eventId));
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/{eventId}/summary")
    @Operation(summary = "Get timeline summary", description = "Get timeline summary with key metrics and status")
    public ResponseEntity<TimelineSummaryResponse> getTimelineSummary(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        TimelineSummaryResponse summary = timelineService.getTimelineSummary(UUID.fromString(eventId));
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    @Operation(summary = "Create timeline item", description = "Create a new timeline item with dependencies and scheduling")
    public ResponseEntity<TimelineItemResponse> createTimelineItem(
            @Valid @RequestBody TimelineItemCreateRequest request) {
        TimelineItemResponse response = timelineService.createTimelineItem(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple timeline items", description = "Create multiple timeline items in one operation")
    public ResponseEntity<List<TimelineItemResponse>> createBulkTimelineItems(
            @Valid @RequestBody TimelineBulkCreateRequest request) {
        List<TimelineItemResponse> responses = timelineService.createBulkTimelineItems(request);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update timeline item", description = "Update an existing timeline item")
    public ResponseEntity<TimelineItemResponse> updateTimelineItem(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId,
            @Valid @RequestBody TimelineItemUpdateRequest request) {
        TimelineItemResponse response = timelineService.updateTimelineItem(UUID.fromString(itemId), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete timeline item", description = "Delete a timeline item and handle dependencies")
    public ResponseEntity<Void> deleteTimelineItem(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId) {
        timelineService.deleteTimelineItem(UUID.fromString(itemId));
        return ResponseEntity.noContent().build();
    }

    // ==================== TIMELINE STATUS MANAGEMENT ====================

    @PutMapping("/{itemId}/status")
    @Operation(summary = "Update timeline item status", description = "Update the status of a timeline item")
    public ResponseEntity<TimelineItemResponse> updateStatus(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId,
            @RequestParam String status) {
        TimelineItemResponse response = timelineService.updateStatus(UUID.fromString(itemId), status);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}/complete")
    @Operation(summary = "Mark timeline item as complete", description = "Mark a timeline item as completed")
    public ResponseEntity<TimelineItemResponse> markComplete(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId) {
        TimelineItemResponse response = timelineService.markComplete(UUID.fromString(itemId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}/assign")
    @Operation(summary = "Assign timeline item", description = "Assign a timeline item to a user")
    public ResponseEntity<TimelineItemResponse> assignTimelineItem(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId,
            @RequestParam UUID assignedTo) {
        TimelineItemResponse response = timelineService.assignTimelineItem(UUID.fromString(itemId), assignedTo);
        return ResponseEntity.ok(response);
    }

    // ==================== WORKBACK SCHEDULING ====================

    @PostMapping("/{eventId}/workback")
    @Operation(summary = "Generate workback schedule", description = "Generate workback milestones from event date")
    public ResponseEntity<List<WorkbackMilestone>> generateWorkback(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @Valid @RequestBody WorkbackRequest request) {
        List<WorkbackMilestone> milestones = timelineService.generateWorkbackSchedule(
                UUID.fromString(eventId), request);
        return ResponseEntity.ok(milestones);
    }

    @PostMapping("/{eventId}/workback/apply")
    @Operation(summary = "Apply workback schedule", description = "Apply generated workback milestones to timeline")
    public ResponseEntity<List<TimelineItemResponse>> applyWorkbackSchedule(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @Valid @RequestBody WorkbackRequest request) {
        List<TimelineItemResponse> timelineItems = timelineService.applyWorkbackSchedule(
                UUID.fromString(eventId), request);
        return ResponseEntity.ok(timelineItems);
    }

    // ==================== TIMELINE FILTERING AND SEARCH ====================

    @GetMapping("/{eventId}/filter")
    @Operation(summary = "Filter timeline items", description = "Filter timeline items by various criteria")
    public ResponseEntity<List<TimelineItemResponse>> filterTimelineItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        List<TimelineItemResponse> items = timelineService.filterTimelineItems(
                UUID.fromString(eventId), status, itemType, priority, assignedTo, startDate, endDate);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{eventId}/upcoming")
    @Operation(summary = "Get upcoming timeline items", description = "Get timeline items scheduled for the next 7 days")
    public ResponseEntity<List<TimelineItemResponse>> getUpcomingItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<TimelineItemResponse> items = timelineService.getUpcomingItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{eventId}/overdue")
    @Operation(summary = "Get overdue timeline items", description = "Get timeline items that are overdue")
    public ResponseEntity<List<TimelineItemResponse>> getOverdueItems(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId) {
        List<TimelineItemResponse> items = timelineService.getOverdueItems(UUID.fromString(eventId));
        return ResponseEntity.ok(items);
    }

    // ==================== DEPENDENCY MANAGEMENT ====================

    @GetMapping("/{itemId}/dependencies")
    @Operation(summary = "Get timeline item dependencies", description = "Get all dependencies for a timeline item")
    public ResponseEntity<List<TimelineItemResponse>> getDependencies(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId) {
        List<TimelineItemResponse> dependencies = timelineService.getDependencies(UUID.fromString(itemId));
        return ResponseEntity.ok(dependencies);
    }

    @PostMapping("/{itemId}/dependencies")
    @Operation(summary = "Add timeline item dependency", description = "Add a dependency to a timeline item")
    public ResponseEntity<TimelineItemResponse> addDependency(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId,
            @RequestParam UUID dependencyId) {
        TimelineItemResponse response = timelineService.addDependency(UUID.fromString(itemId), dependencyId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}/dependencies/{dependencyId}")
    @Operation(summary = "Remove timeline item dependency", description = "Remove a dependency from a timeline item")
    public ResponseEntity<TimelineItemResponse> removeDependency(
            @Parameter(description = "Timeline item ID") @PathVariable("itemId") String itemId,
            @Parameter(description = "Dependency ID") @PathVariable("dependencyId") String dependencyId) {
        TimelineItemResponse response = timelineService.removeDependency(
                UUID.fromString(itemId), UUID.fromString(dependencyId));
        return ResponseEntity.ok(response);
    }

    // ==================== TIMELINE TEMPLATES ====================

    @GetMapping("/templates")
    @Operation(summary = "Get timeline templates", description = "Get available timeline templates by event type")
    public ResponseEntity<List<String>> getTimelineTemplates(
            @RequestParam(required = false) String eventType) {
        List<String> templates = timelineService.getTimelineTemplates(eventType);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/{eventId}/apply-template")
    @Operation(summary = "Apply timeline template", description = "Apply a timeline template to an event")
    public ResponseEntity<List<TimelineItemResponse>> applyTimelineTemplate(
            @Parameter(description = "Event ID") @PathVariable("eventId") String eventId,
            @RequestParam String templateName) {
        List<TimelineItemResponse> items = timelineService.applyTimelineTemplate(
                UUID.fromString(eventId), templateName);
        return ResponseEntity.ok(items);
    }
}

