package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for flyer generation
 */
@Schema(description = "Flyer generation response")
public class FlyerResponse {

    @Schema(description = "Unique identifier of the event", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID eventId;

    @Schema(description = "Status of the flyer generation", example = "completed")
    private String status;

    @Schema(description = "Status message", example = "Flyer generated successfully!")
    private String message;

    @Schema(description = "URL to the generated flyer image", example = "https://api.eventplanner.ai/flyers/123e4567-e89b-12d3-a456-426614174000/generated-flyer.png")
    private String flyerUrl;

    @Schema(description = "URL to the flyer thumbnail", example = "https://api.eventplanner.ai/flyers/123e4567-e89b-12d3-a456-426614174000/thumbnail.png")
    private String thumbnailUrl;

    @Schema(description = "URL to the print-ready PDF", example = "https://api.eventplanner.ai/flyers/123e4567-e89b-12d3-a456-426614174000/print-ready.pdf")
    private String printUrl;

    @Schema(description = "Description of the generated design", example = "Created a professional flyer for Annual Company Conference. The design features a professional style with blue, white and silver color scheme.")
    private String designDescription;

    @Schema(description = "Design style used", example = "professional")
    private String designStyle;

    @Schema(description = "Color scheme used", example = "blue, white, silver")
    private String colorScheme;

    @Schema(description = "File size of the generated flyer in bytes", example = "2048576")
    private Long fileSize;

    @Schema(description = "Dimensions of the generated flyer", example = "1920x1080")
    private String dimensions;

    @Schema(description = "When the flyer was generated", example = "2024-01-15T10:30:00")
    private LocalDateTime generatedAt;

    @Schema(description = "Processing time in milliseconds", example = "2500")
    private Long processingTimeMs;

    @Schema(description = "Additional metadata about the generation process")
    private String metadata;

    // Constructors
    public FlyerResponse() {}

    public FlyerResponse(UUID eventId, String status, String message) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.generatedAt = LocalDateTime.now();
    }

    public FlyerResponse(UUID eventId, String status, String message, String flyerUrl, String thumbnailUrl, String printUrl) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.flyerUrl = flyerUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.printUrl = printUrl;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFlyerUrl() {
        return flyerUrl;
    }

    public void setFlyerUrl(String flyerUrl) {
        this.flyerUrl = flyerUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPrintUrl() {
        return printUrl;
    }

    public void setPrintUrl(String printUrl) {
        this.printUrl = printUrl;
    }

    public String getDesignDescription() {
        return designDescription;
    }

    public void setDesignDescription(String designDescription) {
        this.designDescription = designDescription;
    }

    public String getDesignStyle() {
        return designStyle;
    }

    public void setDesignStyle(String designStyle) {
        this.designStyle = designStyle;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
