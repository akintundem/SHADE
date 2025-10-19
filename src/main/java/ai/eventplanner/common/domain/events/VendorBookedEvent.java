package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vendor booked event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorBookedEvent {
    private UUID eventId;
    private UUID organizationId;
    private UUID eventVendorId;
    private String serviceCategory;
    private String serviceDescription;
    private BigDecimal quoteAmount;
    private BigDecimal finalAmount;
    private LocalDateTime serviceDate;
    private LocalDateTime bookingConfirmedDate;
}
