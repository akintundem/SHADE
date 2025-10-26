package ai.eventplanner.timeline.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class TimelineSummaryResponse {
    private UUID eventId;
    private int totalItems;
    private int completedItems;
    private int pendingItems;
    private int overdueItems;
    private double completionRate;
}
