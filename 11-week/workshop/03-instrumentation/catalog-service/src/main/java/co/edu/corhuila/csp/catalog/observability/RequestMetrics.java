package co.edu.corhuila.csp.catalog.observability;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** The two metrics of the journey, in the Prometheus text format served at /metrics. */
public class RequestMetrics {

    private static final double[] BUCKETS = {0.05, 0.1, 0.25, 0.5, 1, 3};

    private final Map<String, LongAdder> requests = new ConcurrentHashMap<>();
    private final LongAdder[] bucketCounts = new LongAdder[BUCKETS.length + 1];
    private final LongAdder durationCount = new LongAdder();

    public RequestMetrics() {
        for (int i = 0; i < bucketCounts.length; i++) {
            bucketCounts[i] = new LongAdder();
        }
    }

    public void record(String route, String outcome, double seconds) {
        requests.computeIfAbsent("route=\"" + route + "\",outcome=\"" + outcome + "\"", k -> new LongAdder()).increment();
        for (int i = 0; i < BUCKETS.length; i++) {
            if (seconds <= BUCKETS[i]) {
                bucketCounts[i].increment();
            }
        }
        bucketCounts[BUCKETS.length].increment();
        durationCount.increment();
    }

    public String render() {
        StringBuilder text = new StringBuilder();
        new TreeMap<>(requests).forEach((labels, count) ->
                text.append("catalog_requests_total{").append(labels).append("} ").append(count.sum()).append('\n'));
        for (int i = 0; i < BUCKETS.length; i++) {
            text.append("catalog_request_duration_seconds_bucket{le=\"").append(BUCKETS[i]).append("\"} ")
                    .append(bucketCounts[i].sum()).append('\n');
        }
        text.append("catalog_request_duration_seconds_bucket{le=\"+Inf\"} ").append(durationCount.sum()).append('\n');
        return text.toString();
    }
}
