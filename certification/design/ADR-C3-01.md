# ADR-C3-01: `csp-auth` creates service tokens

## Status

Proposed

## Context

* Today, a person runs a command with a private key to create the service tokens (ADR-052, D2 and D3). These tokens last 90 days.
* The tokens are used by `csp-worker`, `booking-service` and `csp-workflow`.
* If the key changes, all tokens stop working at the same time.
* Auth does not create the tokens today because it needs to know which service is asking for a token. Q-007 (ADR-067) is still open because the way to get this credential in production is not clear.

## Decision

* Each service will have its own secret. This secret will be saved in DEW and will only be used to ask Auth for a token.
* The secret tells Auth which service is asking for the token.
* The secret will be sent to Auth using an encrypted connection, so other people cannot easily read it.
* Auth will give a token that lasts 1 hour. The service will ask for a new token before the old one expires.
* One hour is a good time because 5 minutes is too short and the service would need to ask for new tokens many times. One day is too long if someone steals a token.
* This is better than a token that lasts 90 days. Each service has its own secret, so it can be changed or cancelled without affecting the other services.
* If someone steals a token, it will only work for a short time.
* The secret is still a long-term secret. But the risk is lower because it is only used with Auth and each service has its own secret.

## Alternatives considered

* Rejected: create tokens by hand. A person still needs to run the command with the private key and the tokens last 90 days.
* Rejected: save 90-day tokens in DEW and change them from time to time. The tokens still last a long time and a person still needs to create them with the private key.
* Rejected: use Mutual TLS between services. This needs many changes in the system. Today, only Booking to Catalog uses TLS (ADR-067). This option is also part of Q-007.

## Consequences

* Positive: Nobody needs to create tokens by hand. If a token is stolen, it only works for a short time.
* Positive: Changing the keys will not stop all services at the same time because the tokens last only 1 hour and are renewed automatically.
* Negative: The services need Auth to get new tokens. If Auth is not working, they cannot renew their tokens. If Auth is down for a short time, the services can keep working with their current tokens until they expire.
* Negative: Auth needs a new endpoint to create the tokens. This endpoint and its rules must also be added to `auth-service.yaml`.
* This decision replaces D2 of ADR-052 and changes D3, because D3 says there is no endpoint and this decision adds one.
