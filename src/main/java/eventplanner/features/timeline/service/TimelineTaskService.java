package eventplanner.features.timeline.service;

import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.features.collaboration.enums.EventPermission;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.collaboration.service.EventPermissionEvaluator;
import eventplanner.features.timeline.enums.TimelineStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.event.service.EventAccessControlService;
import eventplanner.features.timeline.dto.request.*;
import eventplanner.features.timeline.dto.response.*;
import eventplanner.features.timeline.entity.Task;
import eventplanner.features.timeline.entity.Checklist;
import eventplanner.features.timeline.repository.TaskRepository;
import eventplanner.features.timeline.repository.ChecklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Refactored service for Task and Checklist management with split entities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TimelineTaskService {
    
    private final TaskRepository taskRepository;
    private final ChecklistRepository checklistRepository;
    private final EventRepository eventRepository;
    private final EventUserRepository eventUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthorizationService authorizationService;
    private final EventPermissionEvaluator eventPermissionEvaluator;
    private final NotificationService notificationService;
    private final EventAccessControlService accessControlService;
    
    private Event requireTimelineReadAccess(UserPrincipal user, UUID eventId) {
        return requireTimelineAccess(user, eventId, false);
    }

    private Event requireTimelineManageAccess(UserPrincipal user, UUID eventId) {
        return requireTimelineAccess(user, eventId, true);
    }

    private Event requireTimelineAccess(UserPrincipal user, UUID eventId, boolean requiresManage) {
        Event event = ensureEventExists(eventId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: authentication required");
        }
        if (authorizationService != null) {
            if (authorizationService.isAdmin(user) || authorizationService.isEventOwner(user, eventId)) {
                return event;
            }
        }
        EventUser membership = eventUserRepository.findByEventIdAndUserId(eventId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: you are not a collaborator on this event"));

        if (requiresManage) {
            if (!eventPermissionEvaluator.hasPermission(membership, EventPermission.MANAGE_SCHEDULE)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: missing schedule permission");
            }
        } else {
            if (!eventPermissionEvaluator.hasAnyPermission(
                membership,
                EventPermission.VIEW_EVENT,
                EventPermission.MANAGE_SCHEDULE
            )) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: missing view permission");
            }
        }
        return event;
    }
    
    private Event ensureEventExists(UUID eventId) {
        return accessControlService.ensureEventExists(eventId);
    }

    /**
     * Get all tasks for an event with batch-loaded checklists to avoid N+1 queries.
     */
    public List<TaskDetailResponse> getAllTasks(UUID eventId, UserPrincipal user) {
        requireTimelineReadAccess(user, eventId);
        List<Task> tasks = taskRepository.findByEventIdOrderByTaskOrderAsc(eventId);
        
        if (tasks.isEmpty()) {
            return List.of();
        }

        // Batch load all checklists for all tasks in a single query
        List<UUID> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        List<Checklist> allChecklists = checklistRepository.findByTaskIdInOrderByTaskOrderAsc(taskIds);
        
        // Group checklists by task ID
        Map<UUID, List<Checklist>> checklistsByTaskId = allChecklists.stream()
                .collect(Collectors.groupingBy(c -> c.getTask().getId()));

        return tasks.stream()
                .map(task -> mapToTaskDetailResponseWithChecklists(task, 
                    checklistsByTaskId.getOrDefault(task.getId(), List.of())))
                .collect(Collectors.toList());
    }

    /**
     * Auto-save task draft
     */
    public TaskDetailResponse autoSaveTask(UUID eventId, TaskAutoSaveRequest request, UserPrincipal user) {
        Event event = requireTimelineManageAccess(user, eventId);
        
        Task task;
        UUID previousAssignee = null;
        if (request.getId() != null) {
            task = taskRepository.findByIdAndEventId(request.getId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
            previousAssignee = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
        } else {
            task = new Task();
            task.setEvent(event);
            task.setStatus(TimelineStatus.TO_DO);
            task.setProgressPercentage(0);
            task.setIsDraft(true);
        }
        
        updateTaskFields(task, request);
        
        task = taskRepository.save(task);
        notifyTaskAssignmentIfChanged(task, previousAssignee);
        return mapToTaskDetailResponse(task);
    }

    /**
     * Finalize task draft
     */
    public Optional<TaskDetailResponse> finalizeTask(UUID eventId, UUID taskId, TaskAutoSaveRequest request, UserPrincipal user) {
        requireTimelineManageAccess(user, eventId);
        
        Task task = taskRepository.findByIdAndEventId(taskId, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        UUID previousAssignee = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
            
        updateTaskFields(task, request);
        
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            taskRepository.delete(task);
            return Optional.empty();
        }
        
        task.setIsDraft(false);
        task = taskRepository.save(task);
        notifyTaskAssignmentIfChanged(task, previousAssignee);
        return Optional.of(mapToTaskDetailResponse(task));
    }

    private void updateTaskFields(Task task, TaskAutoSaveRequest request) {
        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getCategory() != null) task.setCategory(request.getCategory());
        if (request.getTaskOrder() != null) task.setTaskOrder(request.getTaskOrder());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        
        if (request.getAssignedTo() != null) {
            UserAccount assignedUser = resolveAssignee(task.getEvent(), request.getAssignedTo());
            task.setAssignedTo(assignedUser);
        }
    }

    /**
     * Auto-save checklist item draft
     */
    public TaskDetailResponse.ChecklistItemResponse autoSaveChecklistItem(UUID taskId, ChecklistAutoSaveRequest request, UserPrincipal user) {
        Task parentTask = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));
        
        requireTimelineManageAccess(user, parentTask.getEvent().getId());
        
        Checklist item;
        UUID previousAssignee = null;
        if (request.getId() != null) {
            item = checklistRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));
            if (!item.getTask().getId().equals(taskId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this task");
            }
            previousAssignee = item.getAssignedTo() != null ? item.getAssignedTo().getId() : null;
        } else {
            item = new Checklist();
            item.setTask(parentTask);
            item.setStatus(TimelineStatus.TO_DO);
            item.setIsDraft(true);
        }
        
        updateChecklistFields(item, request);
        
        item = checklistRepository.save(item);
        recalculateTaskProgress(taskId);
        notifyChecklistAssignmentIfChanged(item, previousAssignee);
        
        return mapToChecklistItemResponse(item);
    }

    /**
     * Finalize checklist item draft
     */
    public Optional<TaskDetailResponse.ChecklistItemResponse> finalizeChecklistItem(UUID taskId, UUID itemId, ChecklistAutoSaveRequest request, UserPrincipal user) {
        Task parentTask = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));
            
        requireTimelineManageAccess(user, parentTask.getEvent().getId());
        
        Checklist item = checklistRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));
        UUID previousAssignee = item.getAssignedTo() != null ? item.getAssignedTo().getId() : null;
            
        if (!item.getTask().getId().equals(taskId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this task");
        }
        
        updateChecklistFields(item, request);
        
        if (item.getTitle() == null || item.getTitle().isBlank()) {
            checklistRepository.delete(item);
            recalculateTaskProgress(taskId);
            return Optional.empty();
        }
        
        item.setIsDraft(false);
        item = checklistRepository.save(item);
        recalculateTaskProgress(taskId);
        notifyChecklistAssignmentIfChanged(item, previousAssignee);
        
        return Optional.of(mapToChecklistItemResponse(item));
    }

    private void updateChecklistFields(Checklist item, ChecklistAutoSaveRequest request) {
        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getDueDate() != null) item.setDueDate(request.getDueDate());
        if (request.getTaskOrder() != null) item.setTaskOrder(request.getTaskOrder());
        if (request.getStatus() != null) item.setStatus(request.getStatus());
        
        if (request.getAssignedTo() != null) {
            UserAccount assignedUser = resolveAssignee(item.getTask().getEvent(), request.getAssignedTo());
            item.setAssignedTo(assignedUser);
        }
    }

    /**
     * Update task orders
     */
    public void updateTaskOrders(UUID eventId, List<UUID> taskIds, UserPrincipal user) {
        requireTimelineManageAccess(user, eventId);
        for (int i = 0; i < taskIds.size(); i++) {
            UUID taskId = taskIds.get(i);
            int order = i;
            taskRepository.findByIdAndEventId(taskId, eventId).ifPresent(task -> {
                task.setTaskOrder(order);
                taskRepository.save(task);
            });
        }
    }

    /**
     * Update checklist orders
     */
    public void updateChecklistOrders(UUID taskId, List<UUID> itemIds, UserPrincipal user) {
        Task parentTask = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        requireTimelineManageAccess(user, parentTask.getEvent().getId());
        
        for (int i = 0; i < itemIds.size(); i++) {
            UUID itemId = itemIds.get(i);
            int order = i;
            checklistRepository.findById(itemId).ifPresent(item -> {
                if (item.getTask().getId().equals(taskId)) {
                    item.setTaskOrder(order);
                    checklistRepository.save(item);
                }
            });
        }
    }

    /**
     * Delete task
     */
    public void deleteTask(UUID taskId, UserPrincipal user) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        requireTimelineManageAccess(user, task.getEvent().getId());
        taskRepository.delete(task);
    }

    /**
     * Delete checklist item
     */
    public void deleteChecklistItem(UUID taskId, UUID itemId, UserPrincipal user) {
        Checklist item = checklistRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
            
        if (!item.getTask().getId().equals(taskId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this task");
        }
        
        requireTimelineManageAccess(user, item.getTask().getEvent().getId());
        
        checklistRepository.delete(item);
        recalculateTaskProgress(taskId);
    }

    private UserAccount resolveAssignee(Event event, UUID userId) {
        if (event == null || userId == null) {
            return null;
        }
        UserAccount assignedUser = userAccountRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (event.getOwner() != null && userId.equals(event.getOwner().getId())) {
            return assignedUser;
        }
        if (eventUserRepository.findByEventIdAndUserId(event.getId(), userId).isPresent()) {
            return assignedUser;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Assignee must be a collaborator on this event");
    }
    
    private void notifyTaskAssignmentIfChanged(Task task, UUID previousAssignee) {
        if (task == null || task.getAssignedTo() == null || task.getAssignedTo().getId() == null) {
            return;
        }
        if (Boolean.TRUE.equals(task.getIsDraft())) {
            return;
        }
        UUID currentAssignee = task.getAssignedTo().getId();
        if (previousAssignee != null && previousAssignee.equals(currentAssignee)) {
            return;
        }
        Event event = task.getEvent();
        if (event == null) {
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("body", "You were assigned to task: " + (task.getTitle() != null ? task.getTitle() : "Task"));
        if (task.getId() != null) {
            data.put("taskId", task.getId().toString());
        }
        if (event.getId() != null) {
            data.put("eventId", event.getId().toString());
        }
        if (task.getDueDate() != null) {
            data.put("dueDate", task.getDueDate().toString());
        }
        
        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.PUSH_NOTIFICATION)
                .to(currentAssignee.toString())
                .subject("New task assignment")
                .templateVariables(data)
                .eventId(event.getId())
                .build());
    }
    
    private void notifyChecklistAssignmentIfChanged(Checklist item, UUID previousAssignee) {
        if (item == null || item.getAssignedTo() == null || item.getAssignedTo().getId() == null) {
            return;
        }
        if (Boolean.TRUE.equals(item.getIsDraft())) {
            return;
        }
        UUID currentAssignee = item.getAssignedTo().getId();
        if (previousAssignee != null && previousAssignee.equals(currentAssignee)) {
            return;
        }
        Task task = item.getTask();
        if (task == null || task.getEvent() == null) {
            return;
        }
        Event event = task.getEvent();
        
        Map<String, Object> data = new HashMap<>();
        data.put("body", "You were assigned to checklist: " + (item.getTitle() != null ? item.getTitle() : "Checklist"));
        if (task.getId() != null) {
            data.put("taskId", task.getId().toString());
        }
        if (item.getId() != null) {
            data.put("checklistId", item.getId().toString());
        }
        if (event.getId() != null) {
            data.put("eventId", event.getId().toString());
        }
        if (item.getDueDate() != null) {
            data.put("dueDate", item.getDueDate().toString());
        }
        
        notificationService.send(NotificationRequest.builder()
                .type(CommunicationType.PUSH_NOTIFICATION)
                .to(currentAssignee.toString())
                .subject("New checklist assignment")
                .templateVariables(data)
                .eventId(event.getId())
                .build());
    }

    private TaskDetailResponse mapToTaskDetailResponse(Task task) {
        List<Checklist> checklistEntities = checklistRepository.findByTaskIdOrderByTaskOrderAsc(task.getId());
        return mapToTaskDetailResponseWithChecklists(task, checklistEntities);
    }

    /**
     * Map task to response using pre-loaded checklists (avoids N+1 queries).
     */
    private TaskDetailResponse mapToTaskDetailResponseWithChecklists(Task task, List<Checklist> checklistEntities) {
        List<TaskDetailResponse.ChecklistItemResponse> checklist = checklistEntities.stream()
            .map(this::mapToChecklistItemResponse)
            .collect(Collectors.toList());
            
        return TaskDetailResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .startDate(task.getStartDate())
            .dueDate(task.getDueDate())
            .priority(task.getPriority())
            .category(task.getCategory())
            .status(task.getStatus())
            .progressPercentage(task.getProgressPercentage())
            .assignedTo(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
            .assignedToName(task.getAssignedTo() != null ? task.getAssignedTo().getName() : null)
            .taskOrder(task.getTaskOrder())
            .completedSubtasksCount(task.getCompletedSubtasksCount())
            .totalSubtasksCount(task.getTotalSubtasksCount())
            .isDraft(task.getIsDraft())
            .checklist(checklist)
            .build();
    }

    private TaskDetailResponse.ChecklistItemResponse mapToChecklistItemResponse(Checklist item) {
        return TaskDetailResponse.ChecklistItemResponse.builder()
            .id(item.getId())
            .title(item.getTitle())
            .description(item.getDescription())
            .dueDate(item.getDueDate())
            .status(item.getStatus())
            .assignedTo(item.getAssignedTo() != null ? item.getAssignedTo().getId() : null)
            .assignedToName(item.getAssignedTo() != null ? item.getAssignedTo().getName() : null)
            .taskOrder(item.getTaskOrder())
            .isDraft(item.getIsDraft())
            .build();
    }

    /**
     * Recalculate task progress with pessimistic locking to prevent race conditions
     * from concurrent checklist updates.
     */
    private void recalculateTaskProgress(UUID taskId) {
        // Acquire pessimistic write lock to prevent concurrent progress updates
        Task task = taskRepository.findByIdForUpdate(taskId).orElse(null);
        if (task == null) return;
        
        List<Checklist> subtasks = checklistRepository.findByTaskIdOrderByTaskOrderAsc(taskId);
        if (subtasks.isEmpty()) {
            task.setProgressPercentage(0);
            task.setCompletedSubtasksCount(0);
            task.setTotalSubtasksCount(0);
        } else {
            int total = subtasks.size();
            int completed = (int) subtasks.stream()
                .filter(t -> t.getStatus() == TimelineStatus.COMPLETED || t.getStatus() == TimelineStatus.DONE)
                .count();
            
            int progress = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;
            
            task.setTotalSubtasksCount(total);
            task.setCompletedSubtasksCount(completed);
            task.setProgressPercentage(progress);
            
            // Never auto-update status for manually-set POSTPONED or CANCELLED tasks.
            if (task.getStatus() != TimelineStatus.POSTPONED
                    && task.getStatus() != TimelineStatus.CANCELLED) {
                if (progress >= 100) {
                    task.setStatus(TimelineStatus.COMPLETED);
                } else if (progress > 0) {
                    task.setStatus(TimelineStatus.ACTIVE);
                } else {
                    task.setStatus(TimelineStatus.TO_DO);
                }
            }
        }
        
        taskRepository.save(task);
    }
}
