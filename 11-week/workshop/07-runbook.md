# CP8 - Runbook: CatalogShowtimeAvailabilityFastBurn

Alert in `06-alerts.yml`. SLO: 99.5 % availability of `POST /showtimes` (`05-slo.md`). Severity P2
(`incident-management.md`). Service runbook: `csp-docs/09-microservices/services/03-catalog-service/runbook.md`.

## Symptoms

- The page `CatalogShowtimeAvailabilityFastBurn` arrives: more than 7.2 % of `POST /showtimes` end in `error` (5xx
  or MongoDB timeout) in 1 hour and in the last 5 minutes.
- The administrator sees errors when scheduling a showtime in `csp-catalog-portal`.
- `ShowtimePublished` stops appearing, because no `outbox_event` is written when the transaction fails.

## Confirm

1. Error ratio now (must be above 0.072):
   `sum(rate(catalog_requests_total{route="/showtimes",outcome="error"}[5m])) / sum(rate(catalog_requests_total{route="/showtimes"}[5m]))`
2. Latency: `histogram_quantile(0.95, sum by (le) (rate(catalog_request_duration_seconds_bucket[5m])))`
3. Find a failed request and follow its trace (logs are JSON, `02-log-contract.md`):

```bash
kubectl logs -n "${K8S_NAMESPACE}" -l app=catalog-service --tail=2000 | jq -c 'select(.level=="ERROR")' | head
kubectl logs -n "${K8S_NAMESPACE}" -l app=catalog-service --tail=2000 | jq -c 'select(.trace_id=="<trace_id>")'
```

4. Readiness: `curl http://localhost:8082/api/v1/catalog/health/ready` must show MongoDB connected.

## Mitigations, from lowest to highest risk

| # | Action | When it helps | Command |
|---|---|---|---|
| 1 | Scale the service | Errors come from saturation of the pods | `kubectl scale deployment/catalog-service -n "${K8S_NAMESPACE}" --replicas=3` |
| 2 | Roll back the last deploy | The alert started right after a deploy | `kubectl rollout undo deployment/catalog-service -n "${K8S_NAMESPACE}"` |
| 3 | Restore or restart MongoDB with the environment procedure | The logs show `mongodb.query.timeout` or `mongodb.transaction.timeout` | Only with Tech Lead approval; do not recreate collections by hand; reapply the Liquibase changesets of `csp-catalog-db` and check the indexes before reopening traffic |

Not available today because the architecture does not define them: feature flags and a circuit breaker.
Do not edit `showtimes` or `outbox_event` by hand.

## Escalation

| Role | Who | When |
|---|---|---|
| On-call engineer and service owner | Karen | First responder; leads a P2 |
| Incident commander and tech lead | Tech Lead | Before mitigation 3, or if the budget passes 75 % |
| Worker relay | Yeferson | If `ShowtimePublished` does not reach RabbitMQ after recovery |
| Infrastructure owner | owner of `csp-infra-mongo` | MongoDB host, disk or network |

Channels: the team group chat as war room and a GitHub issue in `csp-catalog-api` as the record.

## Verify recovery

- The error ratio of step 1 stays below 0.005 for 15 minutes.
- `/health/ready` answers 200 and the P95 of step 2 is back under 0.5 s.
- A test `POST /showtimes` in `qa` returns `201`, and the same `trace_id` appears in the `outbox_event`.
- The alert resolves; close the GitHub issue and write the post-mortem if the budget passed 50 %.
