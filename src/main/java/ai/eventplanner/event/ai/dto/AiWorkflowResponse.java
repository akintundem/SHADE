package ai.eventplanner.event.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class AiWorkflowResponse {
    UUID eventId;
    String status;
    OffsetDateTime startedAt;
    OffsetDateTime completedAt;
    List<String> completedSteps;
    Map<String, Object> generatedArtifacts;
}
