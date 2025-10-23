package ai.eventplanner.admin.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.PlatformPaymentStatus;
import ai.eventplanner.common.domain.enums.PlatformPaymentType;
import ai.eventplanner.event.entity.Event;
import ai.eventplanner.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Platform payments for event creation and premium features
 */
@Entity
@Table(name = "platform_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlatformPayment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PlatformPaymentType paymentType;
    
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlatformPaymentStatus status = PlatformPaymentStatus.PENDING;
    
    @Column(name = "payment_method")
    private String paymentMethod; // stripe, paypal, etc.
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "payment_gateway_reference")
    private String paymentGatewayReference;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(name = "refund_date")
    private LocalDateTime refundDate;
    
    @Column(name = "refund_reason")
    private String refundReason;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public PlatformPayment(UserAccount user, Event event, PlatformPaymentType paymentType, BigDecimal amount) {
        this.user = user;
        this.event = event;
        this.paymentType = paymentType;
        this.amount = amount;
    }
}
