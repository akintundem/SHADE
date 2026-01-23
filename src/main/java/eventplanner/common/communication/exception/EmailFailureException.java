package eventplanner.common.communication.exception;

/**
 * Exception thrown when email fails to be queued to RabbitMQ.
 * Indicates infrastructure failure (RabbitMQ down, serialization error, etc.)
 */
public class EmailFailureException extends RuntimeException {

    private final String recipient;
    private final String templateId;

    public EmailFailureException(String message, String recipient) {
        super(message);
        this.recipient = recipient;
        this.templateId = null;
    }

    public EmailFailureException(String message, String recipient, String templateId) {
        super(message);
        this.recipient = recipient;
        this.templateId = templateId;
    }

    public EmailFailureException(String message, String recipient, Throwable cause) {
        super(message, cause);
        this.recipient = recipient;
        this.templateId = null;
    }

    public EmailFailureException(String message, String recipient, String templateId, Throwable cause) {
        super(message, cause);
        this.recipient = recipient;
        this.templateId = templateId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getTemplateId() {
        return templateId;
    }

    @Override
    public String toString() {
        return "EmailFailureException{" +
                "message='" + getMessage() + '\'' +
                ", recipient='" + recipient + '\'' +
                ", templateId='" + templateId + '\'' +
                '}';
    }
}
