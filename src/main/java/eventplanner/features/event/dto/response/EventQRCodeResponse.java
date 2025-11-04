package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Response DTO for event QR code information
 */
@Schema(description = "Event QR code information response")
@Getter
@Setter
public class EventQRCodeResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "QR code value")
    private String qrCode;

    @Schema(description = "Whether QR code is enabled")
    private Boolean qrCodeEnabled;

    @Schema(description = "QR code image URL")
    private String qrCodeImageUrl;

    @Schema(description = "QR code generation timestamp")
    private String generatedAt;
}
