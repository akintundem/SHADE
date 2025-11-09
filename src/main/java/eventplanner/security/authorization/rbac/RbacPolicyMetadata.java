package eventplanner.security.authorization.rbac;

import lombok.Builder;
import lombok.Value;

/**
 * Metadata extracted from the RBAC policy document for observability.
 */
@Value
@Builder
public class RbacPolicyMetadata {
    String name;
    String version;
    String source;
}
