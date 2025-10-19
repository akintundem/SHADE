package ai.eventplanner.event.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiEventRequest {

    @NotBlank(message = "Event name is required")
    @Pattern(regexp = "^[^<>']{3,120}$", message = "Event name contains invalid characters")
    private String name;

    @NotBlank(message = "Event type is required")
    private String type;

    @NotBlank(message = "Event date is required")
    private String date;

    @NotBlank(message = "Location is required")
    private String location;

    private String organizerId;

    @NotNull(message = "Guest count is required")
    @Positive(message = "Guest count must be positive")
    private Integer guestCount;

    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private BigDecimal budget;

    private String description;

    private List<String> preferences;

    private String notes;
}
