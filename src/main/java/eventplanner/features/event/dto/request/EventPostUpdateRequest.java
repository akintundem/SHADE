package eventplanner.features.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Update an event post")
public class EventPostUpdateRequest {

    @Schema(description = "Updated content/caption")
    private String content;

    @Schema(description = "Optional media object id update")
    private UUID mediaObjectId;
}


