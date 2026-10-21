# CP9 - Incident drill and blameless post-mortem

Simulated incident (drill, 2026-10-21): the downstream of hop 3 in `01-journey.md`, MongoDB `catalog_db`,
answers in 3 seconds for 25 % of the `POST /showtimes` requests during 40 minutes (10:00 to 10:40).
Assumption: a 3 s answer reaches the 3,000 ms limit of `02-log-contract.md`, so the request ends in `503`.
Traffic: the weekly program is loaded from 09:00 to 11:00 at 1 request per minute (`05-slo.md`).

## Trace of a slow request

```
POST /api/v1/catalog/showtimes           api-gateway      3,089 ms   503
  POST /showtimes                        catalog-service  3,062 ms   503
    mongodb find movies                  catalog_db          14 ms
    mongodb find rooms                   catalog_db          11 ms
    mongodb find showtimes (overlap)     catalog_db          18 ms
    mongodb transaction showtimes+outbox catalog_db       3,001 ms   timeout
```

No `outbox_event` is written, so there is no relay span in this trace.

## Log lines of that trace

```json
{"timestamp":"2026-10-21T10:12:30.118Z","level":"INFO","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"e3a91c5d7f204b68a1d6902c4b87f351","span_id":"4d82b6f0a1c93e57","correlation_id":"8c2f6e41-9b07-4d35-a1e8-5f3c7d92b604","event":"showtime.schedule.started","message":"scheduling a showtime","duration_ms":0,"room_id":"a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961"}
{"timestamp":"2026-10-21T10:12:33.162Z","level":"ERROR","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"e3a91c5d7f204b68a1d6902c4b87f351","span_id":"9b17c3e5d0a24f86","correlation_id":"8c2f6e41-9b07-4d35-a1e8-5f3c7d92b604","event":"mongodb.transaction.timeout","message":"transaction on showtimes and outbox_event exceeded the 3000 ms limit","duration_ms":3001,"room_id":"a3f5d2c1-7e08-4b6a-9c14-52d8e7f0a961"}
{"timestamp":"2026-10-21T10:12:33.180Z","level":"ERROR","service":"catalog-service","version":"0.3.0","environment":"qa","trace_id":"e3a91c5d7f204b68a1d6902c4b87f351","span_id":"4d82b6f0a1c93e57","correlation_id":"8c2f6e41-9b07-4d35-a1e8-5f3c7d92b604","event":"request.completed","message":"POST /showtimes answered 503","duration_ms":3062}
{"timestamp":"2026-10-21T10:12:33.189Z","level":"WARN","service":"api-gateway","version":"0.3.0","environment":"qa","trace_id":"e3a91c5d7f204b68a1d6902c4b87f351","span_id":"c60e2a97b4d31f08","correlation_id":"8c2f6e41-9b07-4d35-a1e8-5f3c7d92b604","event":"request.completed","message":"response 503 returned to the portal","duration_ms":3089}
```

## SLIs and alerts (minute-by-minute model, expected values)

| Item | Value |
|---|---|
| Availability SLI during the 40 minutes | 75 % (10 of 40 requests failed) |
| Error ratio at the end of the incident, window of 1 h | 10 / 60 = 16.7 % |
| `CatalogShowtimeAvailabilitySlowBurn` (ticket) | fires first, at **10:10** (minute 10): the 6 h window has little traffic |
| `CatalogShowtimeAvailabilityFastBurn` (page) | fires at **10:17** (minute 17), when the 1 h ratio passes 7.2 % |
| Resolution | fast at 10:43, slow at 11:07 |

Step of `07-runbook.md` that stops the damage: **mitigation 3** (restore or restart MongoDB). Mitigation 1
(scale to 3 replicas) does not help, because the slow component is the database.

## Post-mortem (no blame)

### Timeline

| Time | Event |
|---|---|
| 10:00 | MongoDB starts answering in 3 s to 25 % of the requests |
| 10:10 | Slow-burn ticket opens; nobody is paged |
| 10:17 | Fast-burn page; 4.25 requests have failed |
| 10:19 | Karen acknowledges and opens the GitHub issue in `csp-catalog-api` |
| 10:21 | Confirms with the PromQL of the runbook and follows trace `e3a91c5d...` |
| 10:23 | Mitigation 1: scales to 3 replicas; at 10:27 the ratio is the same |
| 10:30 | The Tech Lead approves mitigation 3 |
| 10:34 | MongoDB is restarted with the environment procedure |
| 10:40 | MongoDB answers normally; damage stops |
| 10:43 | Fast alert resolves |

### Impact

- 40 requests, **10 failed** = 10 of 15 = **66.7 % of the monthly budget** (past the 50 % line, before 75 %).
- In minutes: 25 % of 40 = 10 minutes of total outage, only 4.6 % of the 216 minutes. The two numbers
  differ because traffic is bursty; the budget in requests is the one to follow.

### Root cause

The MongoDB node of `catalog_db` answered slowly for a quarter of the operations, and the transaction of
`showtimes` and `outbox_event` had no faster path than the 3,000 ms limit.

### Contributing factors

- Only availability is alerted: slow-but-successful requests would not have paged anyone.
- The pool saturation of MongoDB is not exported (`catalog_mongodb_pool_in_use` is only in the catalog).
- The runbook had no branch for a slow MongoDB; the 7 minutes between 10:23 and 10:30 went into mitigation 1.
- The 3 s limit makes the administrator wait the full time before seeing the error.

### Actions (stories in the post-mortem; not new HUs in csp-docs)

1. **As the Catalog on-call engineer, I want a latency burn-rate alert on `catalog_request_duration_seconds`, so that a slow MongoDB pages me.**
   Acceptance: a rule in `06-alerts.yml` for SLO 2 passes `promtool check rules`; replaying 25 % of requests above 0.5 s for 10 minutes fires it.
2. **As the service owner, I want `catalog_mongodb_pool_in_use` and `catalog_mongodb_pool_max` at `/metrics`, so that I see saturation.**
   Acceptance: both gauges appear in the output of the service; the PromQL of `04-metrics.md` returns a value.
3. **As the on-call engineer, I want a "slow MongoDB" branch in `07-runbook.md`, so that I skip mitigation 1 when the logs show timeouts.**
   Acceptance: the runbook states the log signal and goes to mitigation 3; the next drill reaches it in under 5 minutes.
