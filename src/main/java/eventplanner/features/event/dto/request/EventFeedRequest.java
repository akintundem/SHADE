package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Request parameters for event feed pagination")
@Getter
@Setter
public class EventFeedRequest {

    @Min(value = 0, message = "Page number must be >= 0")
    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 50, message = "Page size must be <= 50")
    @Schema(description = "Number of posts per page", example = "20", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "Filter by post type: VIDEO, IMAGE, TEXT, or ALL", example = "ALL")
    private String postType;
}

