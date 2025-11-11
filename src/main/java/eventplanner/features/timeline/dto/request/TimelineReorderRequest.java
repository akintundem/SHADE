package eventplanner.features.timeline.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TimelineReorderRequest {

    @NotEmpty(message = "At least one item is required for reorder")
    @Valid
    private List<ItemOrderUpdate> items;

    @Data
    public static class ItemOrderUpdate {
        @NotNull(message = "Item ID is required")
        private UUID itemId;

        private Integer taskOrder;

        private LocalDateTime scheduledAt;
    }
}

