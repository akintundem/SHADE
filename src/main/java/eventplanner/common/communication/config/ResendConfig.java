package eventplanner.common.communication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Resend configuration for email services
 */
@Configuration
public class ResendConfig {

    @Value("${external.resend.api-key:}")
    private String resendApiKey;

    @Value("${external.resend.from-email:noreply@shde.com}")
    private String defaultFromEmail;

    public String getApiKey() {
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            throw new IllegalStateException("Resend API key is not configured. Please set RESEND_API_KEY environment variable.");
        }
        return resendApiKey;
    }

    public String getDefaultFromEmail() {
        return defaultFromEmail;
    }
}

