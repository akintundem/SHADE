package eventplanner.common.qrcode.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code qr-code.*} configuration from {@code application.yml}. The properties
 * let us tune default styling while optionally overriding per-event or per-attendee rules.
 */
@Getter
@Setter
@Validated
@ToString
@ConfigurationProperties(prefix = "qr-code")
public class QRCodeProperties {

    private StyleProperties defaults = new StyleProperties();
    private StyleProperties event = new StyleProperties();
    private StyleProperties attendee = new StyleProperties();

    @Getter
    @Setter
    @ToString
    public static class StyleProperties {
        @Min(128)
        @Max(1024)
        private Integer size;
        private String foregroundColor;
        private String backgroundColor;
        private Boolean gradientEnabled;
        private String gradientStartColor;
        private String gradientEndColor;
        private Boolean gradientVertical;
        private String dotShape;
        private Integer quietZone;
        private Boolean roundedCorners;
        private Integer cornerRadius;
        private String errorCorrection;
        private LogoProperties logo = new LogoProperties();
    }

    @Getter
    @Setter
    @ToString
    public static class LogoProperties {
        private Boolean enabled;
        private String path;
        private Integer size;
        private Boolean rounded;
        private Integer padding;
        private Boolean backgroundVisible;
        private String backgroundColor;
        private Boolean borderEnabled;
        private String borderColor;
        private Integer borderThickness;
    }
}

