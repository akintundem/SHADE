package ai.eventplanner.admin.service;

import ai.eventplanner.admin.entity.AdminDashboard;
import ai.eventplanner.admin.entity.PlatformPayment;
import ai.eventplanner.admin.repository.AdminDashboardRepository;
import ai.eventplanner.admin.repository.PlatformPaymentRepository;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.repo.UserAccountRepository;
import ai.eventplanner.common.domain.enums.PlatformPaymentStatus;
import ai.eventplanner.common.domain.enums.UserStatus;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.event.repo.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin service for platform management and analytics
 */
@Service
@Transactional
public class AdminService {
    
    private final EventRepository eventRepository;
    private final UserAccountRepository userAccountRepository;
    private final PlatformPaymentRepository platformPaymentRepository;
    private final AdminDashboardRepository adminDashboardRepository;
    
    public AdminService(EventRepository eventRepository,
                        UserAccountRepository userAccountRepository,
                        PlatformPaymentRepository platformPaymentRepository,
                        AdminDashboardRepository adminDashboardRepository) {
        this.eventRepository = eventRepository;
        this.userAccountRepository = userAccountRepository;
        this.platformPaymentRepository = platformPaymentRepository;
        this.adminDashboardRepository = adminDashboardRepository;
    }
    
    /**
     * Get admin dashboard overview
     */
    public AdminDashboard getDashboard() {
        return adminDashboardRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(createDefaultDashboard());
    }
    
    /**
     * Get all events with pagination
     */
    public Page<Event> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }
    
    /**
     * Get events by status
     */
    public Page<Event> getEventsByStatus(String status, Pageable pageable) {
        return eventRepository.findByEventStatus(status, pageable);
    }
    
    /**
     * Get events created by a specific user
     */
    public Page<Event> getEventsByUser(UUID userId, Pageable pageable) {
        return eventRepository.findByOwnerId(userId, pageable);
    }
    
    /**
     * Get all users with pagination
     */
    public Page<UserAccount> getAllUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable);
    }
    
    /**
     * Get user details by ID
     */
    public UserAccount getUserById(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * Get all platform payments with pagination
     */
    public Page<PlatformPayment> getAllPayments(Pageable pageable) {
        return platformPaymentRepository.findAll(pageable);
    }
    
    /**
     * Get payments by status
     */
    public Page<PlatformPayment> getPaymentsByStatus(String status, Pageable pageable) {
        PlatformPaymentStatus paymentStatus = PlatformPaymentStatus.valueOf(status.toUpperCase());
        return platformPaymentRepository.findByStatus(paymentStatus, pageable);
    }
    
    /**
     * Get payments for a specific event
     */
    public List<PlatformPayment> getPaymentsForEvent(UUID eventId) {
        return platformPaymentRepository.findByEventId(eventId);
    }
    
    /**
     * Get revenue analytics
     */
    public Map<String, Object> getRevenueAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> analytics = new HashMap<>();
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        // Total revenue in period
        BigDecimal totalRevenue = platformPaymentRepository.getTotalRevenueByDateRange(startDate, endDate);
        analytics.put("totalRevenue", totalRevenue);
        
        // Revenue by payment type
        Map<String, BigDecimal> revenueByType = platformPaymentRepository.getRevenueByPaymentType(startDate, endDate);
        analytics.put("revenueByType", revenueByType);
        
        // Daily revenue trend
        List<Map<String, Object>> dailyRevenue = platformPaymentRepository.getDailyRevenueTrend(startDate, endDate);
        analytics.put("dailyRevenue", dailyRevenue);
        
        // Payment success rate
        Long totalPayments = platformPaymentRepository.countByDateRange(startDate, endDate);
        Long successfulPayments = platformPaymentRepository.countByStatusAndDateRange(PlatformPaymentStatus.COMPLETED, startDate, endDate);
        BigDecimal successRate = totalPayments > 0 ? 
                BigDecimal.valueOf(successfulPayments).divide(BigDecimal.valueOf(totalPayments), 4, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        analytics.put("successRate", successRate);
        
        return analytics;
    }
    
    /**
     * Get event analytics
     */
    public Map<String, Object> getEventAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> analytics = new HashMap<>();
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        // Total events created
        Long totalEvents = eventRepository.countByDateRange(startDate, endDate);
        analytics.put("totalEvents", totalEvents);
        
        // Events by status
        Map<String, Long> eventsByStatus = eventRepository.getEventsByStatus(startDate, endDate);
        analytics.put("eventsByStatus", eventsByStatus);
        
        // Events by month
        List<Map<String, Object>> eventsByMonth = eventRepository.getEventsByMonth(startDate, endDate);
        analytics.put("eventsByMonth", eventsByMonth);
        
        // Average events per user
        Long totalUsers = userAccountRepository.countByDateRange(startDate, endDate);
        BigDecimal avgEventsPerUser = totalUsers > 0 ? 
                BigDecimal.valueOf(totalEvents).divide(BigDecimal.valueOf(totalUsers), 2, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        analytics.put("avgEventsPerUser", avgEventsPerUser);
        
        return analytics;
    }
    
    /**
     * Get user analytics
     */
    public Map<String, Object> getUserAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> analytics = new HashMap<>();
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        // Total users
        Long totalUsers = userAccountRepository.countByDateRange(startDate, endDate);
        analytics.put("totalUsers", totalUsers);
        
        // New users
        Long newUsers = userAccountRepository.countByDateRange(startDate, endDate);
        analytics.put("newUsers", newUsers);
        
        // Users by status
        Map<String, Long> usersByStatus = userAccountRepository.getUsersByStatus();
        analytics.put("usersByStatus", usersByStatus);
        
        // User growth trend
        List<Map<String, Object>> userGrowth = userAccountRepository.getUserGrowthTrend(startDate, endDate);
        analytics.put("userGrowth", userGrowth);
        
        return analytics;
    }
    
    /**
     * Get platform metrics
     */
    public Map<String, Object> getPlatformMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Total counts
        metrics.put("totalEvents", eventRepository.count());
        metrics.put("totalUsers", userAccountRepository.count());
        metrics.put("totalPayments", platformPaymentRepository.count());
        
        // Active counts
        metrics.put("activeEvents", eventRepository.countByEventStatus("ACTIVE"));
        metrics.put("activeUsers", userAccountRepository.countByStatus(UserStatus.ACTIVE));
        metrics.put("completedPayments", platformPaymentRepository.countByStatus(PlatformPaymentStatus.COMPLETED));
        
        // Revenue metrics
        BigDecimal totalRevenue = platformPaymentRepository.getTotalRevenue();
        metrics.put("totalRevenue", totalRevenue);
        
        BigDecimal monthlyRevenue = platformPaymentRepository.getMonthlyRevenue(LocalDateTime.now().minusMonths(1));
        metrics.put("monthlyRevenue", monthlyRevenue);
        
        return metrics;
    }
    
    /**
     * Update user status
     */
    public UserAccount updateUserStatus(UUID userId, String status) {
        UserAccount user = getUserById(userId);
        user.setStatus(UserStatus.valueOf(status.toUpperCase()));
        return userAccountRepository.save(user);
    }
    
    /**
     * Process refund for a payment
     */
    public PlatformPayment processRefund(UUID paymentId, BigDecimal amount, String reason) {
        PlatformPayment payment = platformPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (amount == null) {
            amount = payment.getAmount();
        }
        
        payment.setRefundAmount(amount);
        payment.setRefundDate(LocalDateTime.now());
        payment.setRefundReason(reason);
        payment.setStatus(PlatformPaymentStatus.REFUNDED);
        
        return platformPaymentRepository.save(payment);
    }
    
    /**
     * Create default dashboard
     */
    private AdminDashboard createDefaultDashboard() {
        AdminDashboard dashboard = new AdminDashboard();
        dashboard.setDate(LocalDateTime.now());
        dashboard.setTotalEvents(eventRepository.count());
        dashboard.setTotalUsers(userAccountRepository.count());
        dashboard.setTotalRevenue(platformPaymentRepository.getTotalRevenue());
        return dashboard;
    }
}
