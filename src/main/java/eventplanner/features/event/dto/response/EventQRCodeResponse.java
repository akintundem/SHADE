package eventplanner.features.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for event QR code information.
 * Includes both the raw QR code value and the rendered Base64 image.
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

    @Schema(description = "Base64 data URI representing the QR code image")
    private String qrCodeImageBase64;

    @Schema(description = "QR code generation timestamp")
    private LocalDateTime generatedAt;
}
