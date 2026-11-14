# Git Commit Log - CineSync Platform

## Weeks 11 to 15

Period: 2026-10-11 to 2026-11-14. Author: Karen Caicedo (`karencaicedo1907`). Only commits authored by me in the `csp-*` repositories of the organization are listed. The same change promoted across several branches (develop, qa, release, main) is listed once, with the date and hash of its first commit; automatic merge commits are omitted. Newest first. The list was taken on 2026-11-14.

### Summary

| Repository | Commits |
|---|---|
| csp-api-gateway | 4 |
| csp-booking-portal | 1 |
| csp-catalog-api | 42 |
| csp-catalog-db | 16 |
| csp-catalog-portal | 27 |
| csp-concessions-api | 5 |
| csp-concessions-db | 1 |
| csp-concessions-portal | 12 |
| csp-docs | 38 |
| csp-infra-mongo | 5 |
| csp-infra-postgres | 4 |
| csp-ticketing-db | 1 |
| csp-worker | 3 |
| **Total** | **159** |

### csp-api-gateway

`code-corhuila/csp-api-gateway` - 4 commits

| Commit | Date | Message |
|---|---|---|
| `64f65de` | 2026-11-08 | feat(gateway): route get /admin/reservations to csp-booking-api |
| `40f931c` | 2026-11-04 | feat(gateway): route the concessions service (HU-CONCESSIONS-006) |
| `34ad590` | 2026-11-02 | feat(gateway): route the catalog administration with the treatment of the protected routes |
| `3254725` | 2026-10-22 | feat(gateway): route the public catalog reads to csp-catalog-api |

### csp-booking-portal

`code-corhuila/csp-booking-portal` - 1 commits

| Commit | Date | Message |
|---|---|---|
| `45e95d1` | 2026-11-08 | feat(booking): list the reservations of every user in the admin view |

### csp-catalog-api

`code-corhuila/csp-catalog-api` - 42 commits

| Commit | Date | Message |
|---|---|---|
| `2c3b844` | 2026-11-11 | feat(catalog-api): accept and return the trailer link of a movie |
| `365ad55` | 2026-11-06 | feat(catalog-api): serve https only with the certificate of the platform |
| `37c0941` | 2026-11-06 | fix(catalog-api): mount the tls files in the compose file |
| `c062e4a` | 2026-11-02 | feat(catalog-api): require iss and aud of a service credential on the availability route |
| `4ed59cb` | 2026-10-29 | feat(catalog-api): audit the calls rejected on the availability route |
| `81c9afa` | 2026-10-27 | chore(catalog-api): add the container healthcheck of the catalog service |
| `4167b35` | 2026-10-25 | feat(catalog-api): serve the health and readiness routes of the contract |
| `7d44985` | 2026-10-24 | fix(catalog-api): tolerate 30 seconds of clock skew on the token expiration |
| `0b512bc` | 2026-10-23 | chore(catalog-api): add the development seed script |
| `5f591a3` | 2026-10-23 | chore(catalog-api): describe the delivered service in the readme |
| `6650b4a` | 2026-10-23 | feat(catalog-api): add the seat layout to the availability |
| `6e9982e` | 2026-10-23 | chore(catalog-api): run the tests in ci as catalog_app against the real structure |
| `7013116` | 2026-10-23 | chore(catalog-api): remove the development seed script |
| `a717d57` | 2026-10-23 | chore(catalog-api): pass the database, key and subjects variables from the compose file |
| `17ad013` | 2026-10-22 | feat(catalog): read the movie search from MongoDB with the public rules |
| `1b1497a` | 2026-10-22 | docs(changelog): add HU-CATALOG-006 to the 3.0.0 release |
| `3059a47` | 2026-10-22 | docs(changelog): add HU-CATALOG-002 to the 3.0.0 release |
| `3b3ad84` | 2026-10-22 | feat(catalog): add the movie search use case and its ports |
| `458cfeb` | 2026-10-22 | feat(catalog): add the public movie search route and validate it against the contract |
| `99e166e` | 2026-10-22 | docs(changelog): add HU-CATALOG-004 to the 3.0.0 release |
| `b8d90ce` | 2026-10-22 | docs(changelog): add HU-CATALOG-005 to the 3.0.0 release |
| `f79904d` | 2026-10-22 | docs(changelog): add HU-CATALOG-003 to the 3.0.0 release |
| `0a1f32f` | 2026-10-21 | feat(catalog): add the showtime aggregate with its time interval, overlap rule and lifecycle |
| `0c6c269` | 2026-10-21 | feat(catalog): read the billboard from MongoDB with the public rules |
| `1af03ec` | 2026-10-21 | feat(catalog): add the room ports and use cases with idempotent writes |
| `1bf9a23` | 2026-10-21 | feat(catalog): add the room routes and validate them against the contract |
| `26e021a` | 2026-10-21 | feat(catalog): add the room aggregate with its seats and invariants |
| `2d9f488` | 2026-10-21 | feat(catalog): add the MongoDB room repository with transactional idempotency |
| `36fc52c` | 2026-10-21 | feat(catalog): add the showtime routes and validate them against the contract |
| `3be8599` | 2026-10-21 | feat(catalog): add the availability route with the service credential |
| `4485226` | 2026-10-21 | feat(catalog): add the showtime availability use case |
| `4dd435f` | 2026-10-21 | feat(catalog): persist the showtime with the room lock, the overlap check and the outbox event |
| `5257f8c` | 2026-10-21 | feat(catalog): add the billboard use case and its ports |
| `5510763` | 2026-10-21 | feat(catalog): add the showtime use cases and their ports |
| `65cd351` | 2026-10-21 | feat(catalog): add the public billboard route and validate it against the contract |
| `791f48d` | 2026-10-21 | docs(changelog): record the 3.0.0 release |
| `12d0ff8` | 2026-10-20 | feat(catalog): add the MongoDB movie repository with transactional idempotency |
| `3f2b054` | 2026-10-20 | feat(catalog): add the movie aggregate with its invariants and lifecycle |
| `4fbbe10` | 2026-10-20 | feat(catalog): add the movie routes and validate them against the contract |
| `5f71141` | 2026-10-20 | feat(catalog): authenticate the bearer token and check the catalog:write permission |
| `7100957` | 2026-10-20 | feat(catalog): add the movie ports and use cases with idempotent writes |
| `9afe224` | 2026-10-20 | feat(catalog): add the error envelope, the correlation filter and the exception handler |

### csp-catalog-db

`code-corhuila/csp-catalog-db` - 16 commits

| Commit | Date | Message |
|---|---|---|
| `56bd08f` | 2026-11-11 | feat(catalog-db): add the trailer link to the movie validator |
| `0617c2e` | 2026-10-23 | chore(catalog-db): create catalog_writer and grant it to catalog_app |
| `37a4c07` | 2026-10-23 | chore(catalog-db): run the ci as the real logins and fix the executor versions |
| `98c5aeb` | 2026-10-23 | docs(changelog): add the catalog_writer role to the 3.0.0 release |
| `0406ecd` | 2026-10-22 | docs(changelog): add HU-CATALOG-004 to the 3.0.0 release |
| `12afd44` | 2026-10-22 | docs(changelog): add HU-CATALOG-003 to the 3.0.0 release |
| `3de80e8` | 2026-10-22 | docs(changelog): add HU-CATALOG-006 to the 3.0.0 release |
| `680a5a5` | 2026-10-22 | feat(catalog): add the indexes that answer the movie search |
| `70d7c90` | 2026-10-22 | docs(changelog): add HU-CATALOG-002 to the 3.0.0 release |
| `093d74b` | 2026-10-21 | feat(catalog): add the showtimes and outbox event collections and the room schedule version |
| `12a2417` | 2026-10-21 | docs(changelog): record the 3.0.0 release |
| `8562e0d` | 2026-10-21 | feat(catalog): add the rooms collection and the room operations of the idempotency key |
| `8a16ce5` | 2026-10-21 | feat(catalog): create the catalog_outbox_reader role and grant it to worker_app |
| `a628cf6` | 2026-10-21 | feat(catalog): add the index that answers the billboard query |
| `059395a` | 2026-10-20 | feat(catalog): add the idempotency_key collection and the cast field of movies |
| `2ab71fb` | 2026-10-20 | feat(catalog): add the movies collection with its validator and index |

### csp-catalog-portal

`code-corhuila/csp-catalog-portal` - 27 commits

| Commit | Date | Message |
|---|---|---|
| `715af24` | 2026-11-11 | feat(catalog-portal): set and open the trailer link of a movie |
| `373081c` | 2026-11-08 | docs(readme): name the qa-promote prefix in the branching diagram |
| `1c0ebf5` | 2026-11-04 | chore(catalog-portal): remove the synthetic seat map that nothing links to (HU-FE-CATALOG-002) |
| `00b0a02` | 2026-11-02 | feat(catalog-portal): add the typed service for the catalog administration routes |
| `0ba1d54` | 2026-11-02 | feat(catalog-portal): move the administration billboard screen to the catalog service |
| `1d13479` | 2026-11-02 | feat(catalog-portal): read and write the movies of the administration through the catalog service |
| `25ac8dc` | 2026-11-02 | fix(catalog-portal): clear the room overlap alert when the form or the billboard changes |
| `48a35b7` | 2026-11-02 | fix(scaffold): save tsconfig.federation.json without a byte order mark |
| `5b89e90` | 2026-11-02 | feat(catalog-portal): read and write the rooms of the administration through the catalog service |
| `6ed6e03` | 2026-11-02 | feat(catalog-portal): add the state of the administration showtimes over the catalog service |
| `a3f5ab7` | 2026-11-02 | fix(deploy): document why only the federation assets send the cors header |
| `0ba1d5a` | 2026-10-29 | fix(deploy): use the IPv4 loopback in the container healthcheck |
| `ef1570f` | 2026-10-23 | docs(changelog): record the 3.0.0 release |
| `0b5fa91` | 2026-10-22 | feat(catalog): read the billboard page from the real catalog api |
| `7cc24ab` | 2026-10-22 | feat(catalog): read the movie detail from the real catalog api |
| `7f173e6` | 2026-10-22 | feat(catalog): add the state of the billboard search |
| `90895f7` | 2026-10-22 | feat(catalog): add the typed client of the public catalog reads |
| `3fa070b` | 2026-10-13 | fix(catalog): lock the CORS anchoring in CI and docs #29 |
| `44ed648` | 2026-10-13 | fix(catalog): publish the container as 4203:80 on the csp-frontend network #6 |
| `50c5b86` | 2026-10-13 | docs(qa): merge PR #63 - close release 2.0.1 in the changelog |
| `842dcde` | 2026-10-13 | fix(catalog): answer CORS from CORS_ALLOWED_ORIGIN_REGEX as the other portals do #29 |
| `8fe71d6` | 2026-10-13 | fix(catalog): enforce the CORS anchoring in the entrypoint and CI #29 |
| `b0f66ed` | 2026-10-13 | docs(catalog): close release 2.0.1 in the changelog #6 |
| `bef8374` | 2026-10-13 | fix(qa): merge PR #61 - promote the compose 4203:80 and CORS ADR-026 fixes to qa |
| `6df232c` | 2026-10-11 | chore(qa): merge PR #15 - move the portal dev port to 4203 |
| `76fcee9` | 2026-10-11 | docs(catalog): add the changelog for release 2.0.0 #6 |
| `b97ffca` | 2026-10-11 | docs(qa): merge PR #58 - add the changelog for release 2.0.0 |

### csp-concessions-api

`code-corhuila/csp-concessions-api` - 5 commits

| Commit | Date | Message |
|---|---|---|
| `15ff87b` | 2026-11-07 | feat(concessions): add the internal operations of the workflow to place and cancel an order |
| `24a34fa` | 2026-11-07 | fix(deploy): read the database login of the service from its own variables |
| `3274780` | 2026-11-07 | chore(deploy): pass the subject lists of the service tokens to the container |
| `3e9b403` | 2026-11-04 | fix(concessions): register the users of the race tests in a thread-safe list |
| `83593e9` | 2026-11-04 | test(concessions): keep an order valid when its product is deactivated (HU-CONCESSIONS-006) |

### csp-concessions-db

`code-corhuila/csp-concessions-db` - 1 commits

| Commit | Date | Message |
|---|---|---|
| `2de3560` | 2026-11-07 | fix(deploy): retry the connection of the migration executor while the instance starts |

### csp-concessions-portal

`code-corhuila/csp-concessions-portal` - 12 commits

| Commit | Date | Message |
|---|---|---|
| `470be8e` | 2026-11-08 | fix(deploy): require an anchored cors origin regex and narrow the json location |
| `4fc1f5d` | 2026-11-08 | chore(deploy): restore tsconfig.federation.json rewritten by the federation build |
| `5a65b74` | 2026-11-08 | docs(readme): name the qa-promote prefix in the branching diagram |
| `bcabfb6` | 2026-11-08 | fix(deploy): keep the hardening headers on the health endpoint |
| `0c20f76` | 2026-11-06 | fix(deploy): call the container healthcheck on 127.0.0.1 |
| `1d73694` | 2026-11-06 | docs(changelog): state the combos and inventory stories in the 3.0.0 entry |
| `2873929` | 2026-11-06 | feat(concessions): list and create combos through the API (HU-CONCESSIONS-002) |
| `400a402` | 2026-11-06 | feat(concessions): adjust stock and list movements through the API (HU-CONCESSIONS-003) |
| `d2745fe` | 2026-11-05 | docs(changelog): record the 3.0.0 release |
| `0731f5c` | 2026-11-04 | feat(concessions): list, create and change the status of products through the API (HU-CONCESSIONS-006) |
| `1ce63d8` | 2026-11-04 | feat(concessions): send the snack order to the API with the reservation of the address (HU-CONCESSIONS-004) |
| `23b3653` | 2026-11-04 | feat(concessions): list the published products and combos of the API in the snack selection (HU-CONCESSIONS-004) |

### csp-docs

`code-corhuila/csp-docs` - 38 commits

| Commit | Date | Message |
|---|---|---|
| `0c12c9e` | 2026-11-11 | docs(catalog): add the optional trailer link of a movie (dec-015) |
| `b3c3bb4` | 2026-11-09 | docs(security): correct rows t and d of the booking-to-catalog threat model after the booking review |
| `f359ee1` | 2026-11-09 | docs(security): state that stories 287 and 288 are already in qa |
| `bb5458f` | 2026-11-08 | docs(security): record the tls control of the transport threat of the booking-to-catalog boundary |
| `2519e18` | 2026-11-07 | docs(concessions): propose adr-069 and contract 2.6.0 for the workflow operations |
| `47771d9` | 2026-11-07 | docs(adr): accept adr-069 and correct its notes on contract tests and the compose |
| `80ee83f` | 2026-11-07 | docs(adr): adopt the paged relay discovery in ADR-019 |
| `dfb29e8` | 2026-11-07 | docs(adr): record that the catalog relay waits for a consumer |
| `25f3960` | 2026-11-06 | docs(adr): propose tls between booking and catalog |
| `4b9a0fd` | 2026-11-06 | docs(concessions): record the portal admin screens in the trails of hu-002, hu-003 and hu-007 |
| `939f5b6` | 2026-11-06 | docs(adr): state the startup, health check and declared deviation of adr-067 |
| `edc4222` | 2026-11-06 | docs(catalog): align the documents with the https delivery of adr-067 |
| `31b8c1f` | 2026-11-05 | docs(ux): record the reservation id that Booking passes to the snack selection |
| `bdd0071` | 2026-11-05 | docs(stories): mark HU-CONCESSIONS-006 as done in qa |
| `3a78a82` | 2026-11-03 | docs(catalog): describe the 401 of adr-056 in the availability contract and the runbook |
| `6769caf` | 2026-11-03 | docs(catalog): record dec-013 and dec-014 and amend dec-008 with adr-056 |
| `d328d03` | 2026-11-03 | docs(security): record the mitigations of s and r of the booking-to-catalog threat model |
| `6bca74c` | 2026-11-02 | docs(catalog): record that the administration screens are covered by a technical task of the portal |
| `6398403` | 2026-10-29 | docs(security): STRIDE context and first threats for booking-to-catalog availability |
| `705cd9b` | 2026-10-29 | docs(security): address review on STRIDE for booking-to-catalog availability |
| `a92cf35` | 2026-10-29 | docs(security): complete STRIDE and risk ranking for booking-to-catalog availability |
| `98b6f48` | 2026-10-25 | docs(catalog): define the liveness and readiness routes of the service |
| `d7269dd` | 2026-10-24 | docs(catalog): mark TD-042 and TD-043 as done in develop |
| `04b71ff` | 2026-10-23 | docs(catalog): mark HU-FE-CATALOG-002 as done in qa with its delivery trail |
| `55efad1` | 2026-10-23 | docs(catalog): record ADR-049 and DEC-011, the roles of the catalog in MongoDB |
| `a4374fe` | 2026-10-23 | docs(catalog): add the seat layout to the availability response |
| `25cfbbe` | 2026-10-22 | docs(catalog): mark hu-catalog-001 to hu-catalog-006 done in qa and record their delivery trail |
| `3fe7279` | 2026-10-22 | docs(catalog): record the movie search rules and align the catalog contract |
| `c6060a1` | 2026-10-22 | docs(catalog): add hu-fe-catalog-002 so the catalog portal reads the real catalog api |
| `12545b4` | 2026-10-21 | docs(catalog): correct the statement that the executor cannot create the outbox reader role |
| `68936f0` | 2026-10-21 | docs(catalog): record the showtime rules and the showtime published event and align the showtime contract |
| `bd9d72e` | 2026-10-21 | docs(catalog): record the billboard rules and the public route and align the billboard contract |
| `c6d11c2` | 2026-10-21 | docs(catalog): record the room rules and the idempotent room writes and align the room contract |
| `fa2da28` | 2026-10-21 | docs(catalog): record the availability rules and the service credential and align the availability contract |
| `fde687e` | 2026-10-21 | docs(catalog): renumber the billboard adr to adr-041 |
| `75c2ca8` | 2026-10-20 | docs(catalog): record the movie states and idempotency storage and align the movie contract |
| `81f645b` | 2026-10-20 | docs(catalog): state the limit of cast in the data dictionary |
| `c0f9914` | 2026-10-20 | docs(catalog): require catalog:write on the movie reads and state the cast limit |

### csp-infra-mongo

`code-corhuila/csp-infra-mongo` - 5 commits

| Commit | Date | Message |
|---|---|---|
| `6c336dc` | 2026-10-23 | chore(infra): add the catalog migrator and create the api login with no role |
| `8c1b38a` | 2026-10-23 | chore(infra): migrate with the real executor in the ci |
| `0da785b` | 2026-10-22 | docs(infra): describe only what exists in the readme, init, compose and up script |
| `1bc5d17` | 2026-10-22 | docs(infra): add the changelog and the backup and restore procedure of the instance |
| `2fe6242` | 2026-10-22 | docs(changelog): record the 3.0.0 release |

### csp-infra-postgres

`code-corhuila/csp-infra-postgres` - 4 commits

| Commit | Date | Message |
|---|---|---|
| `31fcf67` | 2026-11-07 | feat(infra): compose concessions in the platform |
| `92152f2` | 2026-11-07 | chore(env): list the concessions variables of the worker in the env examples |
| `4c41334` | 2026-10-23 | chore(env): follow the catalog migrator in the platform env files |
| `9f34212` | 2026-10-23 | chore(infra): compose the catalog executor and API in the platform |

### csp-ticketing-db

`code-corhuila/csp-ticketing-db` - 1 commits

| Commit | Date | Message |
|---|---|---|
| `bde6f99` | 2026-11-08 | fix(db): grant the outbox reader role to the worker login |

### csp-worker

`code-corhuila/csp-worker` - 3 commits

| Commit | Date | Message |
|---|---|---|
| `0f8320b` | 2026-11-09 | docs(readme): name the migration that grants the ticketing outbox reader to the worker |
| `35eb5e1` | 2026-11-08 | chore(deploy): retry the connection of the migration executor while the instance starts |
| `2ae0649` | 2026-11-07 | fix(worker): compile with parameters so each relay binds its own data source |

