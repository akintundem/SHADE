package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.entity.TicketPriceTier;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketPriceTierResponse {

    private UUID id;
    private String name;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Long priceMinor;
    private Integer priority;

    public static TicketPriceTierResponse from(TicketPriceTier tier) {
        TicketPriceTierResponse response = new TicketPriceTierResponse();
        response.setId(tier.getId());
        response.setName(tier.getName());
        response.setStartsAt(tier.getStartsAt());
        response.setEndsAt(tier.getEndsAt());
        response.setPriceMinor(tier.getPriceMinor());
        response.setPriority(tier.getPriority());
        return response;
    }
}
