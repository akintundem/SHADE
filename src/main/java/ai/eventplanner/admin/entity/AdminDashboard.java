package ai.eventplanner.admin.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin dashboard metrics and analytics
 */
@Entity
@Table(name = "admin_dashboard")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminDashboard extends BaseEntity {
    
    @Column(name = "date", nullable = false)
    private LocalDateTime date;
    
    // Event Metrics
    @Column(name = "total_events")
    private Long totalEvents;
    
    @Column(name = "active_events")
    private Long activeEvents;
    
    @Column(name = "completed_events")
    private Long completedEvents;
    
    @Column(name = "cancelled_events")
    private Long cancelledEvents;
    
    // User Metrics
    @Column(name = "total_users")
    private Long totalUsers;
    
    @Column(name = "new_users_today")
    private Long newUsersToday;
    
    @Column(name = "active_users_today")
    private Long activeUsersToday;
    
    // Revenue Metrics
    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;
    
    @Column(name = "revenue_today", precision = 12, scale = 2)
    private BigDecimal revenueToday;
    
    @Column(name = "revenue_this_month", precision = 12, scale = 2)
    private BigDecimal revenueThisMonth;
    
    @Column(name = "average_revenue_per_event", precision = 10, scale = 2)
    private BigDecimal averageRevenuePerEvent;
    
    // Payment Metrics
    @Column(name = "total_payments")
    private Long totalPayments;
    
    @Column(name = "successful_payments")
    private Long successfulPayments;
    
    @Column(name = "failed_payments")
    private Long failedPayments;
    
    @Column(name = "refunded_payments")
    private Long refundedPayments;
    
    // Platform Metrics
    @Column(name = "total_events_created")
    private Long totalEventsCreated;
    
    @Column(name = "events_created_today")
    private Long eventsCreatedToday;
    
    @Column(name = "average_events_per_user", precision = 5, scale = 2)
    private BigDecimal averageEventsPerUser;
    
    @Column(name = "platform_uptime_percentage", precision = 5, scale = 2)
    private BigDecimal platformUptimePercentage;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
