package eventplanner.common.storage.s3.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class AwsS3ConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("aws.s3.access-key-id"))
                && StringUtils.hasText(context.getEnvironment().getProperty("aws.s3.secret-access-key"))
                && StringUtils.hasText(context.getEnvironment().getProperty("aws.s3.region"))
                && (StringUtils.hasText(context.getEnvironment().getProperty("aws.s3.buckets.user"))
                    || StringUtils.hasText(context.getEnvironment().getProperty("aws.s3.buckets.event")));
    }
}
