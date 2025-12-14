package eventplanner.common.storage.upload;

/**
 * Status of media upload for entities that require async S3 uploads.
 */
public enum MediaUploadStatus {
    /**
     * Upload completed successfully (or not required for this entity type)
     */
    COMPLETED,
    
    /**
     * Upload is pending - entity created but media not yet uploaded to S3
     */
    PENDING,
    
    /**
     * Upload failed or timed out
     */
    FAILED
}
