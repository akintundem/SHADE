package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.ticket.entity.TicketTypeTemplate;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketTypeTemplateResponse {

    private UUID id;
    private String name;
    private TicketTypeCategory category;
    private String description;
    private Long priceMinor;
    private String currency;
    private Integer quantityAvailable;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private Integer maxTicketsPerPerson;
    private Boolean requiresApproval;
    private Long earlyBirdPriceMinor;
    private LocalDateTime earlyBirdEndDate;
    private Integer groupDiscountMinQuantity;
    private Integer groupDiscountPercentBps;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketTypeTemplateResponse from(TicketTypeTemplate template) {
        return TicketTypeTemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .category(template.getCategory())
            .description(template.getDescription())
            .priceMinor(template.getPriceMinor())
            .currency(template.getCurrency())
            .quantityAvailable(template.getQuantityAvailable())
            .saleStartDate(template.getSaleStartDate())
            .saleEndDate(template.getSaleEndDate())
            .maxTicketsPerPerson(template.getMaxTicketsPerPerson())
            .requiresApproval(template.getRequiresApproval())
            .earlyBirdPriceMinor(template.getEarlyBirdPriceMinor())
            .earlyBirdEndDate(template.getEarlyBirdEndDate())
            .groupDiscountMinQuantity(template.getGroupDiscountMinQuantity())
            .groupDiscountPercentBps(template.getGroupDiscountPercentBps())
            .createdBy(template.getCreatedBy() != null ? template.getCreatedBy().getId() : null)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
