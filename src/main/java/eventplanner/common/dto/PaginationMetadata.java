package eventplanner.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata for list responses.
 * Provides comprehensive pagination information to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationMetadata {

    /**
     * Current page number (0-indexed)
     */
    private int page;

    /**
     * Number of items per page
     */
    private int size;

    /**
     * Total number of elements across all pages
     */
    private long totalElements;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Whether this is the first page
     */
    private boolean first;

    /**
     * Whether this is the last page
     */
    private boolean last;

    /**
     * Whether there is a next page
     */
    private boolean hasNext;

    /**
     * Whether there is a previous page
     */
    private boolean hasPrevious;

    /**
     * Number of elements in current page
     */
    private int numberOfElements;

    /**
     * Create pagination metadata from Spring Data Page
     */
    public static PaginationMetadata from(Page<?> page) {
        return PaginationMetadata.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .numberOfElements(page.getNumberOfElements())
            .build();
    }

    /**
     * Create pagination metadata manually
     */
    public static PaginationMetadata of(int page, int size, long totalElements, int totalPages) {
        boolean isFirst = page == 0;
        boolean isLast = page >= totalPages - 1;

        return PaginationMetadata.builder()
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(isFirst)
            .last(isLast)
            .hasNext(!isLast)
            .hasPrevious(!isFirst)
            .build();
    }
}
