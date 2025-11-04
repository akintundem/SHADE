package eventplanner.common.communication.services.channel.email;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service for rendering email templates
 */
@Service
public class EmailTemplateService {

    private static final String TEMPLATE_PATH = "templates/welcome-email.html";

    /**
     * Render the SHDE welcome email template with confirmation link
     */
    public String renderWelcomeEmail(String userName, String confirmLink, String baseUrl) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String greeting = userName != null && !userName.trim().isEmpty() 
                ? escapeHtml(userName)
                : "there";

            // Ensure baseUrl ends with / and has proper protocol
            String logoUrl = (baseUrl != null && !baseUrl.isEmpty()) 
                ? baseUrl.replaceAll("/$", "") + "/images/shade_app_icon.png"
                : "https://shde.com/images/shade_app_icon.png";

            return template
                .replace("{{userName}}", greeting)
                .replace("{{confirmLink}}", confirmLink)
                .replace("{{logoUrl}}", logoUrl)
                .replace("{{baseUrl}}", baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://shde.com");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load email template: " + TEMPLATE_PATH, e);
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

