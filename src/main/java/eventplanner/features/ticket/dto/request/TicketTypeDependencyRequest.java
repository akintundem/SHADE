package eventplanner.features.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeDependencyRequest {

    @NotNull(message = "Required ticket type ID is required")
    private UUID requiredTicketTypeId;

    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity = 1;
}
