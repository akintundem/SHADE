package eventplanner.features.timeline.controller;

import eventplanner.features.timeline.dto.TimelineItemCreateRequest;
import eventplanner.features.timeline.dto.request.WorkbackRequest;
import eventplanner.features.timeline.dto.response.WorkbackMilestone;
import eventplanner.features.timeline.entity.TimelineItem;
import eventplanner.features.timeline.service.TimelineService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timeline")
@Tag(name = "Timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/{eventId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get run-of-show timeline")
    public ResponseEntity<List<TimelineItem>> getTimeline(@PathVariable("eventId") String eventId) {
        return ResponseEntity.ok(timelineService.list(java.util.UUID.fromString(eventId)));
    }

    @PostMapping("/{eventId}/workback")
    @RequiresPermission(value = RbacPermissions.TIMELINE_WORKBACK_GENERATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Generate workback schedule from event date")
    public ResponseEntity<List<WorkbackMilestone>> generateWorkback(
            @PathVariable("eventId") String eventId,
            @Valid @RequestBody WorkbackRequest request
    ) {
        // Parse event date
        LocalDate eventDate = LocalDate.parse(request.getEventDate());
        
        // Generate workback milestones
        List<WorkbackMilestone> milestones = List.of(
                createMilestone("Send Save-the-Dates", -60, eventDate, "high", "communication"),
                createMilestone("Confirm Vendors", -30, eventDate, "high", "vendor"),
                createMilestone("Finalize Guest List", -14, eventDate, "medium", "guest"),
                createMilestone("Run-of-Show Draft", -7, eventDate, "high", "planning"),
                createMilestone("Final Confirmations", -2, eventDate, "critical", "final")
        );
        return ResponseEntity.ok(milestones);
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

    @PostMapping
    @RequiresPermission(value = RbacPermissions.TIMELINE_CREATE, resources = {"event_id=#item.eventId"})
    @Operation(summary = "Create timeline item")
    public ResponseEntity<TimelineItem> create(@Valid @RequestBody TimelineItemCreateRequest item) {
        TimelineItem entity = new TimelineItem();
        entity.setEventId(item.getEventId());
        entity.setTitle(item.getTitle());
        entity.setDescription(item.getDescription());
        entity.setScheduledAt(item.getScheduledAt());
        entity.setDurationMinutes(item.getDurationMinutes());
        entity.setAssignedTo(item.getAssignedTo());
        entity.setDependencies(item.getDependencies());
        return ResponseEntity.ok(timelineService.create(entity));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_DELETE, resources = {"event_id=#eventId"})
    @Operation(summary = "Delete timeline item")
    public ResponseEntity<Void> delete(@PathVariable("id") String id,
                                       @RequestParam("eventId") String eventId) {
        timelineService.delete(java.util.UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}
