package eventplanner.features.timeline.service;

import eventplanner.common.audit.service.AuditLogService;
import eventplanner.common.domain.entity.AuditLog;
import eventplanner.common.domain.enums.ActionType;
import eventplanner.common.domain.enums.TimelineChangeType;
import eventplanner.features.timeline.dto.response.TimelineSummaryResponse;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for recording and retrieving timeline change history.
 * Uses the centralized AuditLogService instead of a separate TimelineChangeLog entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineHistoryService {

    private static final DateTimeFormatter SUMMARY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d h:mm a");
    
    private static final String TIMELINE_DOMAIN = "TIMELINE";
    private static final String TIMELINE_ITEM_ENTITY_TYPE = "TimelineItem";

    private final AuditLogService auditLogService;

    /**
     * Record a timeline change using the centralized audit log system.
     */
    @Transactional
    public void recordChange(UUID eventId,
                             UUID timelineItemId,
                             TimelineChangeType changeType,
                             String summary,
                             String details,
                             UserPrincipal actor,
                             Map<String, Object> metadata) {

        ActionType actionType = mapChangeTypeToActionType(changeType);
        
        String description = summary;
        if (details != null && !details.isEmpty()) {
            description = summary != null ? summary + ": " + details : details;
        }

        auditLogService.builder()
                .domain(TIMELINE_DOMAIN)
                .entityType(timelineItemId != null ? TIMELINE_ITEM_ENTITY_TYPE : null)
                .entityId(timelineItemId)
                .action(actionType)
                .status("SUCCESS")
                .user(actor != null ? actor.getId() : null,
                      actor != null ? actor.getName() : null,
                      actor != null ? actor.getUsername() : null)
                .description(description)
                .eventId(eventId)
                .metadata(metadata)
                .log();
    }

    /**
     * Get recent timeline activity for an event from the centralized audit log.
     */
    @Transactional(readOnly = true)
    public List<TimelineSummaryResponse.RecentActivity> getRecentActivity(UUID eventId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 20));
        List<AuditLog> entries = auditLogService.getEventAuditLogs(eventId, pageable)
                .getContent()
                .stream()
                .filter(log -> TIMELINE_DOMAIN.equals(log.getDomain()))
                .limit(limit)
                .collect(Collectors.toList());
        
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.stream()
                .map(entry -> {
                    // Extract task title from description or use a default
                    String taskTitle = extractTaskTitle(entry.getDescription());
                    
                    return TimelineSummaryResponse.RecentActivity.builder()
                            .taskId(entry.getEntityId())
                            .taskTitle(taskTitle)
                            .action(formatAction(entry))
                            .timestamp(entry.getTimestamp())
                            .userId(entry.getUserId())
                            .userName(entry.getUsername())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Map TimelineChangeType to ActionType for the centralized audit log.
     */
    private ActionType mapChangeTypeToActionType(TimelineChangeType changeType) {
        if (changeType == null) {
            return ActionType.UPDATE;
        }
        
        return switch (changeType) {
            case CREATED -> ActionType.CREATE;
            case UPDATED -> ActionType.UPDATE;
            case DELETED -> ActionType.DELETE;
            case REORDERED -> ActionType.UPDATE; // Reordering is a type of update
            case DEPENDENCIES_UPDATED -> ActionType.UPDATE; // Dependency updates are updates
            case PUBLISHED -> ActionType.PUBLISH;
            case UNPUBLISHED -> ActionType.UPDATE; // Unpublishing is treated as an update
        };
    }

    /**
     * Extract task title from audit log description.
     * Tries to extract meaningful title from the description field.
     */
    private String extractTaskTitle(String description) {
        if (description == null || description.isEmpty()) {
            return "Timeline activity";
        }
        
        // If description contains a colon, take the part before it as the title
        int colonIndex = description.indexOf(':');
        if (colonIndex > 0) {
            return description.substring(0, colonIndex).trim();
        }
        
        // Otherwise, use the first part of the description (up to 50 chars)
        return description.length() > 50 
                ? description.substring(0, 50) + "..." 
                : description;
    }

    /**
     * Format action string from audit log entry.
     */
    private String formatAction(AuditLog entry) {
        if (entry.getActionType() == null || entry.getTimestamp() == null) {
            return entry.getActionType() != null ? entry.getActionType().name() : "UPDATED";
        }

        String formattedTime = entry.getTimestamp().format(SUMMARY_TIMESTAMP_FORMAT);
        return "%s (%s)".formatted(entry.getActionType().name(), formattedTime);
    }
}

