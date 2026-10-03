# Wallet Frontend and API Split

## Objective
Separate the current wallet application into `frontend/` (Next.js and React) and `backend/` (Spring Boot REST API) while preserving registration, login, wallet operations, money requests, history, profile, and admin workflows. Keep the existing Java service and database schema as the source of wallet business rules.

## Capability Map

| Module | Responsibility | Depends on |
|---|---|---|
| `identity-api` | Registration, session login/logout, current user, profile, role checks | Existing user service and database |
| `wallet-api` | Balance, transactions, transfers, deposits, withdrawals, requests, top-ups, bill payments | `identity-api` |
| `admin-api` | Overview, user listing/status, transaction search | `identity-api`, `wallet-api` |
| `next-client` | User and admin screens, forms, session-aware API client | All API modules |

Build order: API contract and identity → wallet API → admin API → Next.js client → end-to-end verification.

## Tech Stack

- Backend: Java 17+, Spring Boot 3.3.4, Spring MVC, Spring Security, Spring Data JPA, H2 by default.
- Frontend: Next.js App Router, React, TypeScript.
- Browser requests use relative `/api/...` paths. Next.js rewrites proxy these to the backend so session cookies remain same-origin in development and deployment.
- Authentication uses short-lived signed JWTs in an `HttpOnly`, `SameSite=Strict` cookie scoped to `/api`; tokens are never stored in `localStorage` or `sessionStorage`.
- Spring Security is stateless and validates JWTs on every API request. The signing secret is required from the environment outside local development and is never committed.
- State-changing cookie-authenticated requests require a CSRF token. The Next.js rewrite keeps browser requests same-origin and forwards the auth cookie to Spring.

## API Contract

All API responses are JSON except `204 No Content`. Names are camelCase. Errors use `{ "code": "...", "message": "..." }` and appropriate HTTP status codes.

| Method and path | Access | Purpose |
|---|---|---|
| `GET /api/auth/csrf` | Public | Create/return CSRF token |
| `POST /api/auth/register` | Public | Register a user |
| `POST /api/auth/login` | Public | Validate credentials and set JWT cookie |
| `GET /api/auth/me` | Authenticated | Current user and role |
| `POST /api/auth/logout` | Authenticated | Clear JWT cookie |
| `GET /api/wallet` | User | Wallet owner, account number, balance |
| `GET /api/transactions?type=` | User | Current user's transaction history |
| `POST /api/transfers` | User | Send money by recipient phone |
| `POST /api/wallet/deposits` | User | Simulated add-money operation |
| `POST /api/wallet/withdrawals` | User | Withdraw from wallet |
| `GET /api/money-requests` | User | Sent and incoming requests |
| `POST /api/money-requests` | User | Create a request |
| `POST /api/money-requests/{id}/accept` | User | Accept and pay a request |
| `POST /api/money-requests/{id}/decline` | User | Decline a request |
| `POST /api/mobile-topups` | User | Record simulated mobile top-up |
| `POST /api/bill-payments` | User | Record simulated bill payment |
| `PUT /api/users/me` | User | Update profile/password |
| `GET /api/admin/overview` | Admin | Dashboard totals and latest transactions |
| `GET /api/admin/users` | Admin | List users |
| `PATCH /api/admin/users/{id}` | Admin | Change active status |
| `GET /api/admin/transactions?type=&phone=` | Admin | Search all transactions |

## Commands

- Backend tests: `mvn -f backend/pom.xml test`
- Backend dev server: `cd backend && APP_JWT_SECRET="$(openssl rand -base64 32)" mvn spring-boot:run`
- Frontend checks: `cd frontend && npm run lint && npm run typecheck && npm run build`
- Frontend dev server: `cd frontend && npm run dev`

## Project Structure

```text
backend/                 Spring Boot source, tests, and Maven build
frontend/                Next.js app, components, styles, and API client
data/                    Existing local H2 database; do not replace during migration
MIGRATION_SPEC.md         Shared scope and API contract
tasks/                   Implementation plan and task checklist
```

## Code Style

Use Java records for transport DTOs and keep JPA entities inside the backend. TypeScript API types mirror the JSON contract and are not inferred from database entities. Validate request DTOs at the API boundary; preserve existing service-level wallet invariants.

```java
public record WalletResponse(String ownerName, String accountNumber, BigDecimal balance) {}
```

## Testing Strategy

- Backend: JUnit and Spring Boot/MockMvc tests for API status, JSON shape, session/role access, CSRF, and wallet behavior.
- Frontend: ESLint, TypeScript check, and production build; exercise registration/login, wallet dashboard, forms, request handling, history, profile, and admin views against the running backend.
- Integration: run both applications and verify the Next.js same-origin `/api` proxy, cookie session, and representative user/admin flows.

## Boundaries

- Always preserve existing wallet service rules, stored balances, and database schema unless a discovered incompatibility requires an explicit migration plan.
- Always keep simulated add-money, top-up, and bill-payment behavior clearly simulated.
- Ask before adding real payment integrations, changing authentication to JWT, or changing the persistence schema.
- JWT signing key is configured via environment; local development may use a clearly documented non-production fallback only if needed to run locally.
- Set finite JWT expiry and cookie `HttpOnly`, `SameSite=Strict`, path `/api`, and `Secure` in production.
- Never expose password hashes or internal exception details through API responses.
- Never commit secrets or change/remove existing user data.

## Success Criteria

- Application source exists in distinct `backend/` and `frontend/` directories.
- Spring Boot exposes the documented JSON contract, with authentication, authorization, CSRF, and consistent errors.
- Next.js provides the current user and admin workflows using the API rather than server-rendered Spring templates.
- Backend tests and frontend lint/build pass; the app can be launched using the documented commands.