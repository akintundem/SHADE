package eventplanner.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Provides a Clock bean for testable time operations.
 * Services inject this instead of calling LocalDateTime.now(ZoneOffset.UTC) directly.
 * Tests can inject Clock.fixed(...) for deterministic behavior.
 */
@Configuration
public class TimeConfig {

    @Bean
    @Primary
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
