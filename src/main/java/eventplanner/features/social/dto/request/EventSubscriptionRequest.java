package eventplanner.features.social.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to subscribe to an event")
public class EventSubscriptionRequest {

    @Schema(description = "Subscription type: FOLLOW, NOTIFY, or BOTH", example = "BOTH")
    private String subscriptionType = "BOTH";
}
