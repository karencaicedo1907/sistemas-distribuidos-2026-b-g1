# CP2 - Structured log contract

One JSON object per line, written to stdout, with the same fields in every service of the journey in
`01-journey.md` (`api-gateway`, `catalog-service`, `csp-worker`). One filter by `trace_id` returns
everything that happened to a request.

## Mandatory fields

| Field | Type | Required | Example | Why it exists |
|---|---|---|---|---|
| `timestamp` | string, ISO-8601 UTC with ms | yes | `2026-10-20T19:31:07.214Z` | Orders events across services |
| `level` | string | yes | `ERROR` | Filters severity (see levels below) |
| `service` | string | yes | `catalog-service` | Tells which service wrote the line |
| `version` | string | yes | `0.3.0` | Links a behaviour to a release |
| `environment` | string | yes | `qa` | Separates `dev`, `qa` and `main` data |
| `trace_id` | string, 32 hex | yes | `7d1c3a90b4e84f52a6c08e1f93b25d47` | Joins the lines of one request over all hops, including the broker |
| `span_id` | string, 16 hex | yes | `b7ad6b7169203331` | Places the line inside the span tree |
| `correlation_id` | string, UUID | yes | `c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33` | The `X-Correlation-Id` that `CorrelationIdFilter` already returns to the client |
| `event` | string, `domain.action.result` | yes | `showtime.overlap.rejected` | Stable name to count and alert on |
| `message` | string | yes | `room already has a showtime in that interval` | Human reading, no sensitive data |
| `duration_ms` | integer | yes (0 if not timed) | `3004` | Feeds latency analysis and the SLO |

## Domain fields (Catalog)

| Field | Type | Required | Example | Why it exists |
|---|---|---|---|---|
| `showtime_id` | UUID string | when known | `5e0b7f1a-2c44-4d1e-8a51-3b9e60d7c812` | Follows one showtime (`showtimes._id`) |
| `room_id` | UUID string | when known | `a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961` | Explains overlap rejections (`ShowtimeOverlapService`) |
| `movie_id` | UUID string | when known | `91c2e4b8-6d3f-4a07-b5e9-0c1a7d84f236` | Links the showtime to its movie snapshot |
| `outbox_event_id` | UUID string | in the relay | `0d6e9a42-18b7-4c35-a2f0-7e5b3c91d468` | Joins the request with the `ShowtimePublished` delivered by `csp-worker` (`outbox_event._id`) |

## Optional field

| Field | Type | Required | Example | Why it exists |
|---|---|---|---|---|
| `actor_hash` | string, 12 hex | on authenticated requests | `3f9a1c7e5b02` | Tells which administrator acted without writing the token subject |

## Never log

| Data | Where it appears | How it is handled |
|---|---|---|
| Bearer token (JWT) | `Authorization` header | Never written; only the result `auth.accepted` / `auth.rejected` |
| Subject of the token (`sub`, administrator id) | JwtAuthenticationFilter | Logged as `actor_hash`: first 12 hex of SHA-256 of the `sub` |
| MongoDB connection string and credentials | `MongoClientLimitsConfiguration` | Never written; log only host alias `catalog_db` |
| RS256 private/public key material | JWT configuration | Never written |
| `Idempotency-Key` value | `createShowtime` header | Written as the first 8 characters plus `...` |
| Full request or response body | `CreateShowtimeRequest` | Never written; only the identifiers above |
| Personal data (names, emails, ID numbers) | Other domains | Catalog does not receive them; if one appears, drop the field |

## Example: seven lines, one trace (failed request)

```json
{"timestamp":"2026-10-20T19:31:07.214Z","level":"INFO","service":"api-gateway","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"b7ad6b7169203331","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"request.received","message":"POST /api/v1/catalog/showtimes routed to catalog-service","duration_ms":0}
{"timestamp":"2026-10-20T19:31:07.231Z","level":"INFO","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"0f4c8e21a96d7b05","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"showtime.schedule.started","message":"scheduling a showtime","duration_ms":0,"movie_id":"91c2e4b8-6d3f-4a07-b5e9-0c1a7d84f236","room_id":"a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961"}
{"timestamp":"2026-10-20T19:31:10.240Z","level":"ERROR","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"6a9d3b50c1e8f742","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"mongodb.query.timeout","message":"find on rooms exceeded the 3000 ms limit","duration_ms":3009,"room_id":"a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961"}
{"timestamp":"2026-10-20T19:31:10.289Z","level":"INFO","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"2e7b5c94d0a3f816","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"mongodb.query.retried","message":"second attempt on rooms succeeded","duration_ms":41,"room_id":"a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961"}
{"timestamp":"2026-10-20T19:31:10.317Z","level":"WARN","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"d18f4a63b7c2e095","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"showtime.overlap.rejected","message":"room already has a showtime in that interval","duration_ms":28,"room_id":"a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961","showtime_id":"5e0b7f1a-2c44-4d1e-8a51-3b9e60d7c812"}
{"timestamp":"2026-10-20T19:31:10.322Z","level":"INFO","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"0f4c8e21a96d7b05","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"request.completed","message":"POST /showtimes answered 409","duration_ms":3091}
{"timestamp":"2026-10-20T19:31:10.330Z","level":"INFO","service":"api-gateway","version":"0.3.0","environment":"qa","trace_id":"7d1c3a90b4e84f52a6c08e1f93b25d47","span_id":"b7ad6b7169203331","correlation_id":"c0a8f1d2-5b3e-4c7a-9e21-6f4d8a1b2c33","event":"request.completed","message":"response 409 returned to the portal","duration_ms":3116}
```

Reading the story: the gateway routes the request, Catalog queries `rooms`, the query times out at
3 s (line 3), the retry works (line 4), the domain then rejects the showtime because the room is
busy (line 5, business rule, answer `409`), and both services close the request.

## Levels

| Level | When the team uses it | Example |
|---|---|---|
| `ERROR` | A dependency or the service failed and the request may be lost | `mongodb.query.timeout` on `rooms` |
| `WARN` | A business rule or a limit rejected the request, or a retry was needed | `showtime.overlap.rejected` (answer `409`) |
| `INFO` | A normal step of the journey | `showtime.schedule.started`, `request.completed` |
| `DEBUG` | Detail for diagnosis, off in `qa` and `main` | Overlap query interval boundaries (no identifiers of people) |
