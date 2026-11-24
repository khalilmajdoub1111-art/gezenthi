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

## Conventions

- **Secrets** never go in Git: env file / environment variables only.
- **Database changes** only through new Flyway migrations (`backend/src/main/resources/db/migration`).
- One feature = one commit with a clear message.
