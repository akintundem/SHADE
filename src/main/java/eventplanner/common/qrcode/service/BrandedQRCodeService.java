package eventplanner.common.qrcode.service;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import eventplanner.common.qrcode.config.QRCodeProperties;
import eventplanner.common.qrcode.enumeration.QRCodeDotShape;
import eventplanner.common.qrcode.generator.QRCodeGenerator;
import eventplanner.common.qrcode.model.QRCodeGenerationResult;
import eventplanner.common.qrcode.model.QRCodeStyle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;

/**
 * Facade that builds {@link QRCodeStyle} instances from configuration and delegates
 * rendering to {@link QRCodeGenerator}. Event and attendee flows can request a
 * pre-configured QR code with a single method call.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandedQRCodeService {

    private static final int DEFAULT_SIZE = 320;
    private static final Color DEFAULT_FOREGROUND = new Color(17, 24, 39);
    private static final Color DEFAULT_BACKGROUND = Color.WHITE;
    private static final int DEFAULT_PADDING = 16;

    private final QRCodeGenerator generator;
    private final QRCodeProperties properties;

    /**
     * Generates a QR code using the global defaults defined in configuration.
     */
    public QRCodeGenerationResult generateDefault(String data) {
        return generator.generate(data, buildStyle(properties.getDefaults(), null));
    }

    /**
     * Generates a QR code using the "event" overrides defined in configuration.
     */
    public QRCodeGenerationResult generateForEvent(String data) {
        return generator.generate(data, buildStyle(properties.getDefaults(), properties.getEvent()));
    }

    /**
     * Generates a QR code using the "attendee" overrides defined in configuration.
     */
    public QRCodeGenerationResult generateForAttendee(String data) {
        return generator.generate(data, buildStyle(properties.getDefaults(), properties.getAttendee()));
    }

    /**
     * Allows features to generate QR codes with custom styles without modifying configuration.
     */
    public QRCodeGenerationResult generate(String data, QRCodeStyle style) {
        return generator.generate(data, style);
    }

    private QRCodeStyle buildStyle(QRCodeProperties.StyleProperties base,
                                   QRCodeProperties.StyleProperties overrides) {
        QRCodeProperties.StyleProperties fallback = base != null ? base : new QRCodeProperties.StyleProperties();
        QRCodeProperties.StyleProperties target = overrides != null ? overrides : new QRCodeProperties.StyleProperties();

        int size = resolveInt(target.getSize(), fallback.getSize(), DEFAULT_SIZE);
        Color foreground = parseColor(firstNonBlank(target.getForegroundColor(), fallback.getForegroundColor()), DEFAULT_FOREGROUND);
        Color background = parseColor(firstNonBlank(target.getBackgroundColor(), fallback.getBackgroundColor()), DEFAULT_BACKGROUND);

        boolean gradientEnabled = resolveBoolean(target.getGradientEnabled(), fallback.getGradientEnabled(), false);
        Color gradientStart = parseColor(firstNonBlank(target.getGradientStartColor(), fallback.getGradientStartColor()), foreground);
        Color gradientEnd = parseColor(firstNonBlank(target.getGradientEndColor(), fallback.getGradientEndColor()), foreground);
        boolean gradientVertical = resolveBoolean(target.getGradientVertical(), fallback.getGradientVertical(), true);

        QRCodeDotShape dotShape = parseDotShape(firstNonBlank(target.getDotShape(), fallback.getDotShape()), QRCodeDotShape.ROUNDED);
        int quietZone = resolveInt(target.getQuietZone(), fallback.getQuietZone(), 2);
        boolean roundedCorners = resolveBoolean(target.getRoundedCorners(), fallback.getRoundedCorners(), true);
        int cornerRadius = resolveInt(target.getCornerRadius(), fallback.getCornerRadius(), 24);
        ErrorCorrectionLevel errorCorrection = parseErrorCorrection(firstNonBlank(target.getErrorCorrection(), fallback.getErrorCorrection()), ErrorCorrectionLevel.H);

        QRCodeStyle.LogoStyle logoStyle = buildLogoStyle(size, fallback.getLogo(), target.getLogo());

        return QRCodeStyle.builder()
                .size(size)
                .foregroundColor(foreground)
                .backgroundColor(background)
                .gradientEnabled(gradientEnabled)
                .gradientStartColor(gradientStart)
                .gradientEndColor(gradientEnd)
                .gradientVertical(gradientVertical)
                .dotShape(dotShape)
                .quietZone(quietZone)
                .roundedCorners(roundedCorners)
                .cornerRadius(cornerRadius)
                .errorCorrection(errorCorrection)
                .logo(logoStyle)
                .build();
    }

    private QRCodeStyle.LogoStyle buildLogoStyle(int canvasSize,
                                                 QRCodeProperties.LogoProperties baseLogo,
                                                 QRCodeProperties.LogoProperties overrideLogo) {
        QRCodeProperties.LogoProperties defaults = baseLogo != null ? baseLogo : new QRCodeProperties.LogoProperties();
        QRCodeProperties.LogoProperties overrides = overrideLogo != null ? overrideLogo : new QRCodeProperties.LogoProperties();

        boolean enabled = resolveBoolean(overrides.getEnabled(), defaults.getEnabled(), true);
        String path = firstNonBlank(overrides.getPath(), defaults.getPath());
        int defaultLogoSize = Math.max(canvasSize / 4, 48);
        int size = Math.min(resolveInt(overrides.getSize(), defaults.getSize(), defaultLogoSize), canvasSize - 32);
        boolean rounded = resolveBoolean(overrides.getRounded(), defaults.getRounded(), true);
        int padding = resolveInt(overrides.getPadding(), defaults.getPadding(), DEFAULT_PADDING);
        boolean backgroundVisible = resolveBoolean(overrides.getBackgroundVisible(), defaults.getBackgroundVisible(), true);
        Color backgroundColor = parseColor(firstNonBlank(overrides.getBackgroundColor(), defaults.getBackgroundColor()), Color.WHITE);
        boolean borderEnabled = resolveBoolean(overrides.getBorderEnabled(), defaults.getBorderEnabled(), false);
        Color borderColor = parseColor(firstNonBlank(overrides.getBorderColor(), defaults.getBorderColor()), new Color(229, 231, 235));
        int borderThickness = resolveInt(overrides.getBorderThickness(), defaults.getBorderThickness(), 2);

        return QRCodeStyle.LogoStyle.builder()
                .enabled(enabled)
                .path(path)
                .size(size)
                .rounded(rounded)
                .padding(padding)
                .backgroundVisible(backgroundVisible)
                .backgroundColor(backgroundColor)
                .borderEnabled(borderEnabled)
                .borderColor(borderColor)
                .borderThickness(borderThickness)
                .build();
    }

    private int resolveInt(Integer value, Integer fallback, int defaultValue) {
        if (value != null) {
            return value;
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultValue;
    }

    private boolean resolveBoolean(Boolean value, Boolean fallback, boolean defaultValue) {
        if (value != null) {
            return value;
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultValue;
    }

    private String firstNonBlank(String candidate, String fallback) {
        if (StringUtils.hasText(candidate)) {
            return candidate;
        }
        return fallback;
    }

    private Color parseColor(String color, Color defaultColor) {
        if (!StringUtils.hasText(color)) {
            return defaultColor;
        }

        String value = color.trim().replace("#", "");
        try {
            if (value.length() == 6) {
                return Color.decode("#" + value);
            }
            if (value.length() == 8) {
                int alpha = Integer.parseInt(value.substring(0, 2), 16);
                int red = Integer.parseInt(value.substring(2, 4), 16);
                int green = Integer.parseInt(value.substring(4, 6), 16);
                int blue = Integer.parseInt(value.substring(6, 8), 16);
                return new Color(red, green, blue, alpha);
            }
            log.warn("Unsupported colour value '{}', falling back to {}", color, defaultColor);
            return defaultColor;
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse colour '{}', falling back to {}", color, defaultColor);
            return defaultColor;
        }
    }

    private QRCodeDotShape parseDotShape(String value, QRCodeDotShape defaultShape) {
        if (!StringUtils.hasText(value)) {
            return defaultShape;
        }
        try {
            return QRCodeDotShape.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unsupported dot shape '{}', falling back to {}", value, defaultShape);
            return defaultShape;
        }
    }

    private ErrorCorrectionLevel parseErrorCorrection(String value, ErrorCorrectionLevel defaultLevel) {
        if (!StringUtils.hasText(value)) {
            return defaultLevel;
        }
        try {
            return ErrorCorrectionLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unsupported error correction level '{}', falling back to {}", value, defaultLevel);
            return defaultLevel;
        }
    }
}

