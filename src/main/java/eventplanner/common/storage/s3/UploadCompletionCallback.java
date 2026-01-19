package eventplanner.common.storage.s3;

import eventplanner.security.auth.service.UserPrincipal;

import java.util.UUID;

/**
 * Callback interface for handling upload completion logic specific to each entity type.
 * Implementations can update entity status, set URLs, etc.
 */
public interface UploadCompletionCallback {
    /**
     * Called when upload is completed successfully
     * @param entityId The ID of the entity that owns this upload
     * @param mediaId The ID of the uploaded media
     * @param principal The user who completed the upload
     */
    void onUploadCompleted(UUID entityId, UUID mediaId, UserPrincipal principal);
}
