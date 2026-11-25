# Booking notifications (e-mail & WhatsApp)

## What is sent

| When | To | Channel |
|---|---|---|
| Booking request created | Customer | E-mail "demande reçue" (+ WhatsApp if the customer ticked the opt-in and the API is set up) |
| Booking request created | Agency inbox | E-mail "nouvelle demande" with customer contact + link to the admin |
| Status → Confirmée / Annulée / Refusée | Customer | E-mail |

- Sent **after** the booking is saved, on a background thread: a mail problem never blocks a booking.
- Every attempt is listed on the booking (Admin → Réservations → detail → *Notifications*) and stored in
  `notification_log` with a **masked** recipient (`a***@example.tn`) and the result (Envoyé / Échec / Non envoyé).
- Messages are in French; dynamic values are HTML-escaped.

## WhatsApp without any API (works today)

- **Admin → booking detail → "WhatsApp au client"** opens WhatsApp (web or phone) with a message already
  written for the current status; the agent presses *Send* from the agency's WhatsApp.
- **Customer confirmation page → "Nous écrire sur WhatsApp"** opens a chat with the agency
  (`AGENCY_CONTACT.whatsapp`) with the booking reference pre-filled.

## Enable e-mail (agency SMTP mailbox)

Add to `%USERPROFILE%\.gezenthi\db.env` (values from the hosting control panel of `gezenthi-travel.tn`):

```properties
MAIL_ENABLED=true
MAIL_HOST=<SMTP server, e.g. mail.gezenthi-travel.tn>
MAIL_PORT=587
MAIL_USERNAME=reservation@gezenthi-travel.tn
MAIL_PASSWORD=<mailbox password>
MAIL_FROM=reservation@gezenthi-travel.tn
NOTIFY_AGENCY_EMAIL=reservation@gezenthi-travel.tn
# Port 465 instead? set MAIL_PORT=465, MAIL_SSL=true, MAIL_STARTTLS=false
```

Restart the backend, make a test booking with your own e-mail, and check the *Notifications* block.
Tip: also add SPF/DKIM for the domain at the host so messages don't land in spam.

## Enable automatic WhatsApp (Meta WhatsApp Business Cloud API)

1. Meta Business account (verified) → WhatsApp → add a phone number (one **not** used in the normal WhatsApp app).
2. Create and submit a **message template** (category *Utility*, language *French*), name `booking_received`:
   > Bonjour {{1}}, nous avons bien reçu votre demande de réservation {{2}} pour {{3}} ({{4}}).
   > Un conseiller Gezenthi vous contacte rapidement pour la confirmer.
3. Once approved, add to `db.env`:

```properties
WHATSAPP_ENABLED=true
WHATSAPP_TOKEN=<permanent access token (system user)>
WHATSAPP_PHONE_NUMBER_ID=<phone number id>
WHATSAPP_TEMPLATE=booking_received
WHATSAPP_LANGUAGE=fr
```

Only customers who ticked *"recevoir le suivi sur WhatsApp"* are messaged (Meta requires opt-in).
Meta charges per conversation (utility category); check current pricing for Tunisia.
