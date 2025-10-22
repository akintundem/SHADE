package ai.eventplanner.sendgrid.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SendGrid configuration for email services
 */
@Configuration
public class SendGridConfig {

    @Value("${external.sendgrid.api-key:}")
    private String sendGridApiKey;

    @Bean
    public SendGrid sendGrid() {
        if (sendGridApiKey == null || sendGridApiKey.trim().isEmpty() || sendGridApiKey.equals("your-sendgrid-api-key")) {
            throw new IllegalStateException("SendGrid API key is not configured. Please set SENDGRID_API_KEY environment variable.");
        }
        return new SendGrid(sendGridApiKey);
    }
}
