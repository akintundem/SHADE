package eventplanner.features.timeline.controller;

import eventplanner.features.timeline.dto.request.CreateTimelineItemRequest;
import eventplanner.features.timeline.dto.request.TimelineDependencyBatchRequest;
import eventplanner.features.timeline.dto.request.TimelinePublishRequest;
import eventplanner.features.timeline.dto.request.TimelineReorderRequest;
import eventplanner.features.timeline.dto.request.UpdateTimelineItemRequest;
import eventplanner.features.timeline.dto.request.WorkbackRequest;
import eventplanner.features.timeline.dto.response.TimelineItemResponse;
import eventplanner.features.timeline.dto.response.WorkbackMilestone;
import eventplanner.features.timeline.service.TimelineService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timeline")
@Tag(name = "Timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/{eventId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get run-of-show timeline")
    public ResponseEntity<List<TimelineItemResponse>> getTimeline(@PathVariable("eventId") UUID eventId) {
        return ResponseEntity.ok(timelineService.list(eventId));
    }

    @PostMapping("/{eventId}/workback")
    @RequiresPermission(value = RbacPermissions.TIMELINE_WORKBACK_GENERATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Generate workback schedule from event date")
    public ResponseEntity<List<WorkbackMilestone>> generateWorkback(
            @PathVariable("eventId") UUID eventId,
            @Valid @RequestBody WorkbackRequest request
    ) {
        LocalDate eventDate = LocalDate.parse(request.getEventDate());

        List<WorkbackMilestone> milestones = List.of(
                createMilestone("Send Save-the-Dates", -60, eventDate, "high", "communication"),
                createMilestone("Confirm Vendors", -30, eventDate, "high", "vendor"),
                createMilestone("Finalize Guest List", -14, eventDate, "medium", "guest"),
                createMilestone("Run-of-Show Draft", -7, eventDate, "high", "planning"),
                createMilestone("Final Confirmations", -2, eventDate, "critical", "final")
        );
        return ResponseEntity.ok(milestones);
    }

    @PostMapping
    @RequiresPermission(value = RbacPermissions.TIMELINE_CREATE, resources = {"event_id=#request.eventId"})
    @Operation(summary = "Create timeline item")
    public ResponseEntity<TimelineItemResponse> create(@Valid @RequestBody CreateTimelineItemRequest request,
                                                       @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(timelineService.create(request, user));
    }

    @PutMapping("/{eventId}/items/{itemId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Update timeline item")
    public ResponseEntity<TimelineItemResponse> update(@PathVariable("eventId") UUID eventId,
                                                       @PathVariable("itemId") UUID itemId,
                                                       @Valid @RequestBody UpdateTimelineItemRequest request,
                                                       @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(timelineService.update(eventId, itemId, request, user));
    }

    @PatchMapping("/{eventId}/order")
    @RequiresPermission(value = RbacPermissions.TIMELINE_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Reorder timeline items")
    public ResponseEntity<List<TimelineItemResponse>> reorder(@PathVariable("eventId") UUID eventId,
                                                              @Valid @RequestBody TimelineReorderRequest request,
                                                              @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(timelineService.reorder(eventId, request, user));
    }

    @PatchMapping("/{eventId}/dependencies")
    @RequiresPermission(value = RbacPermissions.TIMELINE_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Batch update timeline dependencies")
    public ResponseEntity<List<TimelineItemResponse>> updateDependencies(@PathVariable("eventId") UUID eventId,
                                                                         @Valid @RequestBody TimelineDependencyBatchRequest request,
                                                                         @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(timelineService.updateDependencies(eventId, request, user));
    }

    @PatchMapping("/{eventId}/publish")
    @RequiresPermission(value = RbacPermissions.TIMELINE_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Publish or unpublish timeline")
    public ResponseEntity<Void> publishTimeline(@PathVariable("eventId") UUID eventId,
                                                @Valid @RequestBody TimelinePublishRequest request,
                                                @AuthenticationPrincipal UserPrincipal user) {
        timelineService.publishTimeline(eventId, request, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{eventId}/items/{itemId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_DELETE, resources = {"event_id=#eventId"})
    @Operation(summary = "Delete timeline item")
    public ResponseEntity<Void> delete(@PathVariable("eventId") UUID eventId,
                                       @PathVariable("itemId") UUID itemId,
                                       @AuthenticationPrincipal UserPrincipal user) {
        timelineService.delete(eventId, itemId, user);
        return ResponseEntity.noContent().build();
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
