package co.edu.corhuila.csp.catalog.observability;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** First step of every request of catalog-service: it reads or creates the trace context and answers with it. */
public class TraceContextMiddleware {

    public static final String TRACEPARENT = "traceparent";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** Everything the rest of the request needs to log and to call downstream. */
    public record RequestContext(TraceContext trace, String correlationId) {}

    /** Reads the headers received and writes the same context in the response headers. */
    public RequestContext handle(Map<String, String> requestHeaders, Map<String, String> responseHeaders) {
        TraceContext trace = TraceContext.parse(requestHeaders.get(TRACEPARENT))
                .map(TraceContext::child)
                .orElseGet(TraceContext::newRoot);
        String received = requestHeaders.get(CORRELATION_HEADER);
        String correlationId = received != null && UUID_PATTERN.matcher(received).matches()
                ? received
                : UUID.randomUUID().toString();
        responseHeaders.put(TRACEPARENT, trace.toTraceparent());
        responseHeaders.put(CORRELATION_HEADER, correlationId);
        return new RequestContext(trace, correlationId);
    }
}
