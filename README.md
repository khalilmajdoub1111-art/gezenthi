# Gezenthi Travel Agency

Website and back-office of Gezenthi Travel Agency (Nabeul).

| Folder | Stack | Docs |
|---|---|---|
| `frontend/` | Angular 22 + Tailwind CSS 4 | [frontend/README.md](frontend/README.md) |
| `backend/` | Spring Boot 4.2 · Java 25 · PostgreSQL 18 · Flyway | see below |

## Prerequisites

- Node.js LTS, JDK 25 (Temurin), PostgreSQL 18, IntelliJ IDEA / VS Code
- Local DB credentials in `%USERPROFILE%\.gezenthi\db.env` (outside the repo, never committed):

```properties
DB_URL=jdbc:postgresql://localhost:5432/gezenthi
DB_USERNAME=gezenthi
DB_PASSWORD=<generated locally>
```

## Run

```bash
cd frontend && npm start                 # http://localhost:4200
cd backend && ./mvnw spring-boot:run     # http://localhost:8080/actuator/health
```

Start the backend first; the Angular dev server proxies `/api` to `localhost:8080` (`frontend/proxy.conf.json`).

## API

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/public/catalog` | Public site: all **published** offers in one call |
| GET / POST | `/api/admin/{type}` | Admin: list all / create |
| GET / PUT / DELETE | `/api/admin/{type}/{id}` | Admin: read / update / delete |

| GET | `/api/auth/csrf` | Sets the `XSRF-TOKEN` cookie |
| POST | `/api/auth/login` · `/api/auth/logout` | Admin session (JSON) |
| GET | `/api/auth/me` | Current admin, or 401 |

`{type}` = `hotels`, `trips`, `omra`, `flights`, `services`. Errors are JSON problem details;
validation errors include `errors: { field: message }`.

| GET | `/api/public/hotels/search?city&checkIn&checkOut&adults&children` | Unified hotel search (own + suppliers) |
| GET · POST | `/api/admin/suppliers` · `/{code}/test` · `/{code}/sync` | Supplier status, connection test, static sync |

Hotel suppliers (TunisiaBeds / OS-TRAVEL API): see **[docs/SUPPLIERS.md](docs/SUPPLIERS.md)**.

## Security

- `/api/admin/**` requires an authenticated **ADMIN** (Spring Security); everything else is denied by default
  except the public catalogue, auth endpoints and `/actuator/health`.
- **Session cookie** (HttpOnly, SameSite=Lax, 2 h idle timeout; set `server.servlet.session.cookie.secure=true`
  behind HTTPS). No token is stored in JavaScript. New session id on login.
- **CSRF** on every write: `XSRF-TOKEN` cookie → `X-XSRF-TOKEN` header (automatic in Angular).
- **Passwords** stored only as bcrypt hashes (`admin_user` table). Same error for wrong password and unknown email.
- **Brute force:** 5 failed logins on an account → locked 15 minutes.
- **Audit log** (`tn.gezenthi.audit` logger): `LOGIN_OK`, `LOGIN_FAILED`, `LOGIN_LOCKED`, `LOGOUT` with IP.
- **First admin** is created at startup from `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD`
  (local: `%USERPROFILE%\.gezenthi\db.env`; never committed). There is no default password.

## Database

- Schema: `backend/src/main/resources/db/migration` (Flyway, `V2__catalog_schema.sql`).
- Demo data: `db/dev-seed` — loaded only by the `dev` profile (default locally), never in production.
- Tests (`./mvnw test`) run against the local DB inside rolled-back transactions.

## Before going live (deployment checklist)

- **HTTPS** everywhere; `server.servlet.session.cookie.secure=true`; HSTS header.
- **Content-Security-Policy** header (scripts from self, images from self + supplier CDNs, frames: openstreetmap.org).
- **Real client IP** behind a reverse proxy (`server.forward-headers-strategy=native` + trusted proxy),
  otherwise rate limits apply to the proxy's IP for everyone.
- Profile `prod` (no `db/dev-seed`, no `only-hotels`), secrets as environment variables.
- Admin lockout is per account (5 failures → 15 min): consider adding IP-based limits too.
- Uploaded admin photos are stored in the DB (data URLs): move to file/object storage.
- Database backups + log retention policy; personal data retention for bookings.

## Conventions

- **Secrets** never go in Git: env file / environment variables only.
- **Database changes** only through new Flyway migrations (`backend/src/main/resources/db/migration`).
- One feature = one commit with a clear message.
