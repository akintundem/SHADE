package eventplanner.common.qrcode.generator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import eventplanner.common.qrcode.exception.QRCodeGenerationException;
import eventplanner.common.qrcode.model.QRCodeGenerationResult;
import eventplanner.common.qrcode.model.QRCodeStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Core QR code renderer responsible for translating matrix data and styling
 * instructions into a high-quality PNG image.
 * <p>
 * This generator produces branded QR codes with extensive customization options:
 * <ul>
 *   <li>Custom colors (solid or gradient)</li>
 *   <li>Multiple dot shapes (square, rounded, circle)</li>
 *   <li>Logo overlay with background capsule</li>
 *   <li>Rounded corners on QR code and logo</li>
 *   <li>Configurable error correction levels</li>
 *   <li>High-quality rendering with anti-aliasing</li>
 * </ul>
 * <p>
 * The generator uses ZXing library for matrix encoding and Java2D for rendering,
 * ensuring compatibility with standard QR code scanners while providing a polished,
 * brand-consistent appearance.
 *
 * @author Event Planner Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class QRCodeGenerator {

    /** Minimum QR code size in pixels (128x128) */
    private static final int MIN_SIZE = 128;
    
    /** Maximum QR code size in pixels (1024x1024) */
    private static final int MAX_SIZE = 1024;
    
    /** Thread-safe QR code writer instance for matrix generation */
    private static final QRCodeWriter WRITER = new QRCodeWriter();

    /**
     * Generates a QR code using the supplied data and style.
     * <p>
     * This is the main entry point for QR code generation. The method:
     * <ol>
     *   <li>Validates input parameters</li>
     *   <li>Creates the QR code matrix using ZXing</li>
     *   <li>Renders the matrix with custom styling</li>
     *   <li>Applies logo overlay if configured</li>
     *   <li>Applies rounded corners if enabled</li>
     *   <li>Returns a result object with multiple output formats</li>
     * </ol>
     *
     * @param data  the value encoded inside the QR code (URL, text, etc.)
     * @param style visual configuration to apply (colors, logo, shapes, etc.)
     * @return a rich representation containing BufferedImage, PNG bytes, and Base64 data URI
     * @throws IllegalArgumentException if data is null/empty or style is invalid
     * @throws QRCodeGenerationException if matrix encoding or rendering fails
     */
    public QRCodeGenerationResult generate(String data, QRCodeStyle style) {
        validateInputs(data, style);
        BitMatrix matrix = createMatrix(data, style);
        BufferedImage rendered = renderMatrix(matrix, style);
        return new QRCodeGenerationResult(rendered);
    }

    /**
     * Validates input parameters before QR code generation.
     * Ensures data is non-empty and style size is within acceptable bounds.
     *
     * @param data  the QR code data to validate
     * @param style the style configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateInputs(String data, QRCodeStyle style) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("QR code data cannot be null or empty");
        }
        Objects.requireNonNull(style, "QR code style is required");
        if (style.getSize() < MIN_SIZE || style.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("QR code size must be between " + MIN_SIZE + " and " + MAX_SIZE);
        }
    }

    /**
     * Creates the QR code bit matrix using ZXing encoder.
     * The matrix represents the black/white pattern that forms the QR code.
     *
     * @param data  the data to encode
     * @param style the style configuration (affects error correction and margin)
     * @return the bit matrix representing the QR code pattern
     * @throws QRCodeGenerationException if encoding fails
     */
    private BitMatrix createMatrix(String data, QRCodeStyle style) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, style.getErrorCorrection());
        hints.put(EncodeHintType.MARGIN, style.getQuietZone());

        try {
            return WRITER.encode(data, BarcodeFormat.QR_CODE, style.getSize(), style.getSize(), hints);
        } catch (WriterException ex) {
            throw new QRCodeGenerationException("Unable to encode QR code matrix", ex);
        }
    }

    /**
     * Renders the bit matrix into a high-quality BufferedImage.
     * <p>
     * The rendering process:
     * <ol>
     *   <li>Creates ARGB canvas for transparency support</li>
     *   <li>Fills background color</li>
     *   <li>Draws each module (dot) with custom shape and paint</li>
     *   <li>Overlays logo if configured</li>
     *   <li>Applies rounded corners mask if enabled</li>
     * </ol>
     *
     * @param matrix the bit matrix from ZXing encoder
     * @param style  the styling configuration
     * @return the rendered QR code image
     */
    private BufferedImage renderMatrix(BitMatrix matrix, QRCodeStyle style) {
        int dimension = matrix.getWidth();
        int canvasSize = style.getSize();
        double moduleSize = (double) canvasSize / dimension;

        BufferedImage canvas = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();

        applyQualityHints(graphics);

        // Paint background first so transparent logos blend correctly.
        graphics.setColor(style.getBackgroundColor());
        graphics.fillRect(0, 0, canvasSize, canvasSize);

        Paint modulePaint = resolveModulePaint(style, canvasSize);
        graphics.setPaint(modulePaint);

        // Render each module (dot) in the matrix
        for (int x = 0; x < dimension; x++) {
            for (int y = 0; y < dimension; y++) {
                if (!matrix.get(x, y)) {
                    continue; // Skip white/empty modules
                }
                // Calculate precise position with sub-pixel accuracy
                double xPos = Math.round(x * moduleSize * 100.0) / 100.0;
                double yPos = Math.round(y * moduleSize * 100.0) / 100.0;
                Shape shape = createDotShape(style, xPos, yPos, moduleSize);
                graphics.fill(shape);
            }
        }

        graphics.dispose();

        // Apply logo overlay if configured
        if (style.getLogo() != null && style.getLogo().isEnabled()) {
            overlayLogo(canvas, style.getLogo());
        }

        // Apply rounded corners mask if enabled
        if (style.isRoundedCorners()) {
            return applyRoundedMask(canvas, style.getCornerRadius());
        }

        return canvas;
    }

    /**
     * Applies high-quality rendering hints to ensure smooth, crisp output.
     * These hints enable anti-aliasing and high-quality interpolation for
     * professional-looking QR codes.
     *
     * @param graphics the Graphics2D context to configure
     */
    private void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    /**
     * Resolves the paint to use for QR code modules (dots).
     * Returns either a solid color or a linear gradient based on style configuration.
     *
     * @param style     the style configuration
     * @param canvasSize the canvas size (used for gradient direction)
     * @return the Paint object (Color or LinearGradientPaint)
     */
    private Paint resolveModulePaint(QRCodeStyle style, int canvasSize) {
        if (!style.isGradientEnabled()) {
            return style.getForegroundColor();
        }

        // Calculate gradient direction (vertical or horizontal)
        float endX = style.isGradientVertical() ? 0 : canvasSize;
        float endY = style.isGradientVertical() ? canvasSize : 0;
        return new LinearGradientPaint(
                0f,
                0f,
                endX,
                endY,
                new float[]{0f, 1f},
                new Color[]{style.getGradientStartColor(), style.getGradientEndColor()}
        );
    }

    /**
     * Creates the geometric shape for a single QR code module (dot).
     * The shape type is determined by the style configuration.
     *
     * @param style     the style configuration
     * @param xPos      the X position of the module
     * @param yPos      the Y position of the module
     * @param moduleSize the size of the module in pixels
     * @return the Shape object representing the module
     */
    private Shape createDotShape(QRCodeStyle style, double xPos, double yPos, double moduleSize) {
        return switch (style.getDotShape()) {
            case CIRCLE -> new Ellipse2D.Double(xPos, yPos, moduleSize, moduleSize);
            case ROUNDED -> new RoundRectangle2D.Double(xPos, yPos, moduleSize, moduleSize,
                    moduleSize * 0.75, moduleSize * 0.75);
            case SQUARE -> new RoundRectangle2D.Double(xPos, yPos, moduleSize, moduleSize, 0, 0);
        };
    }

    /**
     * Overlays a logo image in the center of the QR code.
     * <p>
     * The logo is rendered with:
     * <ul>
     *   <li>Optional background capsule for contrast</li>
     *   <li>Optional border around the capsule</li>
     *   <li>Rounded corners if enabled</li>
     *   <li>Automatic scaling to fit within QR code bounds</li>
     * </ul>
     * The logo size is constrained to ensure QR code remains scannable.
     *
     * @param canvas    the QR code canvas to overlay the logo on
     * @param logoStyle the logo styling configuration
     */
    private void overlayLogo(BufferedImage canvas, QRCodeStyle.LogoStyle logoStyle) {
        if (logoStyle.getPath() == null || logoStyle.getPath().isBlank()) {
            log.debug("Logo rendering skipped: path not provided");
            return;
        }

        BufferedImage logoImage = loadLogo(logoStyle.getPath());
        if (logoImage == null) {
            return;
        }

        int canvasSize = canvas.getWidth();
        // Ensure logo size is reasonable (min 24px, max 50% of canvas)
        int targetSize = Math.min(Math.max(logoStyle.getSize(), 24), canvasSize / 2);
        BufferedImage scaledLogo = scaleImage(logoImage, targetSize, targetSize, logoStyle.isRounded());

        Graphics2D graphics = canvas.createGraphics();
        applyQualityHints(graphics);

        // Calculate capsule dimensions (logo + padding)
        int capsuleSize = targetSize + (logoStyle.getPadding() * 2);
        int capsuleX = (canvasSize - capsuleSize) / 2;
        int capsuleY = (canvasSize - capsuleSize) / 2;

        // Draw background capsule if enabled
        if (logoStyle.isBackgroundVisible()) {
            RoundRectangle2D.Double capsule = new RoundRectangle2D.Double(
                    capsuleX,
                    capsuleY,
                    capsuleSize,
                    capsuleSize,
                    capsuleSize * 0.45,
                    capsuleSize * 0.45
            );
            graphics.setColor(logoStyle.getBackgroundColor());
            graphics.fill(capsule);

            // Draw border if enabled
            if (logoStyle.isBorderEnabled()) {
                graphics.setStroke(new BasicStroke(Math.max(1, logoStyle.getBorderThickness())));
                graphics.setColor(logoStyle.getBorderColor());
                graphics.draw(capsule);
            }
        }

        // Center the logo within the capsule
        int logoX = (canvasSize - targetSize) / 2;
        int logoY = (canvasSize - targetSize) / 2;
        graphics.drawImage(scaledLogo, logoX, logoY, null);
        graphics.dispose();
    }

    /**
     * Scales a logo image to the target dimensions with optional rounded corners.
     * Uses high-quality interpolation for smooth scaling.
     *
     * @param source       the source logo image
     * @param targetWidth  the target width
     * @param targetHeight the target height
     * @param rounded      whether to apply rounded corners clipping
     * @return the scaled logo image
     */
    private BufferedImage scaleImage(BufferedImage source, int targetWidth, int targetHeight, boolean rounded) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        applyQualityHints(g2d);
        if (rounded) {
            // Apply rounded corners clipping (40% radius)
            g2d.setClip(new RoundRectangle2D.Double(0, 0, targetWidth, targetHeight, targetWidth * 0.4, targetHeight * 0.4));
        }
        g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return scaled;
    }

    /**
     * Applies rounded corners mask to the entire QR code canvas.
     * Creates a new image with rounded corners by clipping the original.
     *
     * @param canvas the original QR code canvas
     * @param radius the corner radius in pixels
     * @return a new image with rounded corners applied
     */
    private BufferedImage applyRoundedMask(BufferedImage canvas, int radius) {
        int size = canvas.getWidth();
        BufferedImage rounded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rounded.createGraphics();
        applyQualityHints(g2d);

        RoundRectangle2D roundRect = new RoundRectangle2D.Double(0, 0, size, size, radius, radius);
        g2d.setClip(roundRect);
        g2d.drawImage(canvas, 0, 0, null);
        g2d.dispose();
        return rounded;
    }

    /**
     * Loads a logo image from the classpath.
     * Supports PNG, JPEG, and other formats supported by ImageIO.
     *
     * @param path the classpath-relative path to the logo image
     * @return the loaded BufferedImage, or null if loading fails
     */
    private BufferedImage loadLogo(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("Logo resource '{}' not found on classpath, QR code will render without logo.", path);
            return null;
        }
        try (InputStream stream = resource.getInputStream()) {
            return ImageIO.read(stream);
        } catch (IOException ex) {
            log.warn("Failed to load logo '{}' for QR code overlay: {}", path, ex.getMessage());
            return null;
        }
    }
}

