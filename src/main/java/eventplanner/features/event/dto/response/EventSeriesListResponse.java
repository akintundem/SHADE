package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated event series list.
 */
@Schema(description = "Paginated list of event series")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeriesListResponse {

    @Schema(description = "List of event series")
    private List<EventSeriesResponse> content;

    @Schema(description = "Current page number (0-indexed)")
    private int page;

    @Schema(description = "Page size")
    private int size;

    @Schema(description = "Total number of series")
    private long totalElements;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Whether this is the first page")
    private boolean first;

    @Schema(description = "Whether this is the last page")
    private boolean last;

    @Schema(description = "Whether there are more pages")
    private boolean hasNext;

    @Schema(description = "Whether there are previous pages")
    private boolean hasPrevious;
}
