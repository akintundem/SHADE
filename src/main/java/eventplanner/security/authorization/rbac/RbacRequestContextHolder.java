package eventplanner.security.authorization.rbac;

/**
 * Simple ThreadLocal holder so services can access the request RBAC context.
 */
public final class RbacRequestContextHolder {
    private static final ThreadLocal<RbacRequestContext> CONTEXT = new ThreadLocal<>();

    private RbacRequestContextHolder() {
    }

    public static void set(RbacRequestContext context) {
        CONTEXT.set(context);
    }

    public static RbacRequestContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
