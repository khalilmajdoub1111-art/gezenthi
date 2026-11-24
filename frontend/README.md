# Gezenthi Travel Agency — Frontend

Angular 22 + Tailwind CSS 4. Data comes from the Spring Boot API in `../backend`.

## Run locally

```bash
npm install
npm start        # http://localhost:4200 — proxies /api to http://localhost:8080 (proxy.conf.json)
npm run build    # production build in dist/
```

The backend must be running for offers to appear (`cd ../backend && ./mvnw spring-boot:run`).

## Admin panel (`/admin`)

Manage hotels (Tunisia & world), organised trips, Omra programmes, flights, and Visa / CTN / TRE services:
add, edit, delete, publish / hide, upload photos. Changes are saved in PostgreSQL through the API and
appear on the public site on next load.

- Offer types are declared once in `src/app/admin/admin-entities.ts` (fields, columns, defaults);
  one generic list page and one generic form page serve all of them.
- The form validates client-side; the server validates again and its field errors are shown under each field.
- **Login** at `/admin/login` (session cookie set by the API). `adminGuard` checks the session with
  `/api/auth/me`; an expired session on any admin call sends you back to the login page.

## Project structure

```
src/app/
  core/
    models/       Typed domain models = API contract (keep in sync with backend DTOs)
    data/         Navigation config + static marketing content (hero, testimonials…)
    services/     CatalogService  — public site, GET /api/public/catalog (published only)
                  CatalogStore    — admin CRUD, /api/admin/{type}
                  ApiError        — normalises server problem-details errors
                  AuthService     — login/logout/me, adminGuard, session-expired interceptor
  admin/          Admin panel: login, shell, dashboard, generic list + form, entity configs
  layout/         Public layout, Header (services bar + mega-menus + mobile drawer), Footer
  pages/home/     Landing page + hero search widget
  shared/         Reusable UI: icon set (pure SVG paths), hotel card, scroll-reveal directive
src/styles.css    Brand design tokens (blue #156AB3, gold #FFC523), fonts, animations
public/logo.png   Brand logo (transparent background)
```

## Before going live

- HTTPS + `secure` session cookie on the server; "change password" / manage admins screens.
- Photos uploaded from the admin are stored as data URLs in the DB for now → move to real file storage.
- **No customer data is collected yet.** Search and newsletter forms are intercepted client-side.
- Demo offers are fictional (backend `db/dev-seed`); the email in `AGENCY_CONTACT` is a placeholder.
- Photos are hot-linked from Unsplash for the demo; replace with the agency's own / licensed images.
