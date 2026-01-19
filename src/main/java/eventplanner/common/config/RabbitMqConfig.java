package eventplanner.common.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    @Bean
    public ConnectionFactory rabbitConnectionFactory(RabbitMqProperties properties) {
        String url = properties.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("rabbitmq.url must be configured");
        }

        CachingConnectionFactory factory = new CachingConnectionFactory();
        try {
            factory.setUri(url);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid rabbitmq.url", ex);
        }

        return factory;
    }

    @Bean
    public DirectExchange notificationsExchange(RabbitMqProperties properties) {
        String exchange = properties.getExchange();
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("rabbitmq.exchange must be configured");
        }
        return new DirectExchange(exchange, true, false);
    }
}
