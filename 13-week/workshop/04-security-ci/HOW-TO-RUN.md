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

Run on 2026-10-30 in a copy of `csp-catalog-api` at commit `1d0948c` (branch `develop`, Java 21, Maven 3.9.9), with the test file copied as step 1 says. The build failed, as expected:

```text
[INFO] Running co.edu.corhuila.csp.catalog.adapter.in.http.AvailabilityAuthorizationNegativeTest
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 1.384 s <<< FAILURE!
[ERROR] AvailabilityAuthorizationNegativeTest.aBookingServiceTokenForAnotherAudienceIsUnauthorizedOnAvailability -- <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <401> but was: <200>
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

Reading:

- `aUserTokenWithShowtimeWriteIsForbiddenOnAvailability` passed: a user token gets 403.
- `aBookingServiceTokenForAnotherAudienceIsUnauthorizedOnAvailability` failed with 200 instead of 401: today a token with `sub=booking-service` and `aud=payments-service` still reads availability. This confirms the gap of story #287 (the service does not require `aud`), and the test turns green when the story is done.

## Validation of `security.yml`

- Syntax: loaded with a YAML parser, valid; jobs `secret-scan`, `dependency-audit` and `authz-negative-test`, trigger `pull_request` on `develop`, `qa` and `main`.
- `actionlint` (online playground, https://rhysd.github.io/actionlint/, v1.7.12) with the full content of `security.yml`: "Yay! No error was detected."
- The workflow itself was not executed on GitHub Actions; it lives in the fork folder as a proposal.
