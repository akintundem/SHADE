package eventplanner.features.ticket.dto.request;

import eventplanner.features.ticket.enums.TicketTypeCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for updating an existing ticket type.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketTypeRequest {

    @Size(min = 1, max = 100, message = "Ticket type name must be between 1 and 100 characters")
    private String name;

    /**
     * Category of the ticket type (VIP, General Admission, etc.).
     */
    private TicketTypeCategory category;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /**
     * Price per ticket in the smallest currency unit (e.g., cents).
     * NULL indicates a free ticket.
     */
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private Long priceMinor;

    /**
     * ISO 4217 currency code (e.g., "USD", "EUR", "GBP").
     * Must be a valid currency code supported by payment gateways.
     * Validated using JavaMoney (JSR 354) for ISO 4217 compliance.
     */
    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    @Min(value = 0, message = "Quantity available must be greater than or equal to 0")
    @Max(value = 1000000, message = "Quantity available must not exceed 1,000,000")
    private Integer quantityAvailable;

    private LocalDateTime saleStartDate;

    private LocalDateTime saleEndDate;

    @Min(value = 1, message = "Max tickets per person must be at least 1")
    @Max(value = 100, message = "Max tickets per person must not exceed 100")
    private Integer maxTicketsPerPerson;

    private Boolean isActive;

    private Boolean requiresApproval;

    /**
     * Metadata as a map (will be serialized to JSON string).
     */
    private Map<String, Object> metadata;
}
