package ai.eventplanner.admin.repository;

import ai.eventplanner.admin.entity.PlatformPayment;
import ai.eventplanner.common.domain.enums.PlatformPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for platform payments
 */
@Repository
public interface PlatformPaymentRepository extends JpaRepository<PlatformPayment, UUID> {
    
    /**
     * Find payments by status
     */
    Page<PlatformPayment> findByStatus(PlatformPaymentStatus status, Pageable pageable);
    
    /**
     * Find payments for a specific event
     */
    List<PlatformPayment> findByEventId(UUID eventId);
    
    /**
     * Find payments by user
     */
    Page<PlatformPayment> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Get total revenue
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PlatformPayment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();
    
    /**
     * Get monthly revenue
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PlatformPayment p WHERE p.status = 'COMPLETED' AND p.createdAt >= :startDate")
    BigDecimal getMonthlyRevenue(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get total revenue by date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PlatformPayment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get revenue by payment type
     */
    @Query("SELECT p.paymentType, COALESCE(SUM(p.amount), 0) FROM PlatformPayment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate GROUP BY p.paymentType")
    Map<String, BigDecimal> getRevenueByPaymentType(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get daily revenue trend
     */
    @Query("SELECT DATE(p.createdAt) as date, COALESCE(SUM(p.amount), 0) as revenue FROM PlatformPayment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(p.createdAt) ORDER BY DATE(p.createdAt)")
    List<Map<String, Object>> getDailyRevenueTrend(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count payments by date range
     */
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count payments by status and date range
     */
    Long countByStatusAndDateRange(PlatformPaymentStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count payments by status
     */
    Long countByStatus(PlatformPaymentStatus status);
}
