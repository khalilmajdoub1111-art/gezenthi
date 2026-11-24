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

## Next steps (booking workflow)

1. Booking request form (customer + passengers) stored in our DB, with the `offerId`.
2. Admin screen: requests list → **pre-book** (`BookingCreation` with `PreBooking=true`, checks price) →
   **confirm** (real booking) — only when `booking-enabled=true` and after payment rules are decided.
3. Store supplier booking id, voucher URL, state (OnRequest / Validated / Cancelled), cancellation via
   `BookingCancellation` with `PreCancelled=true` first to show fees.
4. Currency: if the supplier returns EUR, agree on TND pricing or add a conversion rate.
