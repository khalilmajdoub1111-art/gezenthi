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

`{type}` = `hotels`, `trips`, `omra`, `flights`, `services`. Errors are JSON problem details;
validation errors include `errors: { field: message }`.

> ⚠️ `/api/admin/**` is **not authenticated yet** — local development only. Spring Security
> with real admin accounts is the next mandatory step before any deployment.

## Database

- Schema: `backend/src/main/resources/db/migration` (Flyway, `V2__catalog_schema.sql`).
- Demo data: `db/dev-seed` — loaded only by the `dev` profile (default locally), never in production.
- Tests (`./mvnw test`) run against the local DB inside rolled-back transactions.

## Conventions

- **Secrets** never go in Git: env file / environment variables only.
- **Database changes** only through new Flyway migrations (`backend/src/main/resources/db/migration`).
- One feature = one commit with a clear message.
