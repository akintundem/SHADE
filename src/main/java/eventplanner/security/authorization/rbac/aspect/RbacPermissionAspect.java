package eventplanner.security.authorization.rbac.aspect;

import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacAuthorizationService;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Aspect that enforces {@link RequiresPermission} annotations via the RBAC authorization service.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacPermissionAspect {

    private final RbacAuthorizationService authorizationService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(permission)")
    public Object enforcePermission(ProceedingJoinPoint joinPoint, RequiresPermission permission) throws Throwable {
        UserPrincipal principal = resolvePrincipal();
        Map<String, Object> resources = evaluateResources(joinPoint, permission.resources());
        authorizationService.assertAuthorized(principal, permission.value(), resources);
        return joinPoint.proceed();
    }

    private UserPrincipal resolvePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    private Map<String, Object> evaluateResources(ProceedingJoinPoint joinPoint, String[] resourceExpressions) {
        Map<String, Object> values = new HashMap<>();
        if (resourceExpressions == null || resourceExpressions.length == 0) {
            return values;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        for (String entry : resourceExpressions) {
            if (!StringUtils.hasText(entry)) {
                continue;
            }
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                log.warn("Invalid resource mapping '{}' on method {}", entry, method.getName());
                continue;
            }
            String key = parts[0].trim().toLowerCase(Locale.US);
            Expression expression = parser.parseExpression(parts[1].trim());
            Object value = expression.getValue(context);
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }
}
