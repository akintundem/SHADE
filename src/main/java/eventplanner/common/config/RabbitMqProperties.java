package eventplanner.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

    private String url;
    private String exchange;
    private String emailRoutingKey;
    private String pushRoutingKey;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getEmailRoutingKey() {
        return emailRoutingKey;
    }

    public void setEmailRoutingKey(String emailRoutingKey) {
        this.emailRoutingKey = emailRoutingKey;
    }

    public String getPushRoutingKey() {
        return pushRoutingKey;
    }

    public void setPushRoutingKey(String pushRoutingKey) {
        this.pushRoutingKey = pushRoutingKey;
    }
}
