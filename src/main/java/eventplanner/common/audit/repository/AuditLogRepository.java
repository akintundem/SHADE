package eventplanner.common.audit.repository;

import eventplanner.common.domain.entity.AuditLog;
import eventplanner.common.domain.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entities
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    /**
     * Find all audit logs for a specific entity
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, UUID entityId);
    
    /**
     * Find all audit logs for a specific user
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);
    
    /**
     * Find all audit logs for a specific event (budget-related)
     */
    Page<AuditLog> findByEventIdOrderByTimestampDesc(UUID eventId, Pageable pageable);
    
    /**
     * Find all audit logs for a specific action type
     */
    List<AuditLog> findByActionTypeOrderByTimestampDesc(ActionType actionType);
    
    /**
     * Find audit logs within a time range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find audit logs for a specific entity and action type
     */
    List<AuditLog> findByEntityTypeAndEntityIdAndActionTypeOrderByTimestampDesc(
        String entityType, UUID entityId, ActionType actionType);
    
    /**
     * Count audit logs by entity type
     */
    long countByEntityType(String entityType);
    
    /**
     * Count audit logs by user
     */
    long countByUserId(UUID userId);
}

