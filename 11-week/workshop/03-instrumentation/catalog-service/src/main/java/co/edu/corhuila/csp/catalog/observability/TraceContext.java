package co.edu.corhuila.csp.catalog.observability;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** W3C trace context of one request: the trace shared by every hop and the span of this service. */
public record TraceContext(String traceId, String spanId) {

    private static final Pattern TRACEPARENT = Pattern.compile("00-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public static TraceContext newRoot() {
        return new TraceContext(randomHex(16), randomHex(8));
    }

    /** The context carried by a {@code traceparent} header, empty when it is malformed or all zeros. */
    public static Optional<TraceContext> parse(String traceparent) {
        if (traceparent == null) {
            return Optional.empty();
        }
        Matcher m = TRACEPARENT.matcher(traceparent);
        if (!m.matches() || isZero(m.group(1)) || isZero(m.group(2))) {
            return Optional.empty();
        }
        return Optional.of(new TraceContext(m.group(1), m.group(2)));
    }

    /** Same trace, new span: what this service records when it continues a trace it received. */
    public TraceContext child() {
        return new TraceContext(traceId, randomHex(8));
    }

    public String toTraceparent() {
        return "00-" + traceId + "-" + spanId + "-01";
    }

    private static boolean isZero(String hex) {
        return hex.chars().allMatch(c -> c == '0');
    }

    private static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        do {
            RANDOM.nextBytes(buffer);
        } while (isZero(HexFormat.of().formatHex(buffer)));
        return HexFormat.of().formatHex(buffer);
    }
}
