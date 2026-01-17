package eventplanner.features.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private final Ticket ticket = new Ticket();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public static class Ticket {
        private String qrSecret;

        public String getQrSecret() {
            return qrSecret;
        }

        public void setQrSecret(String qrSecret) {
            this.qrSecret = qrSecret;
        }
    }
}
