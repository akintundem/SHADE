package ai.eventplanner.assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AssistantClientConfig {

    @Bean
    public WebClient shadeAssistantClient(
            WebClient.Builder builder,
            @Value("${assistant.service.base-url:http://localhost:9000}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1 * 1024 * 1024))
                        .build())
                .build();
    }
}
