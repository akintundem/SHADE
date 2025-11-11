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
import java.util.Map;
import java.util.UUID;

/**
 * Unified Audit Log Service - Centralized audit logging for ALL domains
 * 
 * Replaces: SecurityAuditService, AttendeeAuditService, and other domain-specific audit services
 * 
 * Features:
 * - Async, non-blocking audit logging
 * - Separate transactions to ensure logs are saved even if main transaction fails
 * - Support for all domains: Security, Budget, Attendee, Event, Vendor, etc.
 * - Rich context capture: user, IP, device, session, idempotency
 * - JSON serialization for old/new values and metadata
 * - Comprehensive query methods
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
    
    // ==================== BUILDER PATTERN FOR FLUENT API ====================
    
    /**
     * Create an audit log builder for fluent API
     */
    public AuditLogBuilder builder() {
        return new AuditLogBuilder(this);
    }
    
    /**
     * Fluent builder for audit logs
     */
    public static class AuditLogBuilder {
        private final AuditLogService service;
        private String domain;
        private String entityType;
        private UUID entityId;
        private ActionType actionType;
        private String status;
        private UUID userId;
        private String username;
        private String email;
        private String description;
        private Object oldValues;
        private Object newValues;
        private String ipAddress;
        private String userAgent;
        private String deviceId;
        private String riskLevel;
        private UUID sessionId;
        private String idempotencyKey;
        private Map<String, Object> metadata;
        private UUID eventId;
        
        private AuditLogBuilder(AuditLogService service) {
            this.service = service;
        }
        
        public AuditLogBuilder domain(String domain) {
            this.domain = domain;
            return this;
        }
        
        public AuditLogBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }
        
        public AuditLogBuilder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }
        
        public AuditLogBuilder action(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }
        
        public AuditLogBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public AuditLogBuilder user(UUID userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            return this;
        }
        
        public AuditLogBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public AuditLogBuilder changes(Object oldValues, Object newValues) {
            this.oldValues = oldValues;
            this.newValues = newValues;
            return this;
        }
        
        public AuditLogBuilder oldValue(Object oldValue) {
            this.oldValues = oldValue;
            return this;
        }
        
        public AuditLogBuilder newValue(Object newValue) {
            this.newValues = newValue;
            return this;
        }
        
        public AuditLogBuilder request(String ipAddress, String userAgent, String deviceId) {
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.deviceId = deviceId;
            return this;
        }
        
        public AuditLogBuilder security(String riskLevel, UUID sessionId) {
            this.riskLevel = riskLevel;
            this.sessionId = sessionId;
            return this;
        }
        
        public AuditLogBuilder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }
        
        public AuditLogBuilder idempotency(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }
        
        public AuditLogBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }
        
        public AuditLogBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public AuditLogBuilder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public void log() {
            service.logWithBuilder(this);
        }
    }
    
    /**
     * Internal method to handle builder-based logging
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void logWithBuilder(AuditLogBuilder builder) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .domain(builder.domain)
                    .entityType(builder.entityType)
                    .entityId(builder.entityId)
                    .actionType(builder.actionType)
                    .status(builder.status)
                    .userId(builder.userId)
                    .username(builder.username)
                    .email(builder.email)
                    .description(builder.description)
                    .oldValues(serializeValue(builder.oldValues))
                    .newValues(serializeValue(builder.newValues))
                    .ipAddress(builder.ipAddress)
                    .userAgent(builder.userAgent)
                    .deviceId(builder.deviceId)
                    .riskLevel(builder.riskLevel)
                    .sessionId(builder.sessionId)
                    .idempotencyKey(builder.idempotencyKey)
                    .metadata(serializeValue(builder.metadata))
                    .eventId(builder.eventId)
                    .build();
            
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created: {} {} in {} domain", 
                builder.actionType, builder.entityType, builder.domain);
        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }
    
    private String serializeValue(Object value) {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize value: {}", e.getMessage());
            return value.toString();
        }
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

