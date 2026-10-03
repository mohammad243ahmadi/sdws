# Secure Digital Wallet System

The project is split into two applications:

- `backend/`: Spring Boot 3 REST API, Spring Security, JPA, H2/MySQL.
- `frontend/`: Next.js App Router, React, TypeScript.

The backend keeps the wallet service and database. The browser talks to it through the Next.js `/api` rewrite, so auth cookies remain same-origin.

## Run Locally

Use two terminals from the repository root.

Terminal 1, backend:

```sh
cd backend
export APP_JWT_SECRET="$(openssl rand -base64 32)"
export SDWS_DB_PATH=/tmp/sdws-dev-db
mvn spring-boot:run
```

Terminal 2, frontend:

```sh
cd frontend
npm install
npm run dev
```

Open http://localhost:3000. The frontend proxies `/api/*` to `http://localhost:8080`. Override the destination with `BACKEND_API_URL` if needed.

Requirements: Java 17 or later, Maven, and Node.js 20.9 or later.

The default development administrator is phone `0700000000`, password `Admin@12345`. Change it before deployment. The backend defaults to `../data/sdws` when run from `backend/`. The workspace's existing `data/sdws.mv.db` currently fails H2 integrity checks; the local run command above uses a fresh database under `/tmp` and leaves that file untouched. Do not point the app back at the existing file until it has been recovered or replaced deliberately.

## API

All backend routes are JSON under `/api`. Authentication uses a short-lived JWT in an `HttpOnly`, `SameSite=Strict` cookie. Browser writes require the CSRF token from `GET /api/auth/csrf`.

- Auth: `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`
- Wallet: `/api/wallet`, `/api/transactions`, `/api/transfers`, `/api/wallet/deposits`, `/api/wallet/withdrawals`
- Requests/payments: `/api/money-requests`, `/api/mobile-topups`, `/api/bill-payments`
- Profile: `/api/users/me`
- Admin: `/api/admin/overview`, `/api/admin/users`, `/api/admin/transactions`

Top-ups, bill payments, and adding money are simulations. They do not contact operators, billers, or payment providers.

## Checks

```sh
mvn -f backend/pom.xml test
cd frontend && npm run lint && npm run typecheck && npm run build
```

For production, set a stable random `APP_JWT_SECRET` (at least 256 bits), enable `JWT_COOKIE_SECURE=true`, and serve both applications over HTTPS. Never use the development admin password or a generated-per-run signing secret in production.
