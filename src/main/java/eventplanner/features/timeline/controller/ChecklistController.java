package eventplanner.features.timeline.controller;

import eventplanner.features.timeline.dto.request.ChecklistAutoSaveRequest;
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
@RequestMapping("/api/v1/tasks/{taskId}/checklist")
@Tag(name = "Checklist", description = "Checklist items for a task")
@RequiredArgsConstructor
public class ChecklistController {

    private final TimelineTaskService taskService;

    @PatchMapping("/auto-save")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_UPDATE, resources = {"task_id=#taskId"})
    @Operation(summary = "Auto-save checklist item draft")
    public ResponseEntity<TaskDetailResponse.ChecklistItemResponse> autoSaveChecklistItem(
            @PathVariable UUID taskId,
            @Valid @RequestBody ChecklistAutoSaveRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(taskService.autoSaveChecklistItem(taskId, request, user));
    }

    @PutMapping("/{itemId}/finalize")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_UPDATE, resources = {"task_id=#taskId"})
    @Operation(summary = "Finalize checklist item", description = "Take item out of draft or delete if empty")
    public ResponseEntity<TaskDetailResponse.ChecklistItemResponse> finalizeChecklistItem(
            @PathVariable UUID taskId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ChecklistAutoSaveRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return taskService.finalizeChecklistItem(taskId, itemId, request, user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/order")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_POSITION_UPDATE, resources = {"task_id=#taskId"})
    @Operation(summary = "Update checklist items order")
    public ResponseEntity<Void> updateOrder(
            @PathVariable UUID taskId,
            @RequestBody List<UUID> itemIds,
            @AuthenticationPrincipal UserPrincipal user) {
        taskService.updateChecklistOrders(taskId, itemIds, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_DELETE, resources = {"task_id=#taskId"})
    @Operation(summary = "Delete checklist item")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID taskId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal UserPrincipal user) {
        taskService.deleteChecklistItem(taskId, itemId, user);
        return ResponseEntity.noContent().build();
    }
}

