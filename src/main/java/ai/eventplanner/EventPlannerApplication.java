package ai.eventplanner;

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
    "ai.eventplanner.event.repo",
    "ai.eventplanner.attendee.repo",
    "ai.eventplanner.timeline.repo",
    "ai.eventplanner.budget.repo",
    "ai.eventplanner.comms.repository",
    "ai.eventplanner.auth.repo",
    "ai.eventplanner.assistant.repository"
})
@EntityScan(basePackages = {
    "ai.eventplanner.event.model",
    "ai.eventplanner.attendee.model",
    "ai.eventplanner.timeline.model",
    "ai.eventplanner.budget.model",
    "ai.eventplanner.comms.model",
    "ai.eventplanner.auth.entity",
    "ai.eventplanner.assistant.entity"
})
public class EventPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventPlannerApplication.class, args);
    }
}
