package eventplanner.common.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for the monolithic Event Planner application
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    private final RedisProperties redisProperties;

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        // Read from environment variable directly if property is not set or is localhost
        // This works around Spring Boot property resolution issues with ${SPRING_REDIS_HOST} in application.yml
        String host = redisProperties.getHost();
        if (host == null || host.isEmpty() || "localhost".equals(host)) {
            String envHost = System.getenv("SPRING_REDIS_HOST");
            if (envHost != null && !envHost.isEmpty()) {
                host = envHost;
            }
        }
        int port = redisProperties.getPort();
        if (port == 0) {
            String envPort = System.getenv("SPRING_REDIS_PORT");
            if (envPort != null && !envPort.isEmpty()) {
                port = Integer.parseInt(envPort);
            } else {
                port = 6379; // default
            }
        }
        standaloneConfiguration.setHostName(host);
        standaloneConfiguration.setPort(port);

        String redisPassword = redisProperties.getPassword();
        if (redisPassword != null && !redisPassword.isEmpty()) {
            standaloneConfiguration.setPassword(RedisPassword.of(redisPassword));
        }
        standaloneConfiguration.setDatabase(redisProperties.getDatabase());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();

        Duration redisTimeout = redisProperties.getTimeout();
        if (redisTimeout != null) {
            builder.commandTimeout(redisTimeout);
        }

        LettuceClientConfiguration clientConfiguration = builder.build();

        return new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
