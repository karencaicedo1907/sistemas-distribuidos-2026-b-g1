# CP1 - Attack surface map

Boundary I own: **booking-service to catalog-service**, route `GET /api/v1/catalog/showtimes/{showtimeId}/availability` (claim issue: https://github.com/code-corhuila/csp-docs/issues/276).

## Data flow and trust boundaries

```mermaid
flowchart LR
    subgraph TB0 [TB0 - untrusted: internet]
        U[Web and mobile clients]
    end
    subgraph TB1 [TB1 - edge]
        GW[api-gateway NGINX :8000/8443]
    end
    subgraph internal [internal network]
        AUTH[auth-service :8081]
        CAT[catalog-service :8082]
        BOOK[booking-service :8083]
        CON[concessions-service :8085]
        TIC[ticketing-fulfillment-service :8084]
        WRK[csp-worker]
        MQ[[RabbitMQ]]
        ADB[(PostgreSQL auth + Redis)]
        CDB[(MongoDB catalog_db)]
        BDB[(PostgreSQL booking)]
    end
    SMTP[SMTP provider]

    U -->|HTTPS + JWT| GW
    GW -->|REST| AUTH
    GW -->|REST billboard only| CAT
    GW -->|REST| BOOK
    GW -->|REST| CON
    GW -->|REST| TIC
    BOOK ==>|"TB2: HTTPS + service token (sub=booking-service)"| CAT
    AUTH --- ADB
    CAT -->|TB3: roles| CDB
    BOOK --- BDB
    WRK -->|TB4: outbox read-only| CDB
    WRK -->|AMQP publish| MQ
    MQ -->|AMQP consume| TIC
    MQ -->|AMQP consume| CON
    TIC -->|SMTP/TLS| SMTP
    style CAT fill:#fde68a,stroke:#b45309
    style BOOK fill:#fde68a,stroke:#b45309
```

Trust boundaries: TB0/TB1 internet to api-gateway, TB2 booking-service to catalog-service (**mine**, highlighted), TB3 catalog-service to MongoDB `catalog_db`, TB4 csp-worker to the catalog outbox and RabbitMQ.

## Asset inventory

| Asset | Where it lives | Classification |
|---|---|---|
| Service token with `sub=booking-service` | Issued by auth-service, sent by booking-service to catalog-service | Restricted |
| Public key of auth-service (JWKS, RS256) | auth-service `/auth/jwks`, configured in catalog-api | Internal |
| Seat layout (`seatLayout`: label, row, number, type) | MongoDB `catalog_db`, answered by availability | Internal |
| Showtime data (`movieTitle`, `roomName`, `startsAt`) | MongoDB `catalog_db` | Internal |
| `SERVICE_SUBJECTS` list of allowed callers | Environment of catalog-api (`.env.example`) | Confidential |
| User identity and email | auth-service PostgreSQL schema `auth` | Confidential |

## Entry points of my boundary

| Entry point | Who calls it | How it authenticates today |
|---|---|---|
| `GET /api/v1/catalog/showtimes/{showtimeId}/availability` on catalog-service :8082 | booking-service, directly (the gateway does not route it, `csp-api-gateway/nginx/routes/catalog.conf`) | Bearer JWT RS256 verified by `JwtAuthenticationFilter`; `sub` must be in `SERVICE_SUBJECTS` (default `booking-service`, ADR-042); no token is 401, a user token is 403 |
| `GET /health` and `GET /health/ready` on catalog-service | Platform and probes | None (public by contract) |
