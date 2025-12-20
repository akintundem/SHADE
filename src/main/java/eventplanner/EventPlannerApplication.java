package eventplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class
})
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "eventplanner.features.event.repository",
    "eventplanner.features.attendee.repository",
    "eventplanner.features.timeline.repository",
    "eventplanner.features.budget.repository",
    "eventplanner.features.collaboration.repository",
    "eventplanner.features.feeds.repository",
    "eventplanner.common.communication.repository",
    "eventplanner.security.auth.repository",
    "eventplanner.security.authorization.domain.repository",
    "eventplanner.admin.repository"
})
@EntityScan(basePackages = {
    "eventplanner.features.event.entity",
    "eventplanner.features.attendee.entity",
    "eventplanner.features.timeline.entity",
    "eventplanner.features.budget.entity",
    "eventplanner.features.collaboration.entity",
    "eventplanner.features.feeds.entity",
    "eventplanner.common.communication.model",
    "eventplanner.common.communication.core",
    "eventplanner.common.domain.entity",
    "eventplanner.security.auth.entity",
    "eventplanner.security.authorization.domain.entity",
    "eventplanner.admin.entity"
})
public class EventPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventPlannerApplication.class, args);
    }
}
