package ai.eventplanner.event.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for generating event flyers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate an event flyer")
public class FlyerRequest {

    @NotBlank(message = "Event name is required")
    @Size(min = 2, max = 100, message = "Event name must be between 2 and 100 characters")
    @Schema(description = "Name of the event", example = "Annual Company Conference", required = true)
    private String eventName;

    @NotBlank(message = "Event type is required")
    @Pattern(regexp = "^(CORPORATE_EVENT|WEDDING|CONFERENCE|BIRTHDAY_PARTY|ANNIVERSARY|GRADUATION|RETIREMENT|HOLIDAY_PARTY|FUNDRAISER|PRODUCT_LAUNCH|NETWORKING_EVENT|TEAM_BUILDING|AWARDS_CEREMONY|GALA|SEMINAR|WORKSHOP|TRADE_SHOW|EXHIBITION|CONCERT|FESTIVAL|SPORTS_EVENT|CHARITY_EVENT|MILESTONE_CELEBRATION|CUSTOM)$", 
             message = "Invalid event type")
    @Schema(description = "Type of the event", example = "CONFERENCE", required = true)
    private String eventType;

    @Size(max = 100, message = "Theme cannot exceed 100 characters")
    @Schema(description = "Event theme", example = "Innovation and Technology")
    private String theme;

    @NotBlank(message = "Event date is required")
    @Size(min = 1, max = 50, message = "Date must be between 1 and 50 characters")
    @Schema(description = "Event date", example = "June 15, 2024", required = true)
    private String date;

    @Size(max = 50, message = "Time cannot exceed 50 characters")
    @Schema(description = "Event time", example = "9:00 AM - 5:00 PM")
    private String time;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    @Schema(description = "Event location", example = "San Francisco Convention Center", required = true)
    private String location;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Event description", example = "Join us for a day of innovation and technology insights")
    private String description;

    @NotNull(message = "Colors are required")
    @Size(min = 1, max = 5, message = "Must specify 1-5 colors")
    @Schema(description = "Color scheme for the flyer", example = "[\"blue\", \"white\", \"silver\"]", required = true)
    private List<String> colors;

    @NotBlank(message = "Style is required")
    @Pattern(regexp = "^(professional|elegant|casual|modern|vintage|magical|rustic|minimalist|bold|creative)$", 
             message = "Invalid style. Must be one of: professional, elegant, casual, modern, vintage, magical, rustic, minimalist, bold, creative")
    @Schema(description = "Design style", example = "professional", required = true)
    private String style;

    @Size(max = 200, message = "Additional notes cannot exceed 200 characters")
    @Schema(description = "Additional design notes or requirements")
    private String notes;

    @Schema(description = "Include QR code for event registration", example = "true")
    private Boolean includeQRCode = false;

    @Schema(description = "Include social media handles", example = "true")
    private Boolean includeSocialMedia = false;

    @Schema(description = "Preferred output formats", example = "[\"png\", \"pdf\"]")
    private List<String> outputFormats;
}
