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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketTypeTemplateRequest {

    @Size(min = 1, max = 100, message = "Template name must be between 1 and 100 characters")
    private String name;

    private TicketTypeCategory category;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private Long priceMinor;

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

    private Boolean requiresApproval;

    @Min(value = 0, message = "Early bird price must be greater than or equal to 0")
    private Long earlyBirdPriceMinor;

    private LocalDateTime earlyBirdEndDate;

    @Min(value = 1, message = "Group discount minimum quantity must be at least 1")
    private Integer groupDiscountMinQuantity;

    @Min(value = 1, message = "Group discount percent must be at least 1")
    private Integer groupDiscountPercentBps;

    private Map<String, Object> metadata;
}
