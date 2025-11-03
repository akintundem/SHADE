package ai.eventplanner.resend.service;

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
    public String renderWelcomeEmail(String userName, String confirmLink) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String greeting = userName != null && !userName.trim().isEmpty() 
                ? "Greetings " + escapeHtml(userName) + ","
                : "Greetings and welcome!";

            return template
                .replace("{{greeting}}", greeting)
                .replace("{{confirmLink}}", confirmLink);
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

