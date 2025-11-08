package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.SecurityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {

    Page<SecurityAuditLog> findByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);

    Page<SecurityAuditLog> findByEmailOrderByOccurredAtDesc(String email, Pageable pageable);

    Page<SecurityAuditLog> findByIpAddressOrderByOccurredAtDesc(String ipAddress, Pageable pageable);

    Page<SecurityAuditLog> findByEventTypeOrderByOccurredAtDesc(
        SecurityAuditLog.SecurityEventType eventType, 
        Pageable pageable
    );

    Page<SecurityAuditLog> findByStatusOrderByOccurredAtDesc(
        SecurityAuditLog.SecurityEventStatus status, 
        Pageable pageable
    );

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.occurredAt >= :since ORDER BY s.occurredAt DESC")
    Page<SecurityAuditLog> findRecentEvents(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.userId = :userId AND s.eventType = :eventType AND s.occurredAt >= :since")
    long countByUserIdAndEventTypeSince(
        @Param("userId") UUID userId,
        @Param("eventType") SecurityAuditLog.SecurityEventType eventType,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.ipAddress = :ipAddress AND s.eventType = :eventType AND s.occurredAt >= :since")
    long countByIpAddressAndEventTypeSince(
        @Param("ipAddress") String ipAddress,
        @Param("eventType") SecurityAuditLog.SecurityEventType eventType,
        @Param("since") LocalDateTime since
    );
}

