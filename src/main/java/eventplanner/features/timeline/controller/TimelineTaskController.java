package eventplanner.features.timeline.controller;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.features.timeline.dto.request.*;
import eventplanner.features.timeline.dto.response.*;
import eventplanner.features.event.service.EventMediaService;
import eventplanner.features.event.dto.request.EventMediaUploadRequest;
import eventplanner.features.event.dto.response.EventPresignedUploadResponse;
import eventplanner.features.timeline.service.TimelineTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;

/**
 * Comprehensive Timeline Task Controller - FAANG-level implementation
 * Handles all timeline task operations including hierarchy, visualization, and management
 */
@RestController
@RequestMapping("/api/v1/timeline")
@Tag(name = "Timeline Tasks", description = "Comprehensive timeline task management with hierarchy and visualization")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class TimelineTaskController {
    
    private final TimelineTaskService timelineTaskService;
    private final EventMediaService eventMediaService;
    
    // ==================== Timeline View Endpoints ====================
    
    @GetMapping("/{eventId}/view")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Get timeline view",
        description = "Get optimized timeline view data for visualization with filters"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Timeline view retrieved successfully",
            content = @Content(schema = @Schema(implementation = TimelineViewResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<TimelineViewResponse> getTimelineView(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @Parameter(description = "View type: daily, weekly, monthly")
            @RequestParam(required = false, defaultValue = "daily") String viewType,
            
            @Parameter(description = "Start date filter (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date filter (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "Status filter")
            @RequestParam(required = false) TimelineStatus status,
            
            @Parameter(description = "Assignee ID filter")
            @RequestParam(required = false) UUID assigneeId,
            
            @Parameter(description = "Category filter")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Include subtasks in response")
            @RequestParam(required = false, defaultValue = "true") Boolean includeSubtasks,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        TimelineViewResponse response = timelineTaskService.getTimelineView(
            eventId, viewType, startDate, endDate, status, assigneeId, category, includeSubtasks, user
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{eventId}/tasks")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_READ, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Get tasks with hierarchy",
        description = "Get all tasks for an event with hierarchical structure (parent/child)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<List<TaskResponse>> getTasks(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @Parameter(description = "Status filter")
            @RequestParam(required = false) TimelineStatus status,
            
            @Parameter(description = "Assignee ID filter")
            @RequestParam(required = false) UUID assigneeId,
            
            @Parameter(description = "Category filter")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Include subtasks")
            @RequestParam(required = false, defaultValue = "true") Boolean includeSubtasks,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        List<TaskResponse> tasks = timelineTaskService.getTasks(
            eventId, status, assigneeId, category, includeSubtasks, user
        );
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{eventId}/summary")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Get timeline summary",
        description = "Get comprehensive timeline summary with statistics, upcoming tasks, and overdue items"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully",
            content = @Content(schema = @Schema(implementation = TimelineSummaryResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<TimelineSummaryResponse> getTimelineSummary(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        TimelineSummaryResponse summary = timelineTaskService.getTimelineSummary(eventId, user);
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/{eventId}/upcoming")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Get upcoming tasks",
        description = "Get tasks due in the next N days (default 7 days)"
    )
    public ResponseEntity<List<TaskResponse>> getUpcomingTasks(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @Parameter(description = "Number of days to look ahead")
            @RequestParam(required = false) Integer days,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        List<TaskResponse> tasks = timelineTaskService.getUpcomingTasks(eventId, days, user);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{eventId}/overdue")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Get overdue tasks",
        description = "Get all overdue tasks for an event"
    )
    public ResponseEntity<List<TaskResponse>> getOverdueTasks(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        List<TaskResponse> tasks = timelineTaskService.getOverdueTasks(eventId, user);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{eventId}/search")
    @RequiresPermission(value = RbacPermissions.TIMELINE_READ, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Search tasks",
        description = "Search tasks by title or description"
    )
    public ResponseEntity<List<TaskResponse>> searchTasks(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        List<TaskResponse> tasks = timelineTaskService.searchTasks(eventId, query, user);
        return ResponseEntity.ok(tasks);
    }
    
    // ==================== Task CRUD Endpoints ====================
    
    @PostMapping("/{eventId}/tasks")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_CREATE, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Create task",
        description = "Create a new task with optional subtasks"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task created successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<TaskResponse> createTask(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @Valid @RequestBody CreateTaskRequest request,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Ensure eventId in request matches path variable
        request.setEventId(eventId);
        
        TaskResponse task = timelineTaskService.createTask(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }
    
    @GetMapping("/tasks/{taskId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_READ, resources = {"task_id=#taskId"})
    @Operation(
        summary = "Get task by ID",
        description = "Get a specific task with its subtasks"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        // This will be implemented in the service - for now return through tasks endpoint
        // TODO: Add getTaskById method to service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @PutMapping("/tasks/{taskId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_UPDATE, resources = {"task_id=#taskId"})
    @Operation(
        summary = "Update task",
        description = "Update an existing task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> updateTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Valid @RequestBody UpdateTaskRequest request,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        TaskResponse task = timelineTaskService.updateTask(taskId, request, user);
        return ResponseEntity.ok(task);
    }
    
    @PutMapping("/tasks/{taskId}/position")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_POSITION_UPDATE, resources = {"task_id=#taskId"})
    @Operation(
        summary = "Update task position",
        description = "Update task position on timeline (drag-and-drop operation)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task position updated successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> updateTaskPosition(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Valid @RequestBody UpdateTaskPositionRequest request,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        TaskResponse task = timelineTaskService.updateTaskPosition(taskId, request, user);
        return ResponseEntity.ok(task);
    }
    
    @PutMapping("/{eventId}/tasks/bulk")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_UPDATE, resources = {"event_id=#eventId"})
    @Operation(
        summary = "Bulk update tasks",
        description = "Update multiple tasks in a single request (max 50 tasks)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks updated successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or too many updates"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<List<TaskResponse>> bulkUpdateTasks(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId,
            
            @Valid @RequestBody BulkUpdateTaskRequest request,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        List<TaskResponse> tasks = timelineTaskService.bulkUpdateTasks(eventId, request, user);
        return ResponseEntity.ok(tasks);
    }
    
    @DeleteMapping("/tasks/{taskId}")
    @RequiresPermission(value = RbacPermissions.TIMELINE_TASK_DELETE, resources = {"task_id=#taskId"})
    @Operation(
        summary = "Delete task",
        description = "Delete a task and all its subtasks"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        timelineTaskService.deleteTask(taskId, user);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== Proof Image Endpoints ====================
    
    @PostMapping("/tasks/{taskId}/proof/upload-url")
    @RequiresPermission(value = RbacPermissions.TIMELINE_PROOF_UPLOAD, resources = {"task_id=#taskId"})
    @Operation(
        summary = "Get presigned URL for proof image upload",
        description = "Get a presigned URL to upload proof image for a task. Only assigned users or owners can upload."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Upload URL generated successfully",
            content = @Content(schema = @Schema(implementation = EventPresignedUploadResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<EventPresignedUploadResponse> getProofUploadUrl(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Valid @RequestBody EventMediaUploadRequest request,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        // Get task to check permissions and get event ID
        eventplanner.features.timeline.entity.TimelineItem task = timelineTaskService.getTaskEntityForUpload(taskId, user);
        
        // Generate presigned URL using event media service
        if (task.getEvent() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Event not found for task");
        }
        EventPresignedUploadResponse response = eventMediaService.createMediaUpload(
            task.getEvent().getId(), user, request
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/tasks/{taskId}/proof")
    @RequiresPermission(value = RbacPermissions.TIMELINE_PROOF_UPLOAD, resources = {"task_id=#taskId"})
    @Operation(
        summary = "Upload proof image",
        description = "Mark a proof image as uploaded for a task. Only assigned users or owners can upload."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proof image uploaded successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> uploadProofImage(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Valid @RequestBody UploadProofImageRequest request,
            
            @AuthenticationPrincipal UserPrincipal user) {
        
        TaskResponse task = timelineTaskService.uploadProofImage(taskId, request, user);
        return ResponseEntity.ok(task);
    }
}
