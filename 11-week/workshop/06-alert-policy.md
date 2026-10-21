# CP7 - Burn-rate alert policy

SLO: 99.5 % availability of `POST /showtimes` in 30 days (`05-slo.md`). Budget: 15 failed requests of
3,000, that is 216 minutes. Rules: `06-alerts.yml`.

## Budget each alert represents

| Alert | Burn rate | Windows | Budget spent when it fires | In requests (of 15) | In minutes (of 216) |
|---|---|---|---|---|---|
| `CatalogShowtimeAvailabilityFastBurn` | 14.4 | 1 h, confirmed in 5 min | 14.4 x 1 h / 720 h = **2 %** | 0.3 | 4.3 |
| `CatalogShowtimeAvailabilitySlowBurn` | 6 | 6 h, confirmed in 30 min | 6 x 6 h / 720 h = **5 %** | 0.75 | 10.8 |

Error ratio that triggers each one: 14.4 x 0.5 % = **7.2 %** (fast) and 6 x 0.5 % = **3 %** (slow).

## Why the traffic guard exists

At about 4 requests per hour, one failure is already 25 % of the window and would page the team for 2 %
of a budget that is less than one request. Both rules add `increase(...) >= 5`: they only evaluate when the
window has at least five requests. The guard is a consequence of the low volume of the journey, and it is
reviewed when real traffic is known.

## Severity and labels

- `severity: page` (fast): the on-call engineer of Catalog answers now; P2 in `incident-management.md`,
  because only administrators use the journey.
- `severity: ticket` (slow): a GitHub issue in `csp-catalog-api`, handled in working hours.
- `team: catalog` on both; `runbook_url` points to `07-runbook.md`.

## Alerts based on causes that will NOT be created

| Alert | Why not |
|---|---|
| CPU of the `catalog-service` pod above 80 % | The user does not feel CPU; if it matters, it shows as latency (SLO 2) or errors (SLO 1) and those alerts already cover it |
| Any `mongodb.query.timeout` line of level ERROR | One timeout is retried and succeeds (see the log example in `02-log-contract.md`); paging on every line would wake the team for requests that ended well |
