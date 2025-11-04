package ai.eventplanner.pushnotification.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if Firebase configuration is properly provided
 */
public class FirebaseCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Firebase is disabled - always return false to prevent FirebaseConfig from loading
        return false;
    }
}

