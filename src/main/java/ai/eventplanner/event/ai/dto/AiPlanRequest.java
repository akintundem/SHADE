package ai.eventplanner.event.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class AiPlanRequest {

    @NotBlank(message = "Event name is required")
    private String eventName;

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Event date is required")
    private String date;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Guest count is required")
    @Positive(message = "Guest count must be positive")
    private Integer guestCount;

    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private BigDecimal budget;

    private List<String> preferences;
}
