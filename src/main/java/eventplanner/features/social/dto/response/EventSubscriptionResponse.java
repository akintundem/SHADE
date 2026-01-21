package eventplanner.features.social.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Event subscription response")
public class EventSubscriptionResponse {
    private UUID id;
    private UUID userId;
    private UUID eventId;

    @Schema(description = "Subscription type: FOLLOW, NOTIFY, or BOTH")
    private String subscriptionType;

    private LocalDateTime createdAt;
}
