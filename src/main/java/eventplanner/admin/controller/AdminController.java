package eventplanner.admin.controller;

import eventplanner.admin.entity.AdminDashboard;
import eventplanner.admin.entity.PlatformPayment;
import eventplanner.admin.service.AdminService;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for platform management and analytics
 * Authorization handled by security filters
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    private final AdminService adminService;
    
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    /**
     * Get admin dashboard overview
     */
    @GetMapping("/dashboard")
    @RequiresPermission(RbacPermissions.ADMIN_DASHBOARD_READ)
    public ResponseEntity<AdminDashboard> getDashboard() {
        AdminDashboard dashboard = adminService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * Get all events with pagination
     */
    @GetMapping("/events")
    @RequiresPermission(RbacPermissions.ADMIN_EVENTS_READ)
    public ResponseEntity<Page<Event>> getAllEvents(Pageable pageable) {
        Page<Event> events = adminService.getAllEvents(pageable);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get events by status
     */
    @GetMapping("/events/status/{status}")
    @RequiresPermission(value = RbacPermissions.ADMIN_EVENTS_FILTER, resources = {"status=#status"})
    public ResponseEntity<Page<Event>> getEventsByStatus(
            @PathVariable String status, 
            Pageable pageable) {
        Page<Event> events = adminService.getEventsByStatus(status, pageable);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get events created by a specific user
     */
    @GetMapping("/events/user/{userId}")
    @RequiresPermission(value = RbacPermissions.ADMIN_EVENTS_USER, resources = {"user_id=#userId"})
    public ResponseEntity<Page<Event>> getEventsByUser(
            @PathVariable UUID userId, 
            Pageable pageable) {
        Page<Event> events = adminService.getEventsByUser(userId, pageable);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get all users with pagination
     */
    @GetMapping("/users")
    @RequiresPermission(RbacPermissions.ADMIN_USERS_READ)
    public ResponseEntity<Page<UserAccount>> getAllUsers(Pageable pageable) {
        Page<UserAccount> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user details by ID
     */
    @GetMapping("/users/{userId}")
    @RequiresPermission(value = RbacPermissions.ADMIN_USERS_DETAIL, resources = {"user_id=#userId"})
    public ResponseEntity<UserAccount> getUserById(@PathVariable UUID userId) {
        UserAccount user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get all platform payments with pagination
     */
    @GetMapping("/payments")
    @RequiresPermission(RbacPermissions.ADMIN_PAYMENTS_READ)
    public ResponseEntity<Page<PlatformPayment>> getAllPayments(Pageable pageable) {
        Page<PlatformPayment> payments = adminService.getAllPayments(pageable);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get payments by status
     */
    @GetMapping("/payments/status/{status}")
    @RequiresPermission(value = RbacPermissions.ADMIN_PAYMENTS_FILTER, resources = {"status=#status"})
    public ResponseEntity<Page<PlatformPayment>> getPaymentsByStatus(
            @PathVariable String status, 
            Pageable pageable) {
        Page<PlatformPayment> payments = adminService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get payments for a specific event
     */
    @GetMapping("/payments/event/{eventId}")
    @RequiresPermission(value = RbacPermissions.ADMIN_PAYMENTS_EVENT, resources = {"event_id=#eventId"})
    public ResponseEntity<List<PlatformPayment>> getPaymentsForEvent(@PathVariable UUID eventId) {
        List<PlatformPayment> payments = adminService.getPaymentsForEvent(eventId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get revenue analytics
     */
    @GetMapping("/analytics/revenue")
    @RequiresPermission(RbacPermissions.ADMIN_ANALYTICS_REVENUE)
    public ResponseEntity<Map<String, Object>> getRevenueAnalytics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        Map<String, Object> analytics = adminService.getRevenueAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get event analytics
     */
    @GetMapping("/analytics/events")
    @RequiresPermission(RbacPermissions.ADMIN_ANALYTICS_EVENTS)
    public ResponseEntity<Map<String, Object>> getEventAnalytics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        Map<String, Object> analytics = adminService.getEventAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get user analytics
     */
    @GetMapping("/analytics/users")
    @RequiresPermission(RbacPermissions.ADMIN_ANALYTICS_USERS)
    public ResponseEntity<Map<String, Object>> getUserAnalytics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        Map<String, Object> analytics = adminService.getUserAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get platform metrics
     */
    @GetMapping("/metrics")
    @RequiresPermission(RbacPermissions.ADMIN_METRICS_READ)
    public ResponseEntity<Map<String, Object>> getPlatformMetrics() {
        Map<String, Object> metrics = adminService.getPlatformMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Update user status (suspend/activate)
     */
    @PutMapping("/users/{userId}/status")
    @RequiresPermission(value = RbacPermissions.ADMIN_USERS_UPDATE, resources = {"user_id=#userId"})
    public ResponseEntity<UserAccount> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam String status) {
        UserAccount user = adminService.updateUserStatus(userId, status);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Process refund for a payment
     */
    @PostMapping("/payments/{paymentId}/refund")
    @RequiresPermission(value = RbacPermissions.ADMIN_PAYMENTS_REFUND, resources = {"payment_id=#paymentId"})
    public ResponseEntity<PlatformPayment> processRefund(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam String reason) {
        PlatformPayment payment = adminService.processRefund(paymentId, amount, reason);
        return ResponseEntity.ok(payment);
    }
}
