# CP6 - SLIs, SLOs and error budget

Journey: schedule and publish a showtime (`01-journey.md`): `POST /showtimes` in `catalog-service`, then
the relay of `ShowtimePublished` by `csp-worker`. Metrics come from `04-metrics.md`.

## Traffic estimate

| Item | Value | Why |
|---|---|---|
| Rooms in the catalog | 5 | Assumption for the MVP cinema |
| Showtimes scheduled per room per day | 5 | A normal day program |
| Successful schedules per day | 25 (750 per 30 days) | 5 x 5 |
| Requests per day | about 100 (3,000 per 30 days) | Administrators retry, correct overlaps (`409`) and fix `400` errors, about 4 requests per showtime |
| Peak | about 60 requests in one hour | The weekly program is loaded in one session |

The numbers are an assumption, not a measurement: there is no production traffic yet. They are reviewed
when the first real week is measured.

## SLO 1 - Availability

- **SLI:** valid requests that did not end in a server error / valid requests.
- **Valid requests:** every `POST /showtimes`. `conflict` (409) and `client_error` (400, 401, 403) are
  correct answers, so they count as good.
- **PromQL:**
  `sum(rate(catalog_requests_total{route="/showtimes",outcome!="error"}[30d])) / sum(rate(catalog_requests_total{route="/showtimes"}[30d]))`
- **Objective:** 99.5 % in 30 days (the production value of NFR-002 in `non-functional.md`).
- **Budget:** 0.5 % of 3,000 = **15 failed requests** per 30 days, or 0.5 % of 43,200 = **216 minutes** of
  total outage (the same 216 minutes of NFR-002).

## SLO 2 - Latency

- **SLI:** requests answered in 0.5 s or less / all requests.
- **PromQL:**
  `sum(rate(catalog_request_duration_seconds_bucket{le="0.5"}[30d])) / sum(rate(catalog_request_duration_seconds_bucket{le="+Inf"}[30d]))`
- **Scope:** the histogram has no `route` label yet (`04-metrics.md`), so the SLI covers all catalog
  routes until the label is added.
- **Objective:** 95 % in 30 days. The limit is 0.5 s because a schedule writes `showtimes` and
  `outbox_event` in one transaction; the billboard read has its own 250 ms target in NFR-001.
- **Budget:** 5 % of 3,000 = **150 slow requests** per 30 days, or 2,160 minutes in which every request is slow.

## SLO 3 - Freshness of the published event

- **SLI:** events published to RabbitMQ within 30 s of the commit / all events.
- **Metric:** `worker_outbox_relay_duration_seconds` measures the age of the event when `csp-worker`
  publishes it (from `outbox_event.created_at` to the publish confirmation). Buckets: `1, 5, 10, 30, 60, 300`.
- **PromQL:**
  `sum(rate(worker_outbox_relay_duration_seconds_bucket{le="30"}[30d])) / sum(rate(worker_outbox_relay_duration_seconds_bucket{le="+Inf"}[30d]))`
- **Objective:** 99 % in 30 days. The billboard projection and the audit consumers depend on it.
- **Budget:** 1 % of 750 events = **7 late events** per 30 days, or 432 minutes of delay.

## Error budget policy (SLO 1)

| Consumed | Requests | Minutes | What the team does |
|---|---|---|---|
| 50 % | 7.5 | 108 | Review the causes in the next weekly meeting; list the top three failures |
| 75 % | 11.25 | 162 | Only fixes and reliability work enter `develop` of `csp-catalog-api`; the Tech Lead is informed |
| 100 % | 15 | 216 | No promotion to `qa` or `main` of Catalog functionality until the window recovers; write a post-mortem |

Traffic is bursty (the weekly load), so the budget in requests is the number to follow; the minutes
only give a reference.
