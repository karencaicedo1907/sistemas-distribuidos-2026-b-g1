# How to run the negative test (CP4)

Story covered: csp-docs#287 `[security] Reject service tokens that are not issued for catalog-service`
(STRIDE S, risk 6). Acceptance criterion 1: a valid RS256 token with `sub=booking-service` but a wrong
audience is rejected with 401 on `GET /showtimes/{showtimeId}/availability`.

## Run it locally

1. Copy `AvailabilityAuthorizationNegativeTest.java` to
   `csp-catalog-api/catalog-adapters/src/test/java/co/edu/corhuila/csp/catalog/adapter/in/http/`.
2. From the root of `csp-catalog-api`:

```bash
mvn -B -pl catalog-adapters -am test -Dtest=AvailabilityAuthorizationNegativeTest -Dsurefire.failIfNoSpecifiedTests=false
```

## What to expect

- `aUserTokenWithShowtimeWriteIsForbiddenOnAvailability` is a baseline: it should pass today (403).
- `aBookingServiceTokenForAnotherAudienceIsUnauthorizedOnAvailability` is expected to fail today, because
  the service does not require `iss` or `aud` (contract text of `serviceAuth`). It turns green when story #287 is done.

## Real console output

Pending (CP8): paste here the output of the command above and the result of validating `security.yml`.
