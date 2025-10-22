package ai.eventplanner.sendgrid.service;

import ai.eventplanner.sendgrid.dto.EmailRequest;
import ai.eventplanner.sendgrid.dto.EmailResponse;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SendGrid email service implementation
 */
@Service
public class SendGridService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridService.class);

    private final SendGrid sendGrid;

    public SendGridService(SendGrid sendGrid) {
        this.sendGrid = sendGrid;
    }

    /**
     * Send a simple email
     */
    public EmailResponse sendEmail(String to, String from, String subject, String content) {
        return sendEmail(to, from, subject, content, "text/plain");
    }

    /**
     * Send a simple email with content type
     */
    public EmailResponse sendEmail(String to, String from, String subject, String content, String contentType) {
        EmailRequest request = EmailRequest.builder()
                .to(List.of(EmailRequest.EmailRecipient.builder()
                        .email(to)
                        .build()))
                .from(from)
                .subject(subject)
                .content(content)
                .contentType(contentType)
                .build();

        return sendEmail(request);
    }

    /**
     * Send email using EmailRequest
     */
    public EmailResponse sendEmail(EmailRequest emailRequest) {
        try {
            Mail mail = buildMail(emailRequest);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            return EmailResponse.builder()
                    .success(response.getStatusCode() >= 200 && response.getStatusCode() < 300)
                    .messageId(response.getHeaders().get("X-Message-Id"))
                    .statusCode(response.getStatusCode())
                    .statusMessage(response.getBody())
                    .sentAt(LocalDateTime.now())
                    .requestId(response.getHeaders().get("X-Request-Id"))
                    .build();

        } catch (IOException e) {
            logger.error("Failed to send email via SendGrid", e);
            return EmailResponse.builder()
                    .success(false)
                    .statusCode(500)
                    .statusMessage("Failed to send email: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    /**
     * Send email using template
     */
    public EmailResponse sendTemplateEmail(String to, String from, String templateId, Map<String, Object> templateData) {
        EmailRequest request = EmailRequest.builder()
                .to(List.of(EmailRequest.EmailRecipient.builder()
                        .email(to)
                        .build()))
                .from(from)
                .templateId(templateId)
                .templateData(templateData)
                .build();

        return sendEmail(request);
    }

    /**
     * Build SendGrid Mail object from EmailRequest
     */
    private Mail buildMail(EmailRequest emailRequest) {
        Mail mail = new Mail();

        // Set from email
        mail.setFrom(new Email(emailRequest.getFrom()));

        // Set subject
        mail.setSubject(emailRequest.getSubject());

        // Set content
        if (emailRequest.getTemplateId() != null) {
            // Use template
            mail.setTemplateId(emailRequest.getTemplateId());
        } else {
            // Use content
            Content content = new Content(emailRequest.getContentType(), emailRequest.getContent());
            mail.addContent(content);
        }

        // Add recipients
        for (EmailRequest.EmailRecipient recipient : emailRequest.getTo()) {
            Personalization personalization = new Personalization();
            personalization.addTo(new Email(recipient.getEmail(), recipient.getName()));

            // Add template data if using template
            if (emailRequest.getTemplateData() != null) {
                emailRequest.getTemplateData().forEach((key, value) -> 
                    personalization.addDynamicTemplateData(key, value));
            }

            // Add substitutions if provided
            if (recipient.getSubstitutions() != null) {
                recipient.getSubstitutions().forEach((key, value) -> 
                    personalization.addSubstitution(key, value.toString()));
            }

            mail.addPersonalization(personalization);
        }

        // Add attachments
        if (emailRequest.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : emailRequest.getAttachments()) {
                Attachments sendGridAttachment = new Attachments();
                sendGridAttachment.setContent(attachment.getContent());
                sendGridAttachment.setFilename(attachment.getFilename());
                sendGridAttachment.setType(attachment.getType());
                sendGridAttachment.setDisposition(attachment.getDisposition());
                mail.addAttachments(sendGridAttachment);
            }
        }

        // Add custom args
        if (emailRequest.getCustomArgs() != null) {
            emailRequest.getCustomArgs().forEach((key, value) -> 
                mail.addCustomArg(key, value.toString()));
        }

        return mail;
    }

    /**
     * Validate email address
     */
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    /**
     * Get SendGrid account information
     */
    public String getAccountInfo() {
        try {
            Request request = new Request();
            request.setMethod(Method.GET);
            request.setEndpoint("user/account");

            Response response = sendGrid.api(request);
            return response.getBody();
        } catch (IOException e) {
            logger.error("Failed to get SendGrid account info", e);
            return "Unable to retrieve account information";
        }
    }
}
