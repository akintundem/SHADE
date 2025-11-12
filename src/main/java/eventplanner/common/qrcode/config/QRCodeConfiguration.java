package eventplanner.common.qrcode.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link QRCodeProperties} so Spring can hydrate the values declared in
 * {@code application.yml}.
 */
@Configuration
@EnableConfigurationProperties(QRCodeProperties.class)
public class QRCodeConfiguration {
}

