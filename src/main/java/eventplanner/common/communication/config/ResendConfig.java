package eventplanner.common.communication.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Resend configuration for email services
 */
@Configuration
@Slf4j
public class ResendConfig {

    @Value("${external.resend.api-key:}")
    private String resendApiKey;

    @Value("${external.resend.from-email:}")
    private String defaultFromEmail;

    @PostConstruct
    public void logConfiguration() {
        if (isApiKeyConfigured()) {
            log.debug("Resend API key is configured");
            log.info("Resend default from email: {}", defaultFromEmail);
        } else {
            log.warn("Resend API key is not configured. Email sending will be disabled. Set RESEND_API_KEY environment variable to enable.");
        }
    }

    public String getApiKey() {
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            throw new IllegalStateException("Resend API key is not configured. Please set RESEND_API_KEY environment variable.");
        }
        return resendApiKey;
    }

    /**
     * Check if the Resend API key is configured
     * @return true if API key is configured, false otherwise
     */
    public boolean isApiKeyConfigured() {
        return resendApiKey != null && !resendApiKey.trim().isEmpty();
    }

    public String getDefaultFromEmail() {
        return defaultFromEmail;
    }
}

