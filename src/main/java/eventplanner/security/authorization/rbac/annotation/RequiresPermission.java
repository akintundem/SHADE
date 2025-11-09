package eventplanner.security.authorization.rbac.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative RBAC guard that maps a controller/service method to a policy permission.
 * <p>
 * Optional {@code resources} entries follow the {@code key=SpEL expression} format,
 * e.g. {@code "event_id=#eventId"}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    String value();

    String[] resources() default {};
}
