package eventplanner.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("auth-event")
@ComponentScan(basePackages = {
        "eventplanner.security",
        "eventplanner.event",
        "eventplanner.common",
        "eventplanner.common.config"
})
public class AuthEventOnlyConfig {
}


