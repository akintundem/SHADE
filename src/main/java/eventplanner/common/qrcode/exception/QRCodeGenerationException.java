package eventplanner.common.qrcode.exception;

/**
 * Signals a failure while generating a QR code. The generator is designed to
 * provide high-quality output and wraps low-level exceptions so that callers
 * can surface meaningful error messages without leaking implementation details.
 */
public class QRCodeGenerationException extends RuntimeException {

    public QRCodeGenerationException(String message) {
        super(message);
    }

    public QRCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

