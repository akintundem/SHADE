package eventplanner.features.timeline.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for uploading proof image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to upload proof image for task")
public class UploadProofImageRequest {
    
    @NotBlank(message = "Image URL is required")
    @Schema(description = "URL of the uploaded proof image", required = true, example = "https://storage.example.com/proofs/task-123/image.jpg")
    private String imageUrl;
    
    @Schema(description = "Description of the proof")
    private String description;
}


