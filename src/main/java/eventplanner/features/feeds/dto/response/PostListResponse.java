package eventplanner.features.feeds.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Paginated list of feed posts")
public class PostListResponse {
    
    @Schema(description = "List of posts")
    private List<FeedPostResponse> posts;
    
    @Schema(description = "Current page number (0-indexed)")
    private Integer currentPage;
    
    @Schema(description = "Page size")
    private Integer pageSize;
    
    @Schema(description = "Total number of posts")
    private Long totalPosts;
    
    @Schema(description = "Total number of pages")
    private Integer totalPages;
    
    @Schema(description = "Whether there is a next page")
    private Boolean hasNext;
    
    @Schema(description = "Whether there is a previous page")
    private Boolean hasPrevious;
}
