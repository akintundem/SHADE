package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for user's events summary
 */
@Schema(description = "User events summary response")
@Getter
@Setter
public class UserEventsSummaryResponse {

    @Schema(description = "Events owned by the user")
    private List<UserEventRelationshipResponse> ownedEvents;

    @Schema(description = "Events the user is attending")
    private List<UserEventRelationshipResponse> attendingEvents;

    @Schema(description = "Events the user is organizing")
    private List<UserEventRelationshipResponse> organizingEvents;

    @Schema(description = "Events the user is coordinating")
    private List<UserEventRelationshipResponse> coordinatingEvents;

    @Schema(description = "Events the user is volunteering for")
    private List<UserEventRelationshipResponse> volunteeringEvents;

    @Schema(description = "Events the user is speaking at")
    private List<UserEventRelationshipResponse> speakingEvents;

    @Schema(description = "Events the user is sponsoring")
    private List<UserEventRelationshipResponse> sponsoringEvents;

    @Schema(description = "Upcoming events")
    private List<UserEventRelationshipResponse> upcomingEvents;

    @Schema(description = "Past events")
    private List<UserEventRelationshipResponse> pastEvents;

    @Schema(description = "Total count of all events")
    private Integer totalCount;

    @Schema(description = "Count of owned events")
    private Integer ownedCount;

    @Schema(description = "Count of attending events")
    private Integer attendingCount;

    @Schema(description = "Count of organizing events")
    private Integer organizingCount;

    @Schema(description = "Count of coordinating events")
    private Integer coordinatingCount;

    @Schema(description = "Count of volunteering events")
    private Integer volunteeringCount;

    @Schema(description = "Count of speaking events")
    private Integer speakingCount;

    @Schema(description = "Count of sponsoring events")
    private Integer sponsoringCount;
}
