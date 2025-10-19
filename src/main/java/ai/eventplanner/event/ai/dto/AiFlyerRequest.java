package ai.eventplanner.event.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiFlyerRequest {

    @NotBlank
    private String eventName;

    @NotBlank
    private String eventType;

    @NotBlank
    private String theme;

    @NotBlank
    private String date;

    @NotBlank
    private String time;

    @NotBlank
    private String location;

    @NotBlank
    private String description;

    @NotBlank
    private String colorScheme;

    @NotBlank
    private String style;
}
