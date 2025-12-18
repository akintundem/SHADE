package eventplanner.features.timeline.controller;

import eventplanner.features.timeline.dto.request.TaskAutoSaveRequest;
import eventplanner.features.timeline.dto.response.TaskDetailResponse;
import eventplanner.features.timeline.service.TimelineTaskService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/tasks")
@Tag(name = "Tasks", description = "High-level task management")
@RequiredArgsConstructor
public class TaskController {

    private final TimelineTaskService taskService;

    @GetMapping
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get all tasks for an event")
    public ResponseEntity<List<TaskDetailResponse>> getAllTasks(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(taskService.getAllTasks(eventId, user));
    }

    @PatchMapping("/auto-save")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_CREATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Auto-save task draft")
    public ResponseEntity<TaskDetailResponse> autoSaveTask(
            @PathVariable UUID eventId,
            @Valid @RequestBody TaskAutoSaveRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(taskService.autoSaveTask(eventId, request, user));
    }

    @PutMapping("/{taskId}/finalize")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Finalize task", description = "Take task out of draft or delete if empty")
    public ResponseEntity<TaskDetailResponse> finalizeTask(
            @PathVariable UUID eventId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskAutoSaveRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return taskService.finalizeTask(eventId, taskId, request, user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/order")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_POSITION_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Update tasks order")
    public ResponseEntity<Void> updateOrder(
            @PathVariable UUID eventId,
            @RequestBody List<UUID> taskIds,
            @AuthenticationPrincipal UserPrincipal user) {
        taskService.updateTaskOrders(eventId, taskIds, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_DELETE, resources = {"event_id=#eventId"})
    @Operation(summary = "Delete task and its checklist")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID eventId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal user) {
        taskService.deleteTask(taskId, user);
        return ResponseEntity.noContent().build();
    }
}

