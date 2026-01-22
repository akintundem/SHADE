package eventplanner.features.currency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Currency reference table - normalized currency codes.
 * Replaces string currency fields with foreign keys for validation.
 */
@Entity
@Table(name = "currencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    /**
     * ISO 4217 currency code (e.g., USD, EUR, GBP, NGN)
     */
    @Id
    @Column(name = "code", length = 3)
    private String code;

    /**
     * Currency full name (e.g., "US Dollar", "Euro")
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Currency symbol (e.g., "$", "€", "£", "₦")
     */
    @Column(name = "symbol", length = 10)
    private String symbol;

    /**
     * Number of decimal places (2 for most currencies, 0 for JPY)
     */
    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces = 2;

    /**
     * Whether this currency is currently active/supported
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
