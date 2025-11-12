package eventplanner.common.qrcode.model;

import eventplanner.common.qrcode.exception.QRCodeGenerationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

/**
 * Encapsulates the different representations of a rendered QR code.
 * The generator creates the in-memory image once and lazily derives PNG bytes
 * and a Base64 data URI so that callers can pick whichever format suits the client.
 */
@Getter
@ToString(exclude = "image")
@EqualsAndHashCode(exclude = "image")
public final class QRCodeGenerationResult {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    private final BufferedImage image;
    private final byte[] pngData;
    private final String base64DataUri;

    public QRCodeGenerationResult(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("QR code image cannot be null");
        }
        this.image = image;
        try {
            this.pngData = encodeToBytes(image);
            this.base64DataUri = DATA_URI_PREFIX + Base64.getEncoder().encodeToString(this.pngData);
        } catch (IOException ex) {
            throw new QRCodeGenerationException("Failed to serialise QR code image", ex);
        }
    }

    private byte[] encodeToBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Streams the PNG representation to the provided output stream. The stream remains open.
     */
    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        outputStream.write(pngData);
    }
}

