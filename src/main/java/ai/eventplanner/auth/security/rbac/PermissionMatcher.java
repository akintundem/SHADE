package ai.eventplanner.auth.security.rbac;

/**
 * Utility for matching permissions against wildcard patterns.
 */
public class PermissionMatcher {

    public boolean matches(String required, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        if ("*".equals(pattern)) {
            return true;
        }
        if (!pattern.contains("*")) {
            return required.equals(pattern);
        }
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '.':
                    regex.append("\\.");
                    break;
                default:
                    regex.append(c);
            }
        }
        return required.matches(regex.toString());
    }
}
