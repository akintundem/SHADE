package eventplanner.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

/**
 * Pagination utilities to normalize and validate pagination parameters.
 * Provides consistent pagination behavior across the application.
 */
public class PaginationUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /**
     * Normalize pagination parameters with default values and limits.
     * Page defaults to 0, size defaults to 20, max size is 100.
     *
     * @param page Page number (0-indexed), defaults to 0
     * @param size Page size, defaults to 20, max 100
     * @return Normalized array [page, size]
     */
    public static int[] normalize(Integer page, Integer size) {
        return normalize(page, size, MAX_SIZE);
    }

    /**
     * Normalize pagination parameters with custom max size.
     *
     * @param page Page number (0-indexed), defaults to 0
     * @param size Page size, defaults to 20
     * @param maxSize Maximum page size
     * @return Normalized array [page, size]
     */
    public static int[] normalize(Integer page, Integer size, int maxSize) {
        int normalizedPage = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int normalizedSize = size != null && size > 0 ? Math.min(size, maxSize) : DEFAULT_SIZE;
        return new int[]{normalizedPage, normalizedSize};
    }

    /**
     * Create Pageable with normalized parameters.
     * Uses default page=0, size=20, max=100.
     *
     * @param page Page number
     * @param size Page size
     * @return Pageable object
     */
    public static Pageable createPageable(Integer page, Integer size) {
        int[] normalized = normalize(page, size);
        return PageRequest.of(normalized[0], normalized[1]);
    }

    /**
     * Create Pageable with normalized parameters and custom max size.
     *
     * @param page Page number
     * @param size Page size
     * @param maxSize Maximum page size
     * @return Pageable object
     */
    public static Pageable createPageable(Integer page, Integer size, int maxSize) {
        int[] normalized = normalize(page, size, maxSize);
        return PageRequest.of(normalized[0], normalized[1]);
    }

    /**
     * Create Pageable with normalized parameters and sort.
     *
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field (defaults to defaultSort if null/blank)
     * @param sortDirection Sort direction (ASC/DESC, defaults to ASC)
     * @param defaultSort Default sort field if sortBy is null/blank
     * @return Pageable object with sorting
     */
    public static Pageable createPageable(Integer page, Integer size, String sortBy,
                                          String sortDirection, String defaultSort) {
        int[] normalized = normalize(page, size);

        String sortField = StringUtils.hasText(sortBy) ? sortBy.trim() : defaultSort;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(normalized[0], normalized[1], sort);
    }

    /**
     * Create Pageable with normalized parameters, sort, and custom max size.
     *
     * @param page Page number
     * @param size Page size
     * @param maxSize Maximum page size
     * @param sortBy Sort field (defaults to defaultSort if null/blank)
     * @param sortDirection Sort direction (ASC/DESC, defaults to ASC)
     * @param defaultSort Default sort field if sortBy is null/blank
     * @return Pageable object with sorting
     */
    public static Pageable createPageable(Integer page, Integer size, int maxSize,
                                          String sortBy, String sortDirection, String defaultSort) {
        int[] normalized = normalize(page, size, maxSize);

        String sortField = StringUtils.hasText(sortBy) ? sortBy.trim() : defaultSort;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(normalized[0], normalized[1], sort);
    }

    /**
     * Create Pageable with multiple sort fields.
     *
     * @param page Page number
     * @param size Page size
     * @param sort Sort object (can have multiple fields)
     * @return Pageable object with sorting
     */
    public static Pageable createPageable(Integer page, Integer size, Sort sort) {
        int[] normalized = normalize(page, size);
        return PageRequest.of(normalized[0], normalized[1], sort);
    }

    /**
     * Normalize page number to ensure it's non-negative.
     *
     * @param page Page number
     * @return Normalized page (0 if null or negative)
     */
    public static int normalizePage(Integer page) {
        return page != null && page >= 0 ? page : DEFAULT_PAGE;
    }

    /**
     * Normalize page size to ensure it's within valid range.
     *
     * @param size Page size
     * @return Normalized size (defaults to 20, max 100)
     */
    public static int normalizeSize(Integer size) {
        return normalizeSize(size, MAX_SIZE);
    }

    /**
     * Normalize page size with custom max.
     *
     * @param size Page size
     * @param maxSize Maximum page size
     * @return Normalized size
     */
    public static int normalizeSize(Integer size, int maxSize) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, maxSize);
    }

    /**
     * Validate that page and size are within acceptable ranges.
     *
     * @param page Page number
     * @param size Page size
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static void validate(Integer page, Integer size) {
        if (page != null && page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        if (size != null && size > MAX_SIZE) {
            throw new IllegalArgumentException("Page size cannot exceed " + MAX_SIZE);
        }
    }
}
