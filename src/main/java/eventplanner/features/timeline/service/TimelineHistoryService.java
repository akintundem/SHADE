package eventplanner.features.timeline.service;

import eventplanner.common.domain.enums.TimelineChangeType;
import eventplanner.features.timeline.dto.response.TimelineSummaryResponse;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for recording and retrieving timeline change history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineHistoryService {

    /**
     * Record a timeline change.
     */
    @Transactional
    public void recordChange(UUID eventId,
                             UUID timelineItemId,
                             TimelineChangeType changeType,
                             String summary,
                             String details,
                             UserPrincipal actor,
                             Map<String, Object> metadata) {
        // Timeline change recording has been removed
        log.debug("Timeline change recorded for event: {}, item: {}", eventId, timelineItemId);
    }

    /**
     * Get recent timeline activity for an event.
     */
    @Transactional(readOnly = true)
    public List<TimelineSummaryResponse.RecentActivity> getRecentActivity(UUID eventId, int limit) {
        // Timeline activity retrieval has been removed
        return Collections.emptyList();
    }
}

