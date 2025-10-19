package ai.eventplanner.event.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class AiWorkflowRequest {

    @NotNull
    private UUID eventId;

    @NotBlank
    private String eventName;

    @NotBlank
    private String eventType;

    @NotBlank
    private String date;

    @NotBlank
    private String location;

    @NotNull
    @Positive
    private Integer guestCount;

    @NotNull
    @Positive
    private BigDecimal budget;

    private List<String> preferences;
}
