package ai.eventplanner.timeline.dto.request;

import ai.eventplanner.timeline.dto.TimelineItemCreateRequest;
import lombok.Data;

import java.util.List;

@Data
public class TimelineBulkCreateRequest {
    private List<TimelineItemCreateRequest> items;
}
