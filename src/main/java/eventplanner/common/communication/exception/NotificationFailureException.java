package eventplanner.common.communication.exception;

import java.util.UUID;

/**
 * Exception thrown when a notification fails to send to an external system.
 * This exception should cause transaction rollback to maintain data consistency.
 *
 * Use this when notification delivery is CRITICAL to the operation.
 * For non-critical "best effort" notifications, use the standard send() method
 * and check the response.
 */
public class NotificationFailureException extends RuntimeException {

    private final UUID communicationId;
    private final String notificationType;

    public NotificationFailureException(String message, UUID communicationId) {
        super(message);
        this.communicationId = communicationId;
        this.notificationType = null;
    }

    public NotificationFailureException(String message, UUID communicationId, String notificationType) {
        super(message);
        this.communicationId = communicationId;
        this.notificationType = notificationType;
    }

    public NotificationFailureException(String message, UUID communicationId, Throwable cause) {
        super(message, cause);
        this.communicationId = communicationId;
        this.notificationType = null;
    }

    public NotificationFailureException(String message, UUID communicationId, String notificationType, Throwable cause) {
        super(message, cause);
        this.communicationId = communicationId;
        this.notificationType = notificationType;
    }

    /**
     * Get the communication ID from the Communication table (if available).
     * This can be used to look up the failed communication record for debugging.
     */
    public UUID getCommunicationId() {
        return communicationId;
    }

    /**
     * Get the notification type (EMAIL, PUSH_NOTIFICATION, SMS, etc.)
     */
    public String getNotificationType() {
        return notificationType;
    }

    @Override
    public String toString() {
        return "NotificationFailureException{" +
                "message='" + getMessage() + '\'' +
                ", communicationId=" + communicationId +
                ", notificationType='" + notificationType + '\'' +
                '}';
    }
}
