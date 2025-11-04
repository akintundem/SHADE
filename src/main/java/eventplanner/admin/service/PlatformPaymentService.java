package eventplanner.admin.service;

import eventplanner.admin.entity.PlatformPayment;
import eventplanner.admin.repository.PlatformPaymentRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.common.domain.enums.PlatformPaymentStatus;
import eventplanner.common.domain.enums.PlatformPaymentType;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling platform payments
 */
@Service
@Transactional
public class PlatformPaymentService {
    
    private final PlatformPaymentRepository platformPaymentRepository;
    private final EventRepository eventRepository;
    
    public PlatformPaymentService(PlatformPaymentRepository platformPaymentRepository,
                                 EventRepository eventRepository) {
        this.platformPaymentRepository = platformPaymentRepository;
        this.eventRepository = eventRepository;
    }
    
    /**
     * Create a payment for event creation
     */
    public PlatformPayment createEventCreationPayment(UserAccount user, Event event, BigDecimal amount) {
        PlatformPayment payment = new PlatformPayment();
        payment.setUser(user);
        payment.setEvent(event);
        payment.setPaymentType(PlatformPaymentType.EVENT_CREATION_FEE);
        payment.setAmount(amount);
        payment.setStatus(PlatformPaymentStatus.PENDING);
        payment.setDescription("Event creation fee for: " + event.getName());
        
        PlatformPayment savedPayment = platformPaymentRepository.save(payment);
        
        // Link payment to event
        event.setPlatformPaymentId(savedPayment.getId());
        event.setCreationFeeAmount(amount);
        eventRepository.save(event);
        
        return savedPayment;
    }
    
    /**
     * Process successful payment
     */
    public PlatformPayment processSuccessfulPayment(UUID paymentId, String transactionId, String paymentMethod) {
        PlatformPayment payment = platformPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(PlatformPaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(LocalDateTime.now());
        
        PlatformPayment savedPayment = platformPaymentRepository.save(payment);
        
        // Update event payment status
        if (payment.getEvent() != null) {
            Event event = payment.getEvent();
            event.setCreationFeePaid(true);
            event.setPaymentDate(LocalDateTime.now());
            eventRepository.save(event);
        }
        
        return savedPayment;
    }
    
    /**
     * Process failed payment
     */
    public PlatformPayment processFailedPayment(UUID paymentId, String reason) {
        PlatformPayment payment = platformPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(PlatformPaymentStatus.FAILED);
        payment.setDescription(payment.getDescription() + " - Failed: " + reason);
        
        return platformPaymentRepository.save(payment);
    }
    
    /**
     * Process refund
     */
    public PlatformPayment processRefund(UUID paymentId, BigDecimal refundAmount, String reason) {
        PlatformPayment payment = platformPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(PlatformPaymentStatus.REFUNDED);
        payment.setRefundAmount(refundAmount);
        payment.setRefundDate(LocalDateTime.now());
        payment.setRefundReason(reason);
        
        PlatformPayment savedPayment = platformPaymentRepository.save(payment);
        
        // Update event payment status
        if (payment.getEvent() != null) {
            Event event = payment.getEvent();
            event.setCreationFeePaid(false);
            eventRepository.save(event);
        }
        
        return savedPayment;
    }
    
    /**
     * Get payment by ID
     */
    public PlatformPayment getPaymentById(UUID paymentId) {
        return platformPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
    
    /**
     * Get payments for user
     */
    public java.util.List<PlatformPayment> getPaymentsForUser(UUID userId) {
        return platformPaymentRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
    
    /**
     * Get payments for event
     */
    public java.util.List<PlatformPayment> getPaymentsForEvent(UUID eventId) {
        return platformPaymentRepository.findByEventId(eventId);
    }
}
