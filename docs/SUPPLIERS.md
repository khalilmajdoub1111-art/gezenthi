# Hotel suppliers (B2B APIs) — how it works

Travel agencies sell two kinds of hotel offers:

| | Own offers ("Sélection Gezenthi") | Supplier offers ("partenaire") |
|---|---|---|
| Where the data lives | Our database, typed in the admin | The supplier's system (TunisiaBeds…) |
| Price | Fixed by the agency | Live net price from the supplier **+ agency markup** |
| Availability | Managed by hand | Live, per dates and occupancy |
| Booking | Agency confirms by phone/email | Supplier API (pre-book → book) — *not enabled yet* |

## Architecture

```
            ┌─────────────── backend (Spring Boot) ───────────────┐
Browser ──► │ /api/public/hotels/search                            │
            │   HotelSearchService                                  │
            │     ├─ own hotels (catalog tables)                    │
            │     └─ for each enabled supplier, in parallel:        │
            │          supplier_hotel (cached ids for the city)     │
            │          HotelSupplier.search(...)  ── HTTPS ──► API  │
            │          + markup, tokens kept server-side            │
            │                                                       │
Admin  ───► │ /api/admin/suppliers  (status · test · sync · log)    │
            │   SupplierSyncService → supplier_hotel, sync log      │
            └───────────────────────────────────────────────────────┘
```

- **`HotelSupplier`** (`backend/.../supplier/HotelSupplier.java`): the one contract every supplier implements
  (test connection, static hotels, search). The rest of the app never sees supplier-specific JSON.
- **`OsTravelSupplier`**: adapter for the **OS-TRAVEL Hotel API v2** used by TunisiaBeds
  (docs: <https://api-edocs.os-travel.com>). Any other supplier on OS-TRAVEL = configuration only.
  A supplier on another platform = one new adapter class.
- **`SupplierRegistry`**: builds suppliers from `gezenthi.suppliers.*` configuration.
- **Static sync**: OS-TRAVEL only searches by *their* hotel ids (max 200 per call), so
  `ListCountry → ListCity → ListHotel` is copied into `supplier_hotel`. Every run is logged in
  `supplier_sync_log` (who, when, result). Hotels that disappear are deactivated, not deleted.
- **Search**: own + supplier offers merged and sorted by price; each supplier call has a timeout, and a
  failing supplier only adds a warning. Results cached 5 min; public endpoint rate-limited (20/min/IP).
- **Tokens stay on the server** (`OfferRefCache`): the browser gets an opaque `offerId`, never the
  supplier `Token`/`Source`, the supplier's name or its net price.

## Configure TunisiaBeds (you do this — credentials never go in Git)

Add to `%USERPROFILE%\.gezenthi\db.env` (the private file next to the DB credentials):

```properties
TUNISIABEDS_LOGIN=<login from the TunisiaBeds email>
TUNISIABEDS_PASSWORD=<password from the TunisiaBeds email>
TUNISIABEDS_ENABLED=true
TUNISIABEDS_MARKUP_PERCENT=10
```

Restart the backend, then in **Admin → Fournisseurs (API)**:

1. **Tester la connexion** → calls `ListCountry` (read-only).
2. **Synchroniser les hôtels** → fills `supplier_hotel` (read-only calls).
3. Search the city of the test hotel on the site.

The account is in **Dev mode**: only test hotel **id 171** returns prices until TunisiaBeds switches it to
production (after test reservations, which must be done together with them — see below).

## Safety rules built in

- Only **read-only** methods are implemented (`ListCountry`, `ListCity`, `ListHotel`, `HotelSearch`).
  `BookingCreation` / `BookingCancellation` are **not called anywhere** in the code.
- `booking-enabled=false` is the future switch for real bookings.
- The request body (which contains the password) is **never logged**; only method, duration and error text.
- Supplier errors shown to the admin never include credentials.

## Hotel pages

`GET /api/public/hotels/{ref}` — `HotelDetail` is called the first time a hotel page opens and cached
7 days in `supplier_hotel_detail`. Supplier HTML is sanitised (jsoup, text formatting only).
Partner hotels also appear on the home page ("Plus de 500 hôtels partenaires", prices on request).

## Booking workflow (built)

1. **Customer** (site): search → hotel/room → `/reservation` form → `POST /api/public/bookings`.
   Only `offerId` + room choice are sent; hotel, dates and **price come from the server** (`OfferRefCache`).
   Consent is mandatory; reference `GZ-YYYY-NNNNNN`.
2. **Admin** → *Réservations*: list, detail (customer, passengers), status (Nouvelle → En cours →
   Confirmée / Annulée / Refusée), internal notes, **full history** in `booking_event` (who, when, what).
3. **Vérifier prix et disponibilité** (partner hotels): fresh `HotelSearch` + `BookingCreation` with
   `PreBooking=true` → supplier confirms price, **nothing is booked**.
4. **Réserver chez le fournisseur**: real `BookingCreation`. **Refused** while `booking-enabled=false`
   (checked in `BookingService` *and* in the adapter); refusals are recorded in the history.

### Before enabling real bookings (with TunisiaBeds)
- Test reservations in their dev environment (needs rates on hotel 171), then production switch.
- Check the real `BookingCreation` response fields (logged as "shape") — price/state/voucher parsing.
- `BookingCancellation` (with `PreCancelled=true` first) is not implemented yet.
- Currency: if the supplier returns EUR, agree on TND pricing or add a conversion rate.
- Customer notification e-mails (needs an SMTP account) and online payment are not implemented.
