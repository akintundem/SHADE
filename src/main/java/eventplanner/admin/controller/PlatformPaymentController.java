package eventplanner.admin.controller;

import eventplanner.admin.entity.PlatformPayment;
import eventplanner.admin.service.PlatformPaymentService;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.features.event.entity.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<PlatformPayment> createEventCreationPayment(
            @RequestParam UUID eventId,
            @RequestParam BigDecimal amount,
            Authentication authentication) {
        
        // Get current user from authentication
        UserAccount user = (UserAccount) authentication.getPrincipal();
        
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
    public ResponseEntity<PlatformPayment> getPaymentById(@PathVariable UUID paymentId) {
        PlatformPayment payment = platformPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Get payments for current user
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<PlatformPayment>> getMyPayments(Authentication authentication) {
        UserAccount user = (UserAccount) authentication.getPrincipal();
        List<PlatformPayment> payments = platformPaymentService.getPaymentsForUser(user.getId());
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Get payments for event
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<PlatformPayment>> getPaymentsForEvent(@PathVariable UUID eventId) {
        List<PlatformPayment> payments = platformPaymentService.getPaymentsForEvent(eventId);
        return ResponseEntity.ok(payments);
    }
}
