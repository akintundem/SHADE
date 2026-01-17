package eventplanner.features.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppProperties.class,
        FeedsCleanupProperties.class
})
public class FeaturesPropertiesConfig {
}
