package eventplanner.features.timeline.service;

import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.timeline.dto.request.*;
import eventplanner.features.timeline.dto.response.*;
import eventplanner.features.timeline.entity.TimelineItem;
import eventplanner.features.timeline.repository.TimelineItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive timeline task service with FAANG-level implementation
 * Handles task hierarchy, progress tracking, timeline visualization, and business logic
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TimelineTaskService {
    
    private final TimelineItemRepository repository;
    private final EventRepository eventRepository;
    private final AuthorizationService authorizationService;
    private final TimelineHistoryService timelineHistoryService;
    
    private static final int MAX_BULK_OPERATIONS = 50;
    private static final int DEFAULT_UPCOMING_DAYS = 7;
    
    /**
     * Validate event access
     */
    private void validateEventAccess(UserPrincipal user, UUID eventId) {
        if (!authorizationService.canAccessEvent(user, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Access denied: You do not have permission to access this event");
        }
    }
    
    /**
     * Ensure event exists
     */
    private Event ensureEventExists(UUID eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Event not found: " + eventId));
    }
    
    /**
     * Get task by ID with event validation
     */
    private TimelineItem getTaskById(UUID taskId, UUID eventId) {
        return repository.findByIdAndEventId(taskId, eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Task not found: " + taskId));
    }
    
    /**
     * Create a task with optional subtasks
     */
    public TaskResponse createTask(CreateTaskRequest request, UserPrincipal user) {
        validateEventAccess(user, request.getEventId());
        ensureEventExists(request.getEventId());
        
        // Create parent task
        TimelineItem task = new TimelineItem();
        task.setEventId(request.getEventId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
        task.setDueDate(request.getDueDate());
        task.setScheduledAt(task.getStartDate());
        task.setDurationMinutes(request.getDurationMinutes());
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setCategory(request.getCategory());
        task.setAssignedTo(request.getAssignedTo());
        task.setStatus(request.getStatus() != null ? request.getStatus() : TimelineStatus.TO_DO);
        task.setParentTaskId(request.getParentTaskId());
        task.setTaskOrder(request.getTaskOrder());
        task.setIsPreview(request.getIsPreview() != null ? request.getIsPreview() : false);
        task.setProgressPercentage(0);
        
        // Calculate end time
        if (task.getStartDate() != null && task.getDurationMinutes() != null) {
            task.setEndTime(task.getStartDate().plusMinutes(task.getDurationMinutes()));
        } else if (task.getDueDate() != null) {
            task.setEndTime(task.getDueDate());
        }
        
        // Handle dependencies
        if (request.getDependencies() != null && !request.getDependencies().isEmpty()) {
            validateDependencies(request.getEventId(), request.getDependencies());
            task.setDependencies(request.getDependencies().toArray(new UUID[0]));
        }
        
        // Determine if this is a parent task
        boolean hasSubtasks = request.getSubtasks() != null && !request.getSubtasks().isEmpty();
        task.setIsParentTask(hasSubtasks);
        
        if (hasSubtasks) {
            task.setTotalSubtasksCount(request.getSubtasks().size());
            task.setCompletedSubtasksCount(0);
        }
        
        // Save parent task
        task = repository.save(task);
        
        // Create subtasks if provided
        List<TaskResponse> subtasks = new ArrayList<>();
        if (hasSubtasks) {
            int order = 0;
            for (CreateTaskRequest.CreateSubtaskRequest subtaskReq : request.getSubtasks()) {
                TimelineItem subtask = new TimelineItem();
                subtask.setEventId(request.getEventId());
                subtask.setTitle(subtaskReq.getTitle());
                subtask.setDescription(subtaskReq.getDescription());
                subtask.setStartDate(subtaskReq.getStartDate() != null ? subtaskReq.getStartDate() : task.getStartDate());
                subtask.setDueDate(subtaskReq.getDueDate() != null ? subtaskReq.getDueDate() : task.getDueDate());
                subtask.setScheduledAt(subtask.getStartDate());
                subtask.setDurationMinutes(subtaskReq.getDurationMinutes());
                subtask.setPriority(subtaskReq.getPriority() != null ? subtaskReq.getPriority() : "MEDIUM");
                subtask.setAssignedTo(subtaskReq.getAssignedTo());
                subtask.setStatus(subtaskReq.getStatus() != null ? subtaskReq.getStatus() : TimelineStatus.TO_DO);
                subtask.setParentTaskId(task.getId());
                subtask.setTaskOrder(subtaskReq.getTaskOrder() != null ? subtaskReq.getTaskOrder() : order++);
                subtask.setIsParentTask(false);
                subtask.setIsPreview(subtaskReq.getIsPreview() != null ? subtaskReq.getIsPreview() : false);
                subtask.setProgressPercentage(0);
                
                if (subtask.getStartDate() != null && subtask.getDurationMinutes() != null) {
                    subtask.setEndTime(subtask.getStartDate().plusMinutes(subtask.getDurationMinutes()));
                } else if (subtask.getDueDate() != null) {
                    subtask.setEndTime(subtask.getDueDate());
                }
                
                subtask = repository.save(subtask);
                subtasks.add(mapToTaskResponse(subtask, null));
            }
        }
        
        // Recalculate parent progress if needed
        if (hasSubtasks) {
            recalculateParentProgress(task.getId());
        }
        
        TaskResponse response = mapToTaskResponse(task, subtasks);
        log.info("Created task {} for event {} by user {}", task.getId(), request.getEventId(), user.getId());
        return response;
    }
    
    /**
     * Check if user can update task
     * Owners can update everything, assigned users can only update their assigned tasks
     */
    private void validateTaskUpdatePermission(TimelineItem task, UserPrincipal user, boolean isOwnerUpdate) {
        validateEventAccess(user, task.getEventId());
        
        // If user is not owner, check if they are assigned to this task
        if (!isOwnerUpdate && task.getAssignedTo() != null) {
            if (!task.getAssignedTo().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You can only update tasks assigned to you");
            }
        }
    }
    
    /**
     * Check if user is event owner
     */
    private boolean isEventOwner(UserPrincipal user, UUID eventId) {
        return authorizationService.isEventOwner(user, eventId);
    }
    
    /**
     * Update task
     * Owners can update everything, assigned users can update their assigned tasks
     */
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UserPrincipal user) {
        TimelineItem task = repository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        
        boolean isOwner = isEventOwner(user, task.getEventId());
        
        // Restrict certain fields to owners only
        if (!isOwner) {
            // Assigned users cannot change: title, description, assignedTo, parentTaskId, dependencies
            if (request.getTitle() != null || request.getDescription() != null || 
                request.getAssignedTo() != null || request.getParentTaskId() != null || 
                request.getDependencies() != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Only event owners can modify task structure");
            }
        }
        
        validateTaskUpdatePermission(task, user, isOwner);
        
        // Update fields
        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
            task.setScheduledAt(request.getStartDate());
        }
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getDurationMinutes() != null) task.setDurationMinutes(request.getDurationMinutes());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getCategory() != null) task.setCategory(request.getCategory());
        if (request.getAssignedTo() != null) task.setAssignedTo(request.getAssignedTo());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getProgressPercentage() != null) {
            task.setProgressPercentage(Math.max(0, Math.min(100, request.getProgressPercentage())));
        }
        if (request.getIsPreview() != null) task.setIsPreview(request.getIsPreview());
        if (request.getParentTaskId() != null) {
            validateParentTask(request.getParentTaskId(), task.getEventId(), taskId);
            task.setParentTaskId(request.getParentTaskId());
        }
        if (request.getTaskOrder() != null) task.setTaskOrder(request.getTaskOrder());
        if (request.getDependencies() != null) {
            validateDependencies(task.getEventId(), request.getDependencies());
            task.setDependencies(request.getDependencies().toArray(new UUID[0]));
        }
        
        // Payment and proof fields
        if (request.getPaymentDate() != null) task.setPaymentDate(request.getPaymentDate());
        if (request.getProofImageUrl() != null) task.setProofImageUrl(request.getProofImageUrl());
        if (request.getProofImageUrls() != null) {
            // Store as JSON array string
            try {
                ObjectMapper mapper = new ObjectMapper();
                task.setProofImageUrls(mapper.writeValueAsString(request.getProofImageUrls()));
            } catch (Exception e) {
                log.error("Error serializing proof image URLs", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid proof image URLs format");
            }
        }
        
        // Recalculate end time
        if (task.getStartDate() != null && task.getDurationMinutes() != null) {
            task.setEndTime(task.getStartDate().plusMinutes(task.getDurationMinutes()));
        } else if (task.getDueDate() != null) {
            task.setEndTime(task.getDueDate());
        }
        
        // Update status based on progress
        if (request.getProgressPercentage() != null) {
            if (request.getProgressPercentage() >= 100) {
                task.setStatus(TimelineStatus.COMPLETED);
            } else if (request.getProgressPercentage() > 0) {
                task.setStatus(TimelineStatus.ACTIVE);
            }
        }
        
        task = repository.save(task);
        
        // Recalculate parent progress if this is a subtask
        if (task.getParentTaskId() != null) {
            recalculateParentProgress(task.getParentTaskId());
        }
        
        // Load subtasks if parent
        List<TaskResponse> subtasks = task.getIsParentTask() 
            ? getSubtasks(task.getId()).stream()
                .map(t -> mapToTaskResponse(t, null))
                .collect(Collectors.toList())
            : null;
        
        return mapToTaskResponse(task, subtasks);
    }
    
    /**
     * Update task position (drag-and-drop)
     */
    public TaskResponse updateTaskPosition(UUID taskId, UpdateTaskPositionRequest request, UserPrincipal user) {
        TimelineItem task = repository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        
        validateEventAccess(user, task.getEventId());
        
        task.setStartDate(request.getStartDate());
        task.setScheduledAt(request.getStartDate());
        
        if (request.getEndDate() != null) {
            task.setEndTime(request.getEndDate());
            task.setDueDate(request.getEndDate());
            
            // Calculate duration if not provided
            if (request.getDurationMinutes() == null && request.getStartDate() != null) {
                long minutes = ChronoUnit.MINUTES.between(request.getStartDate(), request.getEndDate());
                task.setDurationMinutes((int) minutes);
            } else if (request.getDurationMinutes() != null) {
                task.setDurationMinutes(request.getDurationMinutes());
                task.setEndTime(request.getStartDate().plusMinutes(request.getDurationMinutes()));
            }
        } else if (request.getDurationMinutes() != null) {
            task.setDurationMinutes(request.getDurationMinutes());
            task.setEndTime(request.getStartDate().plusMinutes(request.getDurationMinutes()));
        }
        
        task = repository.save(task);
        return mapToTaskResponse(task, null);
    }
    
    /**
     * Get timeline view with optimized data for visualization
     */
    public TimelineViewResponse getTimelineView(
            UUID eventId, 
            String viewType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            TimelineStatus status,
            UUID assigneeId,
            String category,
            Boolean includeSubtasks,
            UserPrincipal user) {
        
        validateEventAccess(user, eventId);
        
        // Get all tasks for the event
        List<TimelineItem> allTasks = repository.findByEventId(eventId);
        
        // Apply filters
        List<TimelineItem> filteredTasks = allTasks.stream()
            .filter(task -> {
                if (status != null && task.getStatus() != status) return false;
                if (assigneeId != null && !assigneeId.equals(task.getAssignedTo())) return false;
                if (category != null && !category.equals(task.getCategory())) return false;
                if (startDate != null && task.getStartDate() != null && task.getStartDate().isBefore(startDate)) return false;
                if (endDate != null && task.getStartDate() != null && task.getStartDate().isAfter(endDate)) return false;
                return true;
            })
            .collect(Collectors.toList());
        
        // Build hierarchical structure
        Map<UUID, List<TimelineItem>> subtaskMap = new HashMap<>();
        List<TimelineItem> parentTasks = new ArrayList<>();
        
        for (TimelineItem task : filteredTasks) {
            if (task.getParentTaskId() == null) {
                parentTasks.add(task);
            } else {
                subtaskMap.computeIfAbsent(task.getParentTaskId(), k -> new ArrayList<>()).add(task);
            }
        }
        
        // Sort parent tasks
        parentTasks.sort(Comparator
            .comparing(TimelineItem::getTaskOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TimelineItem::getStartDate, Comparator.nullsLast(LocalDateTime::compareTo)));
        
        // Build timeline bars
        List<TimelineViewResponse.TimelineBar> timelineBars = parentTasks.stream()
            .map(parent -> buildTimelineBar(parent, subtaskMap, includeSubtasks != null && includeSubtasks))
            .collect(Collectors.toList());
        
        // Calculate status summary
        TimelineViewResponse.StatusSummary statusSummary = calculateStatusSummary(allTasks);
        
        // Calculate overall progress
        Double avgProgress = repository.getAverageProgressForEvent(eventId);
        Integer overallProgress = avgProgress != null ? (int) Math.round(avgProgress) : 0;
        
        // Build metadata
        TimelineViewResponse.TimelineMetadata metadata = TimelineViewResponse.TimelineMetadata.builder()
            .earliestDate(repository.getEarliestDate(eventId))
            .latestDate(repository.getLatestDate(eventId))
            .tasksByAssignee(calculateTasksByAssignee(allTasks))
            .tasksByCategory(calculateTasksByCategory(allTasks))
            .build();
        
        return TimelineViewResponse.builder()
            .eventId(eventId)
            .viewType(viewType != null ? viewType : "daily")
            .viewStartDate(startDate)
            .viewEndDate(endDate)
            .timelineBars(timelineBars)
            .statusSummary(statusSummary)
            .overallProgress(overallProgress)
            .metadata(metadata)
            .build();
    }
    
    /**
     * Get tasks with hierarchical structure
     */
    public List<TaskResponse> getTasks(
            UUID eventId,
            TimelineStatus status,
            UUID assigneeId,
            String category,
            Boolean includeSubtasks,
            UserPrincipal user) {
        
        validateEventAccess(user, eventId);
        
        List<TimelineItem> tasks = repository.findByEventId(eventId);
        
        // Apply filters
        if (status != null) {
            tasks = tasks.stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
        }
        if (assigneeId != null) {
            tasks = tasks.stream()
                .filter(t -> assigneeId.equals(t.getAssignedTo()))
                .collect(Collectors.toList());
        }
        if (category != null) {
            tasks = tasks.stream()
                .filter(t -> category.equals(t.getCategory()))
                .collect(Collectors.toList());
        }
        
        // Build hierarchy
        Map<UUID, List<TimelineItem>> subtaskMap = new HashMap<>();
        List<TimelineItem> parentTasks = new ArrayList<>();
        
        for (TimelineItem task : tasks) {
            if (task.getParentTaskId() == null) {
                parentTasks.add(task);
            } else {
                subtaskMap.computeIfAbsent(task.getParentTaskId(), k -> new ArrayList<>()).add(task);
            }
        }
        
        // Sort and build response
        parentTasks.sort(Comparator
            .comparing(TimelineItem::getTaskOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TimelineItem::getStartDate, Comparator.nullsLast(LocalDateTime::compareTo)));
        
        boolean includeSubs = includeSubtasks != null && includeSubtasks;
        return parentTasks.stream()
            .map(parent -> {
                List<TaskResponse> subtasks = includeSubs && subtaskMap.containsKey(parent.getId())
                    ? subtaskMap.get(parent.getId()).stream()
                        .sorted(Comparator.comparing(TimelineItem::getTaskOrder, Comparator.nullsLast(Integer::compareTo)))
                        .map(t -> mapToTaskResponse(t, null))
                        .collect(Collectors.toList())
                    : null;
                return mapToTaskResponse(parent, subtasks);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get timeline summary
     */
    public TimelineSummaryResponse getTimelineSummary(UUID eventId, UserPrincipal user) {
        validateEventAccess(user, eventId);
        
        List<TimelineItem> allTasks = repository.findByEventId(eventId);
        
        // Calculate overall progress
        Double avgProgress = repository.getAverageProgressForEvent(eventId);
        Integer overallProgress = avgProgress != null ? (int) Math.round(avgProgress) : 0;
        
        // Status breakdown
        long total = allTasks.size();
        long toDo = allTasks.stream().filter(t -> t.getStatus() == TimelineStatus.TO_DO || t.getStatus() == TimelineStatus.PENDING).count();
        long active = allTasks.stream().filter(t -> t.getStatus() == TimelineStatus.ACTIVE || t.getStatus() == TimelineStatus.IN_PROGRESS).count();
        long completed = allTasks.stream().filter(t -> t.getStatus() == TimelineStatus.COMPLETED || t.getStatus() == TimelineStatus.DONE).count();
        long overdue = repository.findOverdueTasks(eventId, LocalDateTime.now()).size();
        
        TimelineSummaryResponse.StatusBreakdown statusBreakdown = TimelineSummaryResponse.StatusBreakdown.builder()
            .total((int) total)
            .toDo((int) toDo)
            .active((int) active)
            .completed((int) completed)
            .overdue((int) overdue)
            .build();
        
        // Upcoming tasks
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(DEFAULT_UPCOMING_DAYS);
        List<TimelineItem> upcoming = repository.findUpcomingTasks(eventId, now, future);
        List<TimelineSummaryResponse.TaskSummary> upcomingTasks = upcoming.stream()
            .limit(10)
            .map(this::mapToTaskSummary)
            .collect(Collectors.toList());
        
        // Overdue tasks
        List<TimelineItem> overdueTasks = repository.findOverdueTasks(eventId, now);
        List<TimelineSummaryResponse.TaskSummary> overdueTaskSummaries = overdueTasks.stream()
            .map(this::mapToTaskSummary)
            .collect(Collectors.toList());
        
        // Tasks by assignee
        Map<UUID, TimelineSummaryResponse.AssigneeTaskSummary> tasksByAssignee = new HashMap<>();
        allTasks.stream()
            .filter(t -> t.getAssignedTo() != null)
            .collect(Collectors.groupingBy(TimelineItem::getAssignedTo))
            .forEach((assigneeId, assigneeTasks) -> {
                int totalTasks = assigneeTasks.size();
                int completedTasks = (int) assigneeTasks.stream()
                    .filter(t -> t.getStatus() == TimelineStatus.COMPLETED || t.getStatus() == TimelineStatus.DONE)
                    .count();
                int overdueCount = (int) assigneeTasks.stream()
                    .filter(t -> repository.findOverdueTasks(eventId, LocalDateTime.now()).contains(t))
                    .count();
                
                tasksByAssignee.put(assigneeId, TimelineSummaryResponse.AssigneeTaskSummary.builder()
                    .assigneeId(assigneeId)
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .overdueTasks(overdueCount)
                    .build());
            });
        
        // Timeline span
        LocalDateTime earliest = repository.getEarliestDate(eventId);
        LocalDateTime latest = repository.getLatestDate(eventId);
        Long totalDays = earliest != null && latest != null 
            ? ChronoUnit.DAYS.between(earliest, latest) 
            : null;
        
        TimelineSummaryResponse.TimelineSpan timelineSpan = TimelineSummaryResponse.TimelineSpan.builder()
            .earliestDate(earliest)
            .latestDate(latest)
            .totalDays(totalDays)
            .build();
        
        // Get recent activity from centralized audit log
        List<TimelineSummaryResponse.RecentActivity> recentActivity = 
            timelineHistoryService.getRecentActivity(eventId, 10);
        
        return TimelineSummaryResponse.builder()
            .eventId(eventId)
            .overallProgress(overallProgress)
            .statusBreakdown(statusBreakdown)
            .upcomingTasks(upcomingTasks)
            .overdueTasks(overdueTaskSummaries)
            .tasksByAssignee(tasksByAssignee)
            .timelineSpan(timelineSpan)
            .recentActivity(recentActivity)
            .build();
    }
    
    /**
     * Get upcoming tasks
     */
    public List<TaskResponse> getUpcomingTasks(UUID eventId, Integer days, UserPrincipal user) {
        validateEventAccess(user, eventId);
        
        int daysToCheck = days != null ? days : DEFAULT_UPCOMING_DAYS;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(daysToCheck);
        
        List<TimelineItem> upcoming = repository.findUpcomingTasks(eventId, now, future);
        return upcoming.stream()
            .map(t -> mapToTaskResponse(t, null))
            .collect(Collectors.toList());
    }
    
    /**
     * Get overdue tasks
     */
    public List<TaskResponse> getOverdueTasks(UUID eventId, UserPrincipal user) {
        validateEventAccess(user, eventId);
        
        List<TimelineItem> overdue = repository.findOverdueTasks(eventId, LocalDateTime.now());
        return overdue.stream()
            .map(t -> mapToTaskResponse(t, null))
            .collect(Collectors.toList());
    }
    
    /**
     * Search tasks
     */
    public List<TaskResponse> searchTasks(UUID eventId, String query, UserPrincipal user) {
        validateEventAccess(user, eventId);
        
        if (query == null || query.trim().isEmpty()) {
            return getTasks(eventId, null, null, null, true, user);
        }
        
        List<TimelineItem> results = repository.searchByEventIdAndQuery(eventId, query.trim());
        return results.stream()
            .map(t -> mapToTaskResponse(t, null))
            .collect(Collectors.toList());
    }
    
    /**
     * Bulk update tasks
     */
    public List<TaskResponse> bulkUpdateTasks(UUID eventId, BulkUpdateTaskRequest request, UserPrincipal user) {
        validateEventAccess(user, eventId);
        
        if (request.getUpdates().size() > MAX_BULK_OPERATIONS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Maximum " + MAX_BULK_OPERATIONS + " updates allowed per request");
        }
        
        List<TaskResponse> results = new ArrayList<>();
        
        for (BulkUpdateTaskRequest.TaskUpdate update : request.getUpdates()) {
            TimelineItem task = getTaskById(update.getTaskId(), eventId);
            
            if (update.getStartDate() != null) {
                task.setStartDate(update.getStartDate());
                task.setScheduledAt(update.getStartDate());
            }
            if (update.getEndDate() != null) {
                task.setEndTime(update.getEndDate());
                task.setDueDate(update.getEndDate());
            }
            if (update.getDurationMinutes() != null) {
                task.setDurationMinutes(update.getDurationMinutes());
                if (task.getStartDate() != null) {
                    task.setEndTime(task.getStartDate().plusMinutes(update.getDurationMinutes()));
                }
            }
            if (update.getStatus() != null) {
                try {
                    task.setStatus(TimelineStatus.valueOf(update.getStatus()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status: {}", update.getStatus());
                }
            }
            
            task = repository.save(task);
            results.add(mapToTaskResponse(task, null));
        }
        
        return results;
    }
    
    /**
     * Get task entity for upload permission check (internal use)
     */
    public TimelineItem getTaskEntityForUpload(UUID taskId, UserPrincipal user) {
        TimelineItem task = repository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        
        boolean isOwner = isEventOwner(user, task.getEventId());
        
        // Only assigned user or owner can upload proof
        if (!isOwner && (task.getAssignedTo() == null || !task.getAssignedTo().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You can only upload proof images for tasks assigned to you");
        }
        
        return task;
    }
    
    /**
     * Delete task
     */
    public void deleteTask(UUID taskId, UserPrincipal user) {
        TimelineItem task = repository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        
        validateEventAccess(user, task.getEventId());
        
        UUID parentTaskId = task.getParentTaskId();
        
        // Delete subtasks first
        List<TimelineItem> subtasks = getSubtasks(taskId);
        repository.deleteAll(subtasks);
        
        // Delete task
        repository.delete(task);
        
        // Recalculate parent progress if needed
        if (parentTaskId != null) {
            recalculateParentProgress(parentTaskId);
        }
        
        log.info("Deleted task {} by user {}", taskId, user.getId());
    }
    
    // ========== Helper Methods ==========
    
    private void validateDependencies(UUID eventId, List<UUID> dependencies) {
        Set<UUID> seen = new HashSet<>();
        for (UUID depId : dependencies) {
            if (depId == null) continue;
            if (!seen.add(depId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate dependency: " + depId);
            }
            TimelineItem dep = repository.findById(depId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Dependency not found: " + depId));
            if (!dep.getEventId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Dependency must belong to the same event");
            }
        }
    }
    
    private void validateParentTask(UUID parentId, UUID eventId, UUID currentTaskId) {
        if (parentId.equals(currentTaskId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Task cannot be its own parent");
        }
        TimelineItem parent = repository.findById(parentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Parent task not found: " + parentId));
        if (!parent.getEventId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Parent task must belong to the same event");
        }
    }
    
    private void recalculateParentProgress(UUID parentTaskId) {
        TimelineItem parent = repository.findById(parentTaskId).orElse(null);
        if (parent == null) return;
        
        List<TimelineItem> subtasks = getSubtasks(parentTaskId);
        if (subtasks.isEmpty()) {
            parent.setProgressPercentage(0);
            parent.setCompletedSubtasksCount(0);
            parent.setTotalSubtasksCount(0);
        } else {
            int total = subtasks.size();
            int completed = (int) subtasks.stream()
                .filter(t -> t.getStatus() == TimelineStatus.COMPLETED || t.getStatus() == TimelineStatus.DONE)
                .count();
            
            int progress = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;
            
            parent.setTotalSubtasksCount(total);
            parent.setCompletedSubtasksCount(completed);
            parent.setProgressPercentage(progress);
            
            // Update parent status based on progress
            if (progress >= 100) {
                parent.setStatus(TimelineStatus.COMPLETED);
            } else if (progress > 0) {
                parent.setStatus(TimelineStatus.ACTIVE);
            }
        }
        
        repository.save(parent);
    }
    
    private List<TimelineItem> getSubtasks(UUID parentTaskId) {
        return repository.findByParentTaskIdOrderByTaskOrderAsc(parentTaskId);
    }
    
    /**
     * Upload proof image for a task
     */
    public TaskResponse uploadProofImage(UUID taskId, UploadProofImageRequest request, UserPrincipal user) {
        TimelineItem task = repository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        
        boolean isOwner = isEventOwner(user, task.getEventId());
        
        // Only assigned user or owner can upload proof
        if (!isOwner && (task.getAssignedTo() == null || !task.getAssignedTo().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You can only upload proof images for tasks assigned to you");
        }
        
        // Set single proof image URL
        task.setProofImageUrl(request.getImageUrl());
        
        // If there are existing proof images, add to the list
        if (task.getProofImageUrls() != null && !task.getProofImageUrls().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<String> existing = mapper.readValue(task.getProofImageUrls(), 
                    new TypeReference<List<String>>() {});
                if (!existing.contains(request.getImageUrl())) {
                    existing.add(request.getImageUrl());
                    task.setProofImageUrls(mapper.writeValueAsString(existing));
                }
            } catch (Exception e) {
                log.warn("Error parsing existing proof images, creating new list", e);
                task.setProofImageUrls("[\"" + request.getImageUrl() + "\"]");
            }
        } else {
            // First proof image
            task.setProofImageUrls("[\"" + request.getImageUrl() + "\"]");
        }
        
        task = repository.save(task);
        return mapToTaskResponse(task, null);
    }
    
    private TaskResponse mapToTaskResponse(TimelineItem entity, List<TaskResponse> subtasks) {
        List<UUID> dependencies = entity.getDependencies() != null 
            ? Arrays.asList(entity.getDependencies()) 
            : Collections.emptyList();
        
        // Parse proof image URLs
        List<String> proofImageUrls = null;
        if (entity.getProofImageUrls() != null && !entity.getProofImageUrls().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                proofImageUrls = mapper.readValue(entity.getProofImageUrls(), 
                    new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Error parsing proof image URLs", e);
            }
        }
        
        return TaskResponse.builder()
            .id(entity.getId())
            .eventId(entity.getEventId())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .startDate(entity.getStartDate())
            .dueDate(entity.getDueDate())
            .scheduledAt(entity.getScheduledAt())
            .endTime(entity.getEndTime())
            .durationMinutes(entity.getDurationMinutes())
            .priority(entity.getPriority())
            .category(entity.getCategory())
            .status(entity.getStatus())
            .progressPercentage(entity.getProgressPercentage())
            .completedSubtasksCount(entity.getCompletedSubtasksCount())
            .totalSubtasksCount(entity.getTotalSubtasksCount())
            .assignedTo(entity.getAssignedTo())
            .parentTaskId(entity.getParentTaskId())
            .taskOrder(entity.getTaskOrder())
            .isParentTask(entity.getIsParentTask())
            .isPreview(entity.getIsPreview())
            .dependencies(dependencies)
            .subtasks(subtasks)
            .paymentDate(entity.getPaymentDate())
            .proofImageUrl(entity.getProofImageUrl())
            .proofImageUrls(proofImageUrls)
            .build();
    }
    
    private TimelineViewResponse.TimelineBar buildTimelineBar(
            TimelineItem task, 
            Map<UUID, List<TimelineItem>> subtaskMap,
            boolean includeSubtasks) {
        
        List<TimelineViewResponse.TimelineBar> subtaskBars = null;
        if (includeSubtasks && subtaskMap.containsKey(task.getId())) {
            subtaskBars = subtaskMap.get(task.getId()).stream()
                .sorted(Comparator.comparing(TimelineItem::getTaskOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(subtask -> buildTimelineBar(subtask, Collections.emptyMap(), false))
                .collect(Collectors.toList());
        }
        
        return TimelineViewResponse.TimelineBar.builder()
            .taskId(task.getId())
            .title(task.getTitle())
            .startDate(task.getStartDate())
            .endDate(task.getEndTime() != null ? task.getEndTime() : task.getDueDate())
            .durationMinutes(task.getDurationMinutes())
            .status(task.getStatus() != null ? task.getStatus().name() : "PENDING")
            .priority(task.getPriority())
            .category(task.getCategory())
            .progressPercentage(task.getProgressPercentage())
            .assignedTo(task.getAssignedTo())
            .parentTaskId(task.getParentTaskId())
            .isParentTask(task.getIsParentTask())
            .isPreview(task.getIsPreview())
            .subtasks(subtaskBars)
            .build();
    }
    
    private TimelineViewResponse.StatusSummary calculateStatusSummary(List<TimelineItem> tasks) {
        int all = tasks.size();
        int toDo = (int) tasks.stream()
            .filter(t -> t.getStatus() == TimelineStatus.TO_DO || t.getStatus() == TimelineStatus.PENDING)
            .count();
        int active = (int) tasks.stream()
            .filter(t -> t.getStatus() == TimelineStatus.ACTIVE || t.getStatus() == TimelineStatus.IN_PROGRESS)
            .count();
        int done = (int) tasks.stream()
            .filter(t -> t.getStatus() == TimelineStatus.COMPLETED || t.getStatus() == TimelineStatus.DONE)
            .count();
        int overdue = (int) tasks.stream()
            .filter(t -> repository.findOverdueTasks(t.getEventId(), LocalDateTime.now()).contains(t))
            .count();
        
        return TimelineViewResponse.StatusSummary.builder()
            .all(all)
            .toDo(toDo)
            .active(active)
            .done(done)
            .overdue(overdue)
            .build();
    }
    
    private Map<UUID, Integer> calculateTasksByAssignee(List<TimelineItem> tasks) {
        return tasks.stream()
            .filter(t -> t.getAssignedTo() != null)
            .collect(Collectors.groupingBy(
                TimelineItem::getAssignedTo,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }
    
    private Map<String, Integer> calculateTasksByCategory(List<TimelineItem> tasks) {
        return tasks.stream()
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.groupingBy(
                TimelineItem::getCategory,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }
    
    private TimelineSummaryResponse.TaskSummary mapToTaskSummary(TimelineItem task) {
        return TimelineSummaryResponse.TaskSummary.builder()
            .taskId(task.getId())
            .title(task.getTitle())
            .status(task.getStatus() != null ? task.getStatus().name() : "PENDING")
            .priority(task.getPriority())
            .dueDate(task.getDueDate())
            .assignedTo(task.getAssignedTo())
            .build();
    }
}

