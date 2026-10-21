# CP4 - Golden signals and metric catalog

Journey: schedule and publish a showtime (`01-journey.md`). Names follow the Prometheus conventions:
snake_case, unit suffix (`_seconds`), `_total` on counters.

The demo in `03-instrumentation` already exposes `catalog_requests_total{route,outcome}` and
`catalog_request_duration_seconds_bucket{le}`. The histogram has no `route` label yet: adding it is the
first change before using the latency queries below. Everything else in this file is the target catalog.

## catalog-service

| Signal | Metric | Type | Labels | PromQL for the dashboard |
|---|---|---|---|---|
| Latency | `catalog_request_duration_seconds` | histogram | `route` | `histogram_quantile(0.95, sum by (le) (rate(catalog_request_duration_seconds_bucket{route="/showtimes"}[5m])))` |
| Traffic | `catalog_requests_total` | counter | `route`, `outcome` | `sum by (route) (rate(catalog_requests_total[5m]))` |
| Errors | `catalog_requests_total` | counter | `route`, `outcome` | `sum(rate(catalog_requests_total{outcome="error"}[5m])) / sum(rate(catalog_requests_total[5m]))` |
| Saturation | `catalog_mongodb_pool_in_use` | gauge | none | `max(catalog_mongodb_pool_in_use) / max(catalog_mongodb_pool_max)` |

`outcome` values: `ok`, `conflict` (409, for example `ShowtimeOverlapService` rejects an overlap),
`client_error` (400, 401, 403) and `error` (5xx and MongoDB timeouts).

## api-gateway

| Signal | Metric | Type | Labels | PromQL for the dashboard |
|---|---|---|---|---|
| Latency | `gateway_request_duration_seconds` | histogram | `route` | `histogram_quantile(0.95, sum by (le) (rate(gateway_request_duration_seconds_bucket{route="/api/v1/catalog/showtimes"}[5m])))` |
| Traffic | `gateway_requests_total` | counter | `route`, `outcome` | `sum by (route) (rate(gateway_requests_total[5m]))` |
| Errors | `gateway_requests_total` | counter | `route`, `outcome` | `sum(rate(gateway_requests_total{outcome="error"}[5m])) / sum(rate(gateway_requests_total[5m]))` |
| Saturation | `gateway_inflight_requests` | gauge | none | `max(gateway_inflight_requests)` |

## csp-worker (outbox relay of `ShowtimePublished`)

| Signal | Metric | Type | Labels | PromQL for the dashboard |
|---|---|---|---|---|
| Latency | `worker_outbox_relay_duration_seconds` | histogram | none | `histogram_quantile(0.95, sum by (le) (rate(worker_outbox_relay_duration_seconds_bucket[5m])))` |
| Traffic | `worker_outbox_relayed_total` | counter | `outcome` | `sum(rate(worker_outbox_relayed_total[5m]))` |
| Errors | `worker_outbox_relayed_total` | counter | `outcome` | `sum(rate(worker_outbox_relayed_total{outcome="error"}[5m])) / sum(rate(worker_outbox_relayed_total[5m]))` |
| Saturation | `worker_amqp_publish_inflight` | gauge | none | `max(worker_amqp_publish_inflight)` |

`outbox_event` has no `processed_at` (`data-model.md`), so the lag of the relay cannot be read from the
database; the relay duration and the error ratio are the signals the worker can give.

## Cardinality

| Label | Values | Estimate | Decision |
|---|---|---|---|
| `route` | route templates of `catalog-service.yaml` (`/showtimes/{showtimeId}`, `/movies`, ...) | about 15 | Allowed, always the template, never the raw URL |
| `outcome` | `ok`, `conflict`, `client_error`, `error` | 4 | Allowed |

Series for `catalog_requests_total`: 15 routes x 4 outcomes = about 60. Series for the histogram:
15 routes x 7 buckets (6 `le` plus `+Inf`) = about 105.

Forbidden labels, because their values are unbounded: `showtime_id`, `room_id`, `movie_id`, the raw URL
with identifiers, the `Idempotency-Key`, any user id or email. What replaces them: the route template
for the URL, and the identifier goes to the log (`02-log-contract.md`), where it can be searched by
`trace_id`.

## Histogram buckets (seconds)

`0.05, 0.1, 0.25, 0.5, 1, 3`

- `0.25`: `non-functional.md` asks for a P95 under 250 ms on the billboard query, so the SLO line has
  its own bucket.
- `0.05` and `0.1`: the fast reads of `rooms` and `movies` by index, to see the normal case.
- `0.5` and `1`: writes with the transaction of `showtimes` and `outbox_event`.
- `3`: the 3 s limit of MongoDB used in the timeout example of the log contract; above it, `+Inf`.
