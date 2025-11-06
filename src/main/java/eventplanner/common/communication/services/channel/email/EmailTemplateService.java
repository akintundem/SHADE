package eventplanner.common.communication.services.channel.email;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for preparing email template variables for Resend templates
 * Templates are managed on Resend dashboard, we just pass the template name/slug and variables
 */
@Service
public class EmailTemplateService {
    
    // Resend template names/slugs (as defined in Resend dashboard)
    public static final String TEMPLATE_EMAIL_VERIFICATION = "email-verification";

    /**
     * Prepare template variables for the email verification/welcome email template
     * Templates are created on Resend dashboard, we just pass variables here
     * 
     * @param userName User's name (can be null)
     * @param confirmLink Email verification confirmation link
     * @param baseUrl Base URL for the application
     * @return Map of template variables for Resend template
     */
    public Map<String, Object> prepareWelcomeEmailVariables(String userName, String confirmLink, String baseUrl) {
        Map<String, Object> variables = new HashMap<>();
        
        // Common variables that might be used in the template
        String greeting = userName != null && !userName.trim().isEmpty() 
            ? userName 
            : "there";
        
        variables.put("userName", greeting);
        variables.put("name", greeting); // Alternative key in case template uses "name"
        variables.put("confirmLink", confirmLink);
        variables.put("verificationLink", confirmLink); // Alternative key
        variables.put("baseUrl", baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://shde.com");
        
        // Ensure baseUrl ends with / and has proper protocol
        String logoUrl = (baseUrl != null && !baseUrl.isEmpty()) 
            ? baseUrl.replaceAll("/$", "") + "/images/shade_app_icon.png"
            : "https://shde.com/images/shade_app_icon.png";
        variables.put("logoUrl", logoUrl);
        
        return variables;
    }
}

