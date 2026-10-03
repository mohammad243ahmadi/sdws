# Implementation Plan

1. Move the Spring application under `backend/` without changing its entities, service rules, or H2 data path; establish a passing backend test/build command.
2. Add the REST API boundary: DTOs, structured errors, session login/logout/current-user, CSRF, and API integration tests.
3. Expose wallet, money-request, top-up, bill-payment, profile, and admin operations through role-protected JSON endpoints; add focused API tests.
4. Create `frontend/` with Next.js, a same-origin `/api` rewrite, shared session-aware API helpers, and accessible app navigation.
5. Migrate authentication, user wallet flows, and admin flows into Next.js pages; remove Spring view-rendering dependencies/templates once replacement is verified.
6. Run backend tests, frontend lint/build, and runtime integration checks; document the two-app startup workflow.

## Risks and Mitigations

- Session and CSRF behavior through a development proxy: verify with API integration and browser requests before migrating all screens.
- Moving Maven's working directory can change relative H2 paths: keep the documented launch from repository root and verify it opens the existing database.
- Frontend/backend ports and origin behavior: proxy `/api` through Next.js instead of relying on browser CORS configuration.
- Feature parity is broad: migrate one vertical workflow at a time and keep the existing Java service as the behavior baseline.