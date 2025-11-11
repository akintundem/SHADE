package eventplanner.features.timeline.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TimelineDependencyBatchRequest {

    @NotEmpty(message = "At least one dependency update is required")
    @Valid
    private List<DependencyUpdate> updates;

    @Data
    public static class DependencyUpdate {

        @NotNull(message = "Item ID is required")
        private UUID itemId;

        private List<UUID> dependencies;
    }
}

