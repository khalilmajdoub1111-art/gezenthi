# Gezenthi Travel Agency — Web

Front-end prototype of the Gezenthi Travel Agency website (Angular 22 + Tailwind CSS 4).
Backend (Spring Boot) will be added in a later phase.

## Run locally

```bash
npm install
npm start        # http://localhost:4200
npm run build    # production build in dist/
```

## Project structure

```
src/app/
  core/
    models/       Typed domain models (Hotel, Destination, NavItem…) = future API contract
    data/         Navigation config + MOCK catalogue data (prototype only)
    services/     CatalogService — the only place components read data from.
                  Swap mock data for HttpClient calls here when the API exists.
  layout/         Header (services bar + mega-menus + mobile drawer), Footer
  pages/home/     Landing page + hero search widget
  shared/         Reusable UI: icon set (pure SVG paths), hotel card, scroll-reveal directive
src/styles.css    Brand design tokens (blue #156AB3, gold #FFC523), fonts, animations
public/logo.png   Brand logo (transparent background)
```

## Prototype status — read before going live

- **No data is collected or sent.** Search and newsletter forms are intercepted client-side.
- **All hotels, prices, reviews and stats are fictional placeholders** (`core/data/mock-catalog.ts`).
- **Contact details are placeholders** (`AGENCY_CONTACT` in `core/data/navigation.ts`).
- Photos are hot-linked from Unsplash for the demo; replace with the agency's own / licensed images.
