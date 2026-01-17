package eventplanner.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external")
public class ExternalServicesProperties {

    private final EmailService emailService = new EmailService();
    private final PushService pushService = new PushService();
    private final Email email = new Email();

    public EmailService getEmailService() {
        return emailService;
    }

    public PushService getPushService() {
        return pushService;
    }

    public Email getEmail() {
        return email;
    }

    public static class EmailService {
        private String url;
        private String secret;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class PushService {
        private String url;
        private String secret;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Email {
        private String from;
        private String fromEvents;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getFromEvents() {
            return fromEvents;
        }

        public void setFromEvents(String fromEvents) {
            this.fromEvents = fromEvents;
        }
    }
}
