package eventplanner.common.communication.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Sanitizes user-supplied values before they are injected into email templates.
 *
 * <p>Template engines (Resend / React-Email, Handlebars, etc.) render content
 * server-side and send it directly to email clients.  If user-controlled data
 * is embedded without escaping — e.g. an event name that contains HTML tags —
 * it can result in:
 * <ul>
 *   <li>HTML injection in email clients that render HTML</li>
 *   <li>Phishing content spoofing legitimate system emails</li>
 *   <li>Injected hyperlinks or images redirecting recipients</li>
 * </ul>
 *
 * <p>This class HTML-encodes the five core characters ({@code & < > " '}) in
 * every {@link String} value found in the template variable map, including one
 * level of nested maps.  Non-string values (numbers, booleans, etc.) are left
 * unchanged because they carry no injection risk.
 */
public final class EmailVariableSanitizer {

    private EmailVariableSanitizer() {
    }

    /**
     * Return a new map with all {@link String} values HTML-escaped.
     * The original map is not modified.
     *
     * @param variables raw template variables (may be {@code null})
     * @return sanitized copy, or an empty map when input is {@code null}
     */
    public static Map<String, Object> sanitize(Map<String, Object> variables) {
        if (variables == null) {
            return new HashMap<>();
        }
        Map<String, Object> sanitized = new HashMap<>(variables.size());
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }
        return sanitized;
    }

    private static Object sanitizeValue(Object value) {
        if (value instanceof String s) {
            return escapeHtml(s);
        }
        if (value instanceof Map<?, ?> nested) {
            // Sanitize one level of nested maps (e.g., event metadata objects)
            Map<String, Object> sanitizedNested = new HashMap<>(nested.size());
            for (Map.Entry<?, ?> entry : nested.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toString() : "";
                sanitizedNested.put(key, sanitizeValue(entry.getValue()));
            }
            return sanitizedNested;
        }
        return value;
    }

    /**
     * Escape the five HTML-significant characters.
     * Uses numeric entities for {@code '} to maximise compatibility across
     * HTML contexts and older email clients.
     */
    static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&'  -> sb.append("&amp;");
                case '<'  -> sb.append("&lt;");
                case '>'  -> sb.append("&gt;");
                case '"'  -> sb.append("&quot;");
                case '\'' -> sb.append("&#x27;");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
