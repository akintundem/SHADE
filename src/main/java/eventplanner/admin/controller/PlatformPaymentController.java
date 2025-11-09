package eventplanner.admin.controller;

import eventplanner.admin.entity.PlatformPayment;
import eventplanner.admin.service.PlatformPaymentService;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller for platform payment operations
 */
@RestController
@RequestMapping("/api/platform-payments")
public class PlatformPaymentController {
    
    private final PlatformPaymentService platformPaymentService;
    
    public PlatformPaymentController(PlatformPaymentService platformPaymentService) {
        this.platformPaymentService = platformPaymentService;
    }
    
    /**
     * Create payment for event creation
     */
    @PostMapping("/event-creation")
    @RequiresPermission(RbacPermissions.PAYMENT_CREATE)
    public ResponseEntity<PlatformPayment> createEventCreationPayment(
            @RequestParam UUID eventId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UserAccount user = requireUser(principal);
        
        // Get event (you'll need to inject EventRepository or EventService)
        // For now, we'll create a mock event
        Event event = new Event();
        event.setId(eventId);
        
        PlatformPayment payment = platformPaymentService.createEventCreationPayment(user, event, amount);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Process successful payment
     */
    @PostMapping("/{paymentId}/success")
    @RequiresPermission(value = RbacPermissions.PAYMENT_PROCESS, resources = {"payment_id=#paymentId"})
    public ResponseEntity<PlatformPayment> processSuccessfulPayment(
            @PathVariable UUID paymentId,
            @RequestParam String transactionId,
            @RequestParam String paymentMethod) {
        
        PlatformPayment payment = platformPaymentService.processSuccessfulPayment(paymentId, transactionId, paymentMethod);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Process failed payment
     */
    @PostMapping("/{paymentId}/fail")
    @RequiresPermission(value = RbacPermissions.PAYMENT_PROCESS, resources = {"payment_id=#paymentId"})
    public ResponseEntity<PlatformPayment> processFailedPayment(
            @PathVariable UUID paymentId,
            @RequestParam String reason) {
        
        PlatformPayment payment = platformPaymentService.processFailedPayment(paymentId, reason);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Process refund
     */
    @PostMapping("/{paymentId}/refund")
    @RequiresPermission(value = RbacPermissions.PAYMENT_REFUND, resources = {"payment_id=#paymentId"})
    public ResponseEntity<PlatformPayment> processRefund(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) BigDecimal refundAmount,
            @RequestParam String reason) {
        
        PlatformPayment payment = platformPaymentService.processRefund(paymentId, refundAmount, reason);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    @RequiresPermission(value = RbacPermissions.PAYMENT_READ, resources = {"payment_id=#paymentId"})
    public ResponseEntity<PlatformPayment> getPaymentById(@PathVariable UUID paymentId) {
        PlatformPayment payment = platformPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Get payments for current user
     */
    @GetMapping("/my-payments")
    @RequiresPermission(value = RbacPermissions.PAYMENT_READ, resources = {"user_id=#principal.id"})
    public ResponseEntity<List<PlatformPayment>> getMyPayments(@AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = requireUser(principal).getId();
        List<PlatformPayment> payments = platformPaymentService.getPaymentsForUser(userId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get payments for event
     */
    @GetMapping("/event/{eventId}")
    @RequiresPermission(value = RbacPermissions.ADMIN_PAYMENTS_EVENT, resources = {"event_id=#eventId"})
    public ResponseEntity<List<PlatformPayment>> getPaymentsForEvent(@PathVariable UUID eventId) {
        List<PlatformPayment> payments = platformPaymentService.getPaymentsForEvent(eventId);
        return ResponseEntity.ok(payments);
    }

    private UserAccount requireUser(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal.getUser();
    }
}
