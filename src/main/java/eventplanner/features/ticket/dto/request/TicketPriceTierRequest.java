package eventplanner.features.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketPriceTierRequest {

    @Size(max = 120, message = "Tier name must not exceed 120 characters")
    private String name;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    @Min(value = 0, message = "Tier price must be greater than or equal to 0")
    private Long priceMinor;

    private Integer priority = 0;
}
