package eventplanner.common.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.audit.repository.AuditLogRepository;
import eventplanner.common.domain.entity.AuditLog;
import eventplanner.common.domain.enums.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for creating and managing audit logs
 * Operations are async and use separate transactions to avoid impacting main business logic
 */
@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create an audit log entry asynchronously
     * Uses a separate transaction to ensure audit logs are saved even if main transaction fails
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created: {} {} on {} {}", 
                auditLog.getActionType(), 
                auditLog.getEntityType(), 
                auditLog.getEntityId(),
                auditLog.getUserId() != null ? "by user " + auditLog.getUserId() : "");
        } catch (Exception e) {
            // Log but don't throw - audit failures shouldn't break business logic
            logger.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a simple audit log entry
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, ActionType actionType, 
                    UUID userId, String username, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActionType(actionType);
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setDescription(description);
        
        logAction(auditLog);
    }
    
    /**
     * Create an audit log with old and new values
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWithChanges(String entityType, UUID entityId, ActionType actionType,
                               UUID userId, String username, String description,
                               Object oldValue, Object newValue, UUID eventId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActionType(actionType);
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setDescription(description);
        auditLog.setEventId(eventId);
        
        // Serialize old and new values to JSON
        try {
            if (oldValue != null) {
                auditLog.setOldValues(objectMapper.writeValueAsString(oldValue));
            }
            if (newValue != null) {
                auditLog.setNewValues(objectMapper.writeValueAsString(newValue));
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit values: {}", e.getMessage());
            auditLog.setOldValues(oldValue != null ? oldValue.toString() : null);
            auditLog.setNewValues(newValue != null ? newValue.toString() : null);
        }
        
        logAction(auditLog);
    }
    
    /**
     * Get audit logs for a specific entity
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityAuditLogs(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }
    
    /**
     * Get audit logs for a specific user
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserAuditLogs(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }
    
    /**
     * Get audit logs for a specific event
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getEventAuditLogs(UUID eventId, Pageable pageable) {
        return auditLogRepository.findByEventIdOrderByTimestampDesc(eventId, pageable);
    }
    
    /**
     * Get audit logs within a time range
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime);
    }
    
    /**
     * Get audit logs for a specific action type
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(ActionType actionType) {
        return auditLogRepository.findByActionTypeOrderByTimestampDesc(actionType);
    }
    
    /**
     * Get audit statistics
     */
    @Transactional(readOnly = true)
    public AuditStatistics getStatistics(String entityType) {
        long totalCount = auditLogRepository.countByEntityType(entityType);
        return new AuditStatistics(entityType, totalCount);
    }
    
    /**
     * Simple statistics class
     */
    public static class AuditStatistics {
        private final String entityType;
        private final long totalCount;
        
        public AuditStatistics(String entityType, long totalCount) {
            this.entityType = entityType;
            this.totalCount = totalCount;
        }
        
        public String getEntityType() {
            return entityType;
        }
        
        public long getTotalCount() {
            return totalCount;
        }
    }
}

