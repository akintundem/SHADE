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
    "ai.eventplanner.assistant.repository",
    "ai.eventplanner.admin.repository",
    "ai.eventplanner.risk.repository",
    "ai.eventplanner.checklist.repository",
    "ai.eventplanner.roles.repo",
    "ai.eventplanner.user.repo",
    "ai.eventplanner.vendor.repository"
})
@EntityScan(basePackages = {
    "ai.eventplanner.event.entity",
    "ai.eventplanner.event.model",
    "ai.eventplanner.attendee.model",
    "ai.eventplanner.attendee.entity",
    "ai.eventplanner.timeline.model",
    "ai.eventplanner.timeline.entity",
    "ai.eventplanner.budget.model",
    "ai.eventplanner.budget.entity",
    "ai.eventplanner.comms.model",
    "ai.eventplanner.comms.entity",
    "ai.eventplanner.auth.entity",
    "ai.eventplanner.assistant.entity",
    "ai.eventplanner.vendor.entity",
    "ai.eventplanner.checklist.entity",
    "ai.eventplanner.roles.entity",
    "ai.eventplanner.user.entity",
    "ai.eventplanner.admin.entity",
    "ai.eventplanner.risk.entity"
})
public class EventPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventPlannerApplication.class, args);
    }
}
