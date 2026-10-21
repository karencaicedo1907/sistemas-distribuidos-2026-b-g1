package co.edu.corhuila.csp.catalog.observability;

import java.io.PrintStream;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes one JSON line per event with the fields of the log contract (02-log-contract.md). */
public class StructuredLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final String service;
    private final String version;
    private final String environment;
    private final Clock clock;
    private final PrintStream out;

    public StructuredLogger(String service, String version, String environment, Clock clock, PrintStream out) {
        this.service = service;
        this.version = version;
        this.environment = environment;
        this.clock = clock;
        this.out = out;
    }

    public void log(String level, TraceContextMiddleware.RequestContext request, String event, String message,
            long durationMs, Map<String, String> domainFields) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("timestamp", TIMESTAMP.format(clock.instant().atZone(java.time.ZoneOffset.UTC)));
        line.put("level", level);
        line.put("service", service);
        line.put("version", version);
        line.put("environment", environment);
        line.put("trace_id", request.trace().traceId());
        line.put("span_id", request.trace().spanId());
        line.put("correlation_id", request.correlationId());
        line.put("event", event);
        line.put("message", message);
        line.put("duration_ms", durationMs);
        domainFields.forEach(line::put);
        out.println(toJson(line));
    }

    private static String toJson(Map<String, Object> fields) {
        StringBuilder json = new StringBuilder("{");
        fields.forEach((name, value) -> {
            if (json.length() > 1) {
                json.append(',');
            }
            json.append('"').append(name).append("\":");
            json.append(value instanceof Number ? value.toString() : "\"" + escape(value.toString()) + "\"");
        });
        return json.append('}').toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\n").replace("\r", "\r");
    }
}
