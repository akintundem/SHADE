package eventplanner.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.problem.jackson.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

/**
 * Configuration for Zalando Problem library.
 * Enables RFC 7807 Problem Details for HTTP APIs.
 * 
 * This provides standardized, sanitized error responses across the API.
 */
@Configuration
public class ProblemConfig {

    @Bean
    public ProblemModule problemModule() {
        // Don't include stack traces in responses
        return new ProblemModule().withStackTraces(false);
    }

    @Bean
    public ConstraintViolationProblemModule constraintViolationProblemModule() {
        return new ConstraintViolationProblemModule();
    }
}
