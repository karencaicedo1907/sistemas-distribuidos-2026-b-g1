<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       13-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 13

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Karen Johana Caicedo Arias
- GITHUB_USER: karencaicedo1907
- TEAM: CineSync Platform
- SPRINT_GOAL: Threat-model the trust boundary booking-service to catalog-service (showtime availability) and turn it into three security stories and CI gates.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| W13-STRIDE | STRIDE analysis of booking-service to catalog-service | doing | PR https://github.com/code-corhuila/csp-docs/pull/286 (two pushes, review pending) |
| csp-docs#287 | [security] Reject service tokens that are not issued for catalog-service | todo | https://github.com/code-corhuila/csp-docs/issues/287 |
| csp-docs#288 | [security] Audit every rejected call to the availability route | todo | https://github.com/code-corhuila/csp-docs/issues/288 |
| csp-docs#289 | [security] Booking verifies the seat layout it receives from catalog | todo | https://github.com/code-corhuila/csp-docs/issues/289 |

## 2. My individual contribution
- Claimed the boundary in https://github.com/code-corhuila/csp-docs/issues/276 and mapped the attack surface in `13-week/workshop/01-attack-surface.md`.
- Wrote the STRIDE table (six rows, ranking and residual risk) in PR #286 and opened the three security issues with labels and priority.
- Proposed `security.yml` (gitleaks, OWASP dependency-check, negative authorization test) in `13-week/workshop/04-security-ci/`.
- Reviewed row S of a teammate: https://github.com/code-corhuila/csp-docs/pull/277#pullrequestreview-5691804173

## 3. Blockers and risks
- The negative test is written but not run yet; the one for story #287 is expected to fail until the story is implemented.
- The library book is cited, but the pages used and the `[B]` rows are still missing.

## 4. Plan for next week
- Answer the review of my PR, add the book pages, refine the three issues and run the negative test with its real output (CP6 to CP8).
- Write the implementation plan in the reflection document (CP9).

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [x] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [x] DDD / hexagonal boundaries respected (domain has no I/O)
- [x] No secrets; config via environment variables

## 6. Evidence links
- Workshop folder: https://github.com/karencaicedo1907/sistemas-distribuidos-2026-b-g1/tree/main/13-week/workshop
- Claim issue: https://github.com/code-corhuila/csp-docs/issues/276
- Reflection document: https://docs.google.com/document/d/1vamzHa7fk9dxduzl3-g55HQ4cscvzR68_6XMvWDedxg/edit?usp=sharing
