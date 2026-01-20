package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for occurrence generation results.
 */
@Schema(description = "Results of occurrence generation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateOccurrencesResponse {

    @Schema(description = "Series ID")
    private UUID seriesId;

    @Schema(description = "Number of occurrences generated")
    private int generatedCount;

    @Schema(description = "Number of occurrences skipped (conflicts, etc.)")
    private int skippedCount;

    @Schema(description = "Total occurrences in series after generation")
    private int totalOccurrences;

    @Schema(description = "IDs of generated events")
    private List<UUID> generatedEventIds;

    @Schema(description = "Dates of generated events")
    private List<LocalDateTime> generatedDates;

    @Schema(description = "Skipped dates with reasons")
    private List<SkippedOccurrence> skippedOccurrences;

    @Schema(description = "Whether more occurrences can be generated")
    private boolean canGenerateMore;

    @Schema(description = "Next available occurrence date")
    private LocalDateTime nextAvailableDate;

    @Schema(description = "Message about the generation result")
    private String message;

    /**
     * Represents a skipped occurrence.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkippedOccurrence {
        @Schema(description = "Date that was skipped")
        private LocalDateTime date;

        @Schema(description = "Reason for skipping")
        private String reason;
    }
}
