package ai.eventplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration for HTTP clients
 */
@Configuration
public class WebClientConfig {

    @Value("${assistant.service.base-url:http://python-app:8000}")
    private String shadeAssistantBaseUrl;

    @Bean(name = "shadeAssistantClient")
    public WebClient shadeAssistantClient() {
        return WebClient.builder()
                .baseUrl(shadeAssistantBaseUrl)
                .build();
    }
}

