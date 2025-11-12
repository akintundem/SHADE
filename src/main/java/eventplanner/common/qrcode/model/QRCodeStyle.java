package eventplanner.common.qrcode.model;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import eventplanner.common.qrcode.enumeration.QRCodeDotShape;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.awt.Color;

/**
 * Rich styling configuration for rendering branded QR codes.
 * <p>
 * The {@link eventplanner.common.qrcode.generator.QRCodeGenerator} consumes this object to decide how the matrix is drawn:
 * size, color palette, gradients, corner smoothing, and optional logo overlays.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class QRCodeStyle {

    /**
     * Final pixel size of the square QR code canvas. Larger sizes yield higher fidelity.
     */
    private final int size;

    /**
     * Primary color used for the modules (dots) when gradients are disabled.
     */
    private final Color foregroundColor;

    /**
     * Canvas background color. Transparency is supported via the alpha channel.
     */
    private final Color backgroundColor;

    /**
     * Enables a linear gradient that transitions between {@code gradientStartColor}
     * and {@code gradientEndColor}. When disabled, {@code foregroundColor} is used.
     */
    private final boolean gradientEnabled;

    /**
     * Starting color of the gradient. Ignored when {@code gradientEnabled} is {@code false}.
     */
    private final Color gradientStartColor;

    /**
     * Ending color of the gradient. Ignored when {@code gradientEnabled} is {@code false}.
     */
    private final Color gradientEndColor;

    /**
     * Controls the gradient direction. When {@code true} the gradient flows vertically,
     * otherwise it flows horizontally.
     */
    private final boolean gradientVertical;

    /**
     * Geometric shape used to paint each QR module.
     */
    private final QRCodeDotShape dotShape;

    /**
     * Quiet zone (margin) around the QR code, expressed in modules. Higher values improve
     * scanner tolerance when logos or heavy gradients are used.
     */
    private final int quietZone;

    /**
     * Applies rounded clipping to the final QR code image.
     */
    private final boolean roundedCorners;

    /**
     * Radius of the outer rounded corners, in pixels, when {@code roundedCorners} is enabled.
     */
    private final int cornerRadius;

    /**
     * Configuration for overlaying a logo in the QR code centre.
     */
    private final LogoStyle logo;

    /**
     * ZXing error correction level. Higher levels are more resilient to logo overlays
     * but increase QR density.
     */
    private final ErrorCorrectionLevel errorCorrection;

    /**
     * Behavioural and visual styling for the embedded logo.
     */
    @Getter
    @Builder(toBuilder = true)
    @ToString
    public static class LogoStyle {

        /**
         * When {@code false}, the generator skips logo rendering entirely.
         */
        @Builder.Default
        private final boolean enabled = true;

        /**
         * Classpath location of the logo image. PNG with transparency is recommended.
         */
        private final String path;

        /**
         * Maximum rendered size of the logo in pixels. The generator ensures the logo
         * never exceeds the QR canvas.
         */
        private final int size;

        /**
         * Enables rounded clipping for the logo image.
         */
        @Builder.Default
        private final boolean rounded = true;

        /**
         * Padding around the logo when drawing the optional background capsule.
         */
        private final int padding;

        /**
         * Renders a solid background capsule behind the logo to maintain contrast.
         */
        @Builder.Default
        private final boolean backgroundVisible = true;

        /**
         * Background colour used for the capsule drawn behind the logo.
         */
        private final Color backgroundColor;

        /**
         * Optional border around the logo capsule. Disabled when {@code false}.
         */
        @Builder.Default
        private final boolean borderEnabled = false;

        /**
         * Colour used for the border around the capsule when {@code borderEnabled} is {@code true}.
         */
        private final Color borderColor;

        /**
         * Thickness, in pixels, of the capsule border.
         */
        private final int borderThickness;
    }
}

