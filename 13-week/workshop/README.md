# W13 Workshop - Threat modeling and the security backlog

<!-- CONFIG-START -->
- FULL_NAME: Karen Johana Caicedo Arias
- GITHUB_USER: karencaicedo1907
- TEAM: CineSync Platform
- DOCS_REPO: https://github.com/code-corhuila/csp-docs
- BOUNDARY: booking-service to catalog-service (GET /api/v1/catalog/showtimes/{showtimeId}/availability)
- CLAIM_ISSUE: https://github.com/code-corhuila/csp-docs/issues/276
- REFLECTION_DOC: https://docs.google.com/document/d/1vamzHa7fk9dxduzl3-g55HQ4cscvzR68_6XMvWDedxg/edit?usp=sharing
- LIBRARY_BOOK: Antonio Angel Ramos Varon, Hacking y Seguridad de Paginas Web, edition not stated in the library copy, Ra-Ma, 2014, ISBN 9788499642956, call number 0011078
- LIBRARY_RECORD_TIME: 2026-10-28 19:05
- STORIES: https://github.com/code-corhuila/csp-docs/issues/287 https://github.com/code-corhuila/csp-docs/issues/288 https://github.com/code-corhuila/csp-docs/issues/289
<!-- CONFIG-END -->

## CP7 - Refined security backlog

- Story #287 now names its dependency on csp-auth (it must issue `iss` and `aud` first) and is related to https://github.com/code-corhuila/csp-docs/issues/285 and https://github.com/code-corhuila/csp-docs/issues/291.
- Stories #288 and #289 were checked against the issues of the other boundaries; no overlap found, so they stay as written.
- Every story has its STRIDE letter, risk value, `security` and priority labels, the link to PR #286 and the Backlog column of the team board.
