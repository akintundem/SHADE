package eventplanner.features.timeline.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimelinePublishRequest {

    @NotNull(message = "Published flag is required")
    private Boolean published;

    private String message;
}

