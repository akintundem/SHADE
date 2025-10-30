package ai.eventplanner.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("auth-event")
@ComponentScan(basePackages = {
        "ai.eventplanner.auth",
        "ai.eventplanner.event",
        "ai.eventplanner.common",
        "ai.eventplanner.config"
})
public class AuthEventOnlyConfig {
}


