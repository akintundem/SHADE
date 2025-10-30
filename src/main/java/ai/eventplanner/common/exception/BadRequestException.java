package ai.eventplanner.common.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String code, String message) {
        super(code, message, 400);
    }
}


