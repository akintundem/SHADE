package eventplanner.features.ticket.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Shared promotion payload used by ticket type create/update requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetails {
    private String code;
    @Min(0)
    @Max(100_00)
    private Integer percentOffBasisPoints;
    @Min(0)
    private Long amountOffMinor;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean active;
}
