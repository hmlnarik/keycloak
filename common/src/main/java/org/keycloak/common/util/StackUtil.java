package org.keycloak.common.util;

import java.util.regex.Pattern;
import org.jboss.logging.Logger;


/**
 *
 * @author hmlnarik
 */
public class StackUtil {
    private static final Logger LOG = Logger.getLogger("org.keycloak.STACK_TRACE");
    public static StringBuilder getShortStackTraceIfTraceEnabled() {
        return getShortStackTraceIfTraceEnabled("\n    ");
    }

    private static final Pattern IGNORED = Pattern.compile("^sun\\.|java\\.lang\\.reflect\\.");
    private static final StringBuilder EMPTY = new StringBuilder(0);

    public static StringBuilder getShortStackTraceIfTraceEnabled(String prefix) {
        if (! LOG.isTraceEnabled()) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = (new Throwable()).getStackTrace();
        for (int endIndex = 2; endIndex < stackTrace.length; endIndex++) {
            StackTraceElement st = stackTrace[endIndex];
            if (IGNORED.matcher(st.getClassName()).find()) {
                continue;
            }
            if (st.getClassName().startsWith("org.jboss.resteasy")) {
                break;
            }
            sb.append(prefix).append(st);
        }
        return sb;
    }
}
