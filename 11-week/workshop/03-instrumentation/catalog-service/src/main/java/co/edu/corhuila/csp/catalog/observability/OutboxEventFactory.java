package co.edu.corhuila.csp.catalog.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the {@code outbox_event} document of ShowtimePublished. catalog-service never talks to RabbitMQ
 * (ADR-019), so the trace context is stored in the document and csp-worker copies it to the message header.
 */
public class OutboxEventFactory {

    public Map<String, Object> showtimePublished(UUID showtimeId, TraceContext trace) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("_id", UUID.randomUUID().toString());
        document.put("aggregate_type", "Showtime");
        document.put("aggregate_id", showtimeId.toString());
        document.put("event_type", "ShowtimePublished");
        document.put("traceparent", trace.toTraceparent());
        return document;
    }
}
