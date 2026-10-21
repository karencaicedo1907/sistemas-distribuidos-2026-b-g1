package co.edu.corhuila.csp.catalog.observability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plain Java checks (no Maven on this machine): exits with code 1 when any check fails. */
public class TracePropagationTest {

    private static final String TRACE_ID = "7d1c3a90b4e84f52a6c08e1f93b25d47";
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        traceIdReceivedIsTheOneStoredInTheOutboxEvent();
        malformedTraceparentStartsANewTrace();
        metricsCountRequestsByOutcome();
        System.out.println("tests: " + (passed + failed) + ", passed: " + passed + ", failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void traceIdReceivedIsTheOneStoredInTheOutboxEvent() {
        var request = new TraceContextMiddleware().handle(
                Map.of("traceparent", "00-" + TRACE_ID + "-b7ad6b7169203331-01"), new HashMap<>());
        var outbox = new OutboxEventFactory().showtimePublished(UUID.randomUUID(), request.trace());
        check("outbox keeps the trace_id received", outbox.get("traceparent").toString().contains(TRACE_ID));
    }

    private static void malformedTraceparentStartsANewTrace() {
        var response = new HashMap<String, String>();
        var request = new TraceContextMiddleware().handle(Map.of("traceparent", "not-a-traceparent"), response);
        check("new trace_id has 32 hex", request.trace().traceId().matches("[0-9a-f]{32}"));
        check("response carries traceparent", response.get("traceparent").contains(request.trace().traceId()));
    }

    private static void metricsCountRequestsByOutcome() {
        var metrics = new RequestMetrics();
        metrics.record("/showtimes", "conflict", 0.07);
        check("counter by outcome", metrics.render().contains("catalog_requests_total{route=\"/showtimes\",outcome=\"conflict\"} 1"));
        check("histogram bucket", metrics.render().contains("catalog_request_duration_seconds_bucket{le=\"0.1\"} 1"));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + name);
        }
    }
}
