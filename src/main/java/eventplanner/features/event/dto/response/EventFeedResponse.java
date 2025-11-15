package eventplanner.features.event.dto.response;

import eventplanner.common.domain.enums.EventScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Event feed response for guest users")
@Getter
@Setter
public class EventFeedResponse {
    
    @Schema(description = "Event ID")
    private UUID eventId;
    
    @Schema(description = "Event name")
    private String eventName;
    
    @Schema(description = "Event description")
    private String description;
    
    @Schema(description = "Cover image URL")
    private String coverImageUrl;
    
    @Schema(description = "Event start date and time")
    private LocalDateTime startDateTime;
    
    @Schema(description = "Event end date and time")
    private LocalDateTime endDateTime;
    
    @Schema(description = "Event hashtag")
    private String hashtag;
    
    @Schema(description = "Event website URL")
    private String eventWebsiteUrl;
    
    @Schema(description = "List of feed posts (videos, pictures, tweets)")
    private List<FeedPost> posts;
    
    // Pagination metadata
    @Schema(description = "Current page number (0-indexed)")
    private Integer currentPage;
    
    @Schema(description = "Number of items per page")
    private Integer pageSize;
    
    @Schema(description = "Total number of posts available")
    private Long totalPosts;
    
    @Schema(description = "Total number of pages")
    private Integer totalPages;
    
    @Schema(description = "Whether there is a next page")
    private Boolean hasNext;
    
    @Schema(description = "Whether there is a previous page")
    private Boolean hasPrevious;

    @Schema(description = "Event scope: FULL (full details) or FEED (feed view)", example = "FEED")
    private EventScope scope = EventScope.FEED;
}

