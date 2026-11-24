# Gezenthi Travel Agency — Web

Front-end prototype of the Gezenthi Travel Agency website (Angular 22 + Tailwind CSS 4).
Backend (Spring Boot) will be added in a later phase.

## Run locally

```bash
npm install
npm start        # http://localhost:4200
npm run build    # production build in dist/
```

## Admin panel (`/admin`)

Manage hotels (Tunisia & world), organised trips, Omra programmes, flights, and Visa / CTN / TRE services:
add, edit, delete, publish / hide, upload photos. Changes appear on the public site immediately.

- Offer types are declared once in `src/app/admin/admin-entities.ts` (fields, columns, defaults);
  one generic list page and one generic form page serve all of them.
- **Prototype storage:** data is saved in the browser's localStorage (`CatalogStore`), so each browser
  has its own copy. "Réinitialiser les données de démo" on the dashboard restores the seed content.
- **Demo login only — not secure.** Real authentication (Spring Security) is required before going live.

## Project structure

```
src/app/
  core/
    models/       Typed domain models (Hotel, Destination, NavItem…) = future API contract
    data/         Navigation config + MOCK seed catalogue (prototype only)
    services/     CatalogStore    — admin-managed data (localStorage today, Spring Boot API later)
                  CatalogService  — read-only, published-only view used by the public site
                  DemoAuthService — demo admin gate (NOT security)
  admin/          Admin panel: login, shell, dashboard, generic list + form, entity configs
  layout/         Public layout, Header (services bar + mega-menus + mobile drawer), Footer
  pages/home/     Landing page + hero search widget
  shared/         Reusable UI: icon set (pure SVG paths), hotel card, scroll-reveal directive
src/styles.css    Brand design tokens (blue #156AB3, gold #FFC523), fonts, animations
public/logo.png   Brand logo (transparent background)
```

## Prototype status — read before going live

- **No data is collected or sent.** Search and newsletter forms are intercepted client-side.
- **All hotels, prices, reviews and stats are fictional placeholders** (`core/data/mock-catalog.ts`).
- **Contact details** live in `AGENCY_CONTACT` (`core/data/navigation.ts`); the email is still a placeholder.
- Photos are hot-linked from Unsplash for the demo; replace with the agency's own / licensed images.
