package eventplanner.features.ticket.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import eventplanner.features.event.entity.Event;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.money.Monetary;
import java.time.LocalDateTime;

/**
 * Ticket type entity defining ticket categories for an event.
 * Supports both paid and free tickets (price is nullable for free tickets).
 * Includes quantity management with reserved tickets for pending payments.
 */
@Entity
@Table(name = "ticket_types", indexes = {
    @Index(name = "idx_ticket_types_event_id", columnList = "event_id"),
    @Index(name = "idx_ticket_types_event_active", columnList = "event_id, is_active"),
    @Index(name = "idx_ticket_types_sale_dates", columnList = "sale_start_date, sale_end_date"),
    @Index(name = "idx_ticket_types_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketType extends BaseEntity {

    /**
     * Many-to-one relationship with the event this ticket type belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank(message = "Ticket type name is required")
    @Size(max = 100, message = "Ticket type name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Category of the ticket type (VIP, General Admission, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private TicketTypeCategory category;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Price per ticket. NULL indicates a free ticket.
     */
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    @Column(name = "price_minor")
    private Long priceMinor;

    /**
     * ISO 4217 currency code (e.g., "USD", "EUR", "GBP").
     * Stored as String for database compatibility, validated using JavaMoney (JSR 354).
     * This ensures compatibility with payment gateways like Stripe and PayPal.
     */
    @Size(max = 3, message = "Currency code must be 3 characters")
    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Min(value = 0, message = "Quantity available must be greater than or equal to 0")
    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @Min(value = 0, message = "Quantity sold must be greater than or equal to 0")
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold = 0;

    /**
     * Quantity reserved for pending payments (for paid tickets).
     * These tickets are not available for purchase but not yet sold.
     */
    @Min(value = 0, message = "Quantity reserved must be greater than or equal to 0")
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sale_start_date")
    private LocalDateTime saleStartDate;

    @Column(name = "sale_end_date")
    private LocalDateTime saleEndDate;

    /**
     * Maximum number of tickets a single person can purchase.
     * NULL means unlimited.
     */
    @Min(value = 1, message = "Max tickets per person must be at least 1")
    @Column(name = "max_tickets_per_person")
    private Integer maxTicketsPerPerson;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @Min(value = 0, message = "Early bird price must be greater than or equal to 0")
    @Column(name = "early_bird_price_minor")
    private Long earlyBirdPriceMinor;

    @Column(name = "early_bird_end_date")
    private LocalDateTime earlyBirdEndDate;

    @Min(value = 1, message = "Group discount minimum quantity must be at least 1")
    @Column(name = "group_discount_min_qty")
    private Integer groupDiscountMinQuantity;

    @Min(value = 1, message = "Group discount percent must be at least 1")
    @Column(name = "group_discount_percent_bps")
    private Integer groupDiscountPercentBps;

    /**
     * Metadata stored as JSON string for extensibility.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    @PreUpdate
    public void validate() {
        // Validate quantity constraints
        if (quantitySold == null) {
            quantitySold = 0;
        }
        if (quantityReserved == null) {
            quantityReserved = 0;
        }
        if (quantityAvailable == null) {
            quantityAvailable = 0;
        }
        
        // Validate that sold + reserved doesn't exceed available
        if (quantitySold + quantityReserved > quantityAvailable) {
            throw new IllegalArgumentException(
                "Quantity sold (" + quantitySold + ") + quantity reserved (" + quantityReserved + 
                ") cannot exceed quantity available (" + quantityAvailable + ")");
        }
        
        // Validate sale dates
        if (saleStartDate != null && saleEndDate != null && saleEndDate.isBefore(saleStartDate)) {
            throw new IllegalArgumentException("Sale end date must be after sale start date");
        }
        
        // Validate price
        if (priceMinor != null && priceMinor < 0) {
            throw new IllegalArgumentException("Price must be greater than or equal to 0");
        }

        if (earlyBirdPriceMinor != null && earlyBirdPriceMinor < 0) {
            throw new IllegalArgumentException("Early bird price must be greater than or equal to 0");
        }
        if (earlyBirdPriceMinor != null && earlyBirdEndDate == null) {
            throw new IllegalArgumentException("Early bird end date is required when early bird price is set");
        }
        if (groupDiscountMinQuantity != null && groupDiscountMinQuantity < 1) {
            throw new IllegalArgumentException("Group discount minimum quantity must be at least 1");
        }
        if (groupDiscountPercentBps != null && (groupDiscountPercentBps < 1 || groupDiscountPercentBps > 10_000)) {
            throw new IllegalArgumentException("Group discount percent must be between 1 and 10000 basis points");
        }
        if (groupDiscountMinQuantity != null && (groupDiscountPercentBps == null || groupDiscountPercentBps <= 0)) {
            throw new IllegalArgumentException("Group discount percent is required when minimum quantity is set");
        }
        
        // Validate currency code using JavaMoney (JSR 354) - ISO 4217 standard
        if (currency != null && !currency.isEmpty()) {
            try {
                // Normalize to uppercase and validate using JavaMoney
                String normalized = currency.trim().toUpperCase();
                Monetary.getCurrency(normalized); // Validate currency code
                // Store normalized version
                this.currency = normalized;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid currency code: " + currency + ". Must be a valid ISO 4217 code (e.g., USD, EUR, GBP).", e);
            }
        }
    }

    /**
     * Check if tickets are currently available for purchase.
     */
    public boolean isAvailable() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        
        int remaining = quantityAvailable - quantitySold - quantityReserved;
        if (remaining <= 0) {
            return false;
        }
        
        // Check if sale period is active
        LocalDateTime now = LocalDateTime.now();
        if (saleStartDate != null && now.isBefore(saleStartDate)) {
            return false;
        }
        if (saleEndDate != null && now.isAfter(saleEndDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if a specific quantity can be purchased.
     */
    public boolean canPurchase(int quantity) {
        if (!isAvailable()) {
            return false;
        }
        
        int remaining = quantityAvailable - quantitySold - quantityReserved;
        return remaining >= quantity;
    }

    /**
     * Check if ticket type is currently on sale (within sale period).
     */
    public boolean isOnSale() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (saleStartDate != null && now.isBefore(saleStartDate)) {
            return false;
        }
        if (saleEndDate != null && now.isAfter(saleEndDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Get remaining quantity available for purchase.
     */
    public int getQuantityRemaining() {
        return Math.max(0, quantityAvailable - quantitySold - quantityReserved);
    }
}
