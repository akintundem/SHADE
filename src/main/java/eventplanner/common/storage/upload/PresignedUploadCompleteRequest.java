package eventplanner.common.storage.upload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Generic request for completing a presigned upload
 */
@Data
public class PresignedUploadCompleteRequest {
    @NotBlank(message = "objectKey is required")
    private String objectKey;
    
    private String resourceUrl;
    
    @NotBlank(message = "fileName is required")
    private String fileName;
    
    @NotBlank(message = "contentType is required")
    private String contentType;
    
    private String category;
    private Boolean isPublic = true;
    private String description;
    private String tags;
    private String metadata;
}
