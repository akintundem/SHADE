package ai.eventplanner.admin.controller;

import ai.eventplanner.admin.entity.AdminDashboard;
import ai.eventplanner.admin.entity.PlatformPayment;
import ai.eventplanner.admin.service.AdminService;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.auth.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for platform management and analytics
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    /**
     * Get admin dashboard overview
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboard> getDashboard() {
        AdminDashboard dashboard = adminService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * Get all events with pagination
     */
    @GetMapping("/events")
    public ResponseEntity<Page<Event>> getAllEvents(Pageable pageable) {
        Page<Event> events = adminService.getAllEvents(pageable);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get events by status
     */
    @GetMapping("/events/status/{status}")
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
    public ResponseEntity<Page<UserAccount>> getAllUsers(Pageable pageable) {
        Page<UserAccount> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user details by ID
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserAccount> getUserById(@PathVariable UUID userId) {
        UserAccount user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get all platform payments with pagination
     */
    @GetMapping("/payments")
    public ResponseEntity<Page<PlatformPayment>> getAllPayments(Pageable pageable) {
        Page<PlatformPayment> payments = adminService.getAllPayments(pageable);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get payments by status
     */
    @GetMapping("/payments/status/{status}")
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
    public ResponseEntity<List<PlatformPayment>> getPaymentsForEvent(@PathVariable UUID eventId) {
        List<PlatformPayment> payments = adminService.getPaymentsForEvent(eventId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get revenue analytics
     */
    @GetMapping("/analytics/revenue")
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
    public ResponseEntity<Map<String, Object>> getPlatformMetrics() {
        Map<String, Object> metrics = adminService.getPlatformMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Update user status (suspend/activate)
     */
    @PutMapping("/users/{userId}/status")
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
    public ResponseEntity<PlatformPayment> processRefund(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam String reason) {
        PlatformPayment payment = adminService.processRefund(paymentId, amount, reason);
        return ResponseEntity.ok(payment);
    }
}
