import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AGENCY_CONTACT } from '../../core/data/navigation';
import { ApiError } from '../../core/services/api-error';
import { BookingService, OfferSummary, PassengerIn } from '../../core/services/booking.service';
import { Icon } from '../../shared/icon/icon';

type PaxGroup = FormGroup<{
  type: FormControl<'ADULT' | 'CHILD'>;
  civility: FormControl<string>;
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  age: FormControl<number | null>;
}>;

/**
 * /reservation?offer=…&board=…&room=…
 * Booking REQUEST: price, hotel and dates come from the server-side offer; the customer only
 * provides contact details + passengers + consent. A Gezenthi agent then confirms.
 */
@Component({
  selector: 'app-booking-page',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block bg-sand min-h-screen' },
  template: `
    <section class="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      @if (done(); as ref) {
        <div class="mx-auto max-w-xl rounded-[28px] bg-white p-10 text-center shadow-lg ring-1 ring-slate-100">
          <span class="mx-auto grid size-16 place-items-center rounded-full bg-emerald-50 text-emerald-600"><app-icon name="check" [size]="32" [stroke]="2.5" /></span>
          <h1 class="mt-6 font-display text-2xl font-bold text-brand-950">Demande envoyée !</h1>
          <p class="mt-2 text-slate-600">Votre référence :</p>
          <p class="mt-1 font-display text-3xl font-bold tracking-wide text-brand-700">{{ ref }}</p>
          <p class="mt-4 text-slate-600">Un conseiller Gezenthi vous contacte rapidement pour confirmer la disponibilité et le paiement.
            Aucun paiement n’a été effectué.</p>
          <a routerLink="/" class="mt-8 inline-flex rounded-full bg-brand-600 px-6 py-3 font-semibold text-white hover:bg-brand-700">Retour à l’accueil</a>
        </div>
      } @else if (loadError()) {
        <div class="mx-auto max-w-xl rounded-3xl bg-white p-10 text-center ring-1 ring-slate-100">
          <p class="text-slate-700">{{ loadError() }}</p>
          <a routerLink="/" class="mt-6 inline-block font-semibold text-brand-600">← Nouvelle recherche</a>
        </div>
      } @else if (!offer()) {
        <div class="grid place-items-center py-24"><span class="size-10 animate-spin rounded-full border-4 border-brand-100 border-t-brand-600"></span></div>
      } @else {
        @let o = offer()!;
        <h1 class="font-display text-3xl font-bold text-brand-950">Demande de réservation</h1>
        <p class="mt-1 text-slate-500">Aucun paiement maintenant : un conseiller confirme votre réservation.</p>

        <div class="mt-8 grid gap-8 lg:grid-cols-[1fr_340px]">
          <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-6" novalidate>
            <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100" formGroupName="customer">
              <h2 class="font-display text-lg font-semibold text-brand-950">Vos coordonnées</h2>
              <div class="mt-4 grid gap-4 sm:grid-cols-[120px_1fr_1fr]">
                <select formControlName="civility" [class]="input" aria-label="Civilité">
                  <option value="Mrs">Mme</option><option value="Mr">M.</option><option value="Ms">Mlle</option>
                </select>
                <input formControlName="firstName" placeholder="Prénom" autocomplete="given-name" [class]="input" />
                <input formControlName="lastName" placeholder="Nom" autocomplete="family-name" [class]="input" />
              </div>
              <div class="mt-4 grid gap-4 sm:grid-cols-2">
                <input formControlName="email" type="email" placeholder="E-mail" autocomplete="email" [class]="input" />
                <input formControlName="phone" type="tel" placeholder="Téléphone (+216…)" autocomplete="tel" [class]="input" />
              </div>
            </div>

            <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
              <h2 class="font-display text-lg font-semibold text-brand-950">Voyageurs</h2>
              <p class="text-sm text-slate-500">Noms tels qu’ils figurent sur la pièce d’identité. Adulte 1 = titulaire de la réservation.</p>
              <div class="mt-4 space-y-3" formArrayName="passengers">
                @for (p of passengers.controls; track $index; let i = $index) {
                  <div [formGroupName]="i" class="grid items-center gap-3 sm:grid-cols-[110px_120px_1fr_1fr]">
                    <span class="text-sm font-semibold text-brand-900">
                      {{ p.controls.type.value === 'ADULT' ? 'Adulte ' + (i + 1) : 'Enfant (' + p.controls.age.value + ' ans)' }}
                    </span>
                    @if (p.controls.type.value === 'ADULT') {
                      <select formControlName="civility" [class]="input" aria-label="Civilité">
                        <option value="Mrs">Mme</option><option value="Mr">M.</option><option value="Ms">Mlle</option>
                      </select>
                    } @else { <span></span> }
                    <input formControlName="firstName" placeholder="Prénom" [class]="input" />
                    <input formControlName="lastName" placeholder="Nom" [class]="input" />
                  </div>
                }
              </div>
            </div>

            <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
              <label class="block text-sm font-semibold text-brand-950">Demande particulière (facultatif)
                <textarea formControlName="comment" rows="3" maxlength="1000" placeholder="Lit bébé, chambre calme, heure d’arrivée…" [class]="input + ' mt-2'"></textarea>
              </label>
              <label class="mt-4 flex cursor-pointer items-start gap-3 text-sm text-slate-700">
                <input type="checkbox" formControlName="consent" class="mt-0.5 size-5 accent-brand-600" />
                <span>J’accepte que Gezenthi Travel utilise ces informations uniquement pour traiter ma réservation
                  et me contacter à ce sujet. Elles ne sont jamais revendues.</span>
              </label>
            </div>

            @if (error()) {
              <p class="rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</p>
            }
            <button type="submit" [disabled]="sending()"
                    class="w-full rounded-2xl bg-gradient-to-br from-gold-300 to-gold-400 py-4 font-display text-lg font-bold text-brand-950 shadow-lg shadow-gold-400/40 transition hover:-translate-y-0.5 disabled:opacity-60">
              {{ sending() ? 'Envoi…' : 'Envoyer ma demande de réservation' }}
            </button>
          </form>

          <aside class="lg:sticky lg:top-24 lg:self-start">
            <div class="overflow-hidden rounded-3xl bg-white shadow-lg shadow-brand-950/5 ring-1 ring-slate-100">
              @if (o.image) { <img [src]="o.image" [alt]="o.hotelName" class="h-40 w-full object-cover" /> }
              <div class="p-6">
                @if (o.stars) { <p class="text-sm text-gold-400">{{ '★'.repeat(o.stars) }}</p> }
                <h2 class="font-display text-lg font-semibold text-brand-950">{{ o.hotelName }}</h2>
                <p class="text-sm text-slate-500">{{ o.city }}</p>
                <dl class="mt-4 space-y-2 text-sm">
                  <div class="flex justify-between"><dt class="text-slate-500">Dates</dt><dd class="font-medium">{{ o.checkIn | date: 'd MMM' }} → {{ o.checkOut | date: 'd MMM y' }}</dd></div>
                  <div class="flex justify-between"><dt class="text-slate-500">Durée</dt><dd class="font-medium">{{ o.nights }} nuit(s)</dd></div>
                  <div class="flex justify-between"><dt class="text-slate-500">Voyageurs</dt><dd class="font-medium">{{ o.adults }} adulte(s){{ o.childAges.length ? ', ' + o.childAges.length + ' enfant(s)' : '' }}</dd></div>
                  <div class="flex justify-between gap-4"><dt class="text-slate-500">Chambre</dt><dd class="text-right font-medium">{{ o.roomName }}</dd></div>
                  <div class="flex justify-between gap-4"><dt class="text-slate-500">Pension</dt><dd class="text-right font-medium">{{ o.boardName }}</dd></div>
                </dl>
                <p class="mt-3 text-xs" [class]="o.refundable ? 'text-emerald-600' : 'text-rose-600'">
                  {{ o.refundable ? 'Annulable' + (o.cancellationDeadline ? ' jusqu’au ' + o.cancellationDeadline : '') : 'Non remboursable' }}
                </p>
                <div class="mt-4 flex items-end justify-between border-t border-slate-100 pt-4">
                  <span class="text-sm text-slate-500">Total séjour</span>
                  <span class="font-display text-2xl font-bold text-brand-700">{{ o.totalPrice | number: '1.0-0' }} {{ o.currency === 'TND' ? 'DT' : o.currency }}</span>
                </div>
                <p class="mt-4 text-xs text-slate-500">Une question ? <a [href]="contact.phoneHref" class="font-semibold text-brand-700">{{ contact.phone }}</a></p>
              </div>
            </div>
          </aside>
        </div>
      }
    </section>
  `,
})
export class BookingPage {
  readonly offerParam = input<string | undefined>(undefined, { alias: 'offer' });
  readonly boardParam = input<string | undefined>(undefined, { alias: 'board' });
  readonly roomParam = input<string | undefined>(undefined, { alias: 'room' });

  private readonly api = inject(BookingService);
  protected readonly contact = AGENCY_CONTACT;
  protected readonly offer = signal<OfferSummary | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly sending = signal(false);
  protected readonly done = signal<string | null>(null);
  protected readonly input =
    'w-full rounded-xl bg-slate-50 px-4 py-3 text-sm text-brand-950 ring-1 ring-slate-200 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-400';

  protected readonly form = new FormGroup({
    customer: new FormGroup({
      civility: new FormControl('Mrs', { nonNullable: true }),
      firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
      lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
      email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
      phone: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[+0-9 ()./-]{6,30}$/)] }),
    }),
    passengers: new FormArray<PaxGroup>([]),
    comment: new FormControl('', { nonNullable: true }),
    consent: new FormControl(false, { nonNullable: true, validators: [Validators.requiredTrue] }),
  });

  protected get passengers(): FormArray<PaxGroup> {
    return this.form.controls.passengers;
  }

  constructor() {
    effect(() => {
      const id = this.offerParam();
      const board = Number(this.boardParam() ?? 0);
      const room = Number(this.roomParam() ?? 0);
      untracked(() => void this.load(id, board, room));
    });
  }

  private async load(id: string | undefined, board: number, room: number): Promise<void> {
    if (!id) {
      this.loadError.set('Offre introuvable. Merci de relancer la recherche.');
      return;
    }
    try {
      const o = await this.api.offer(id, board, room);
      this.passengers.clear();
      for (let i = 0; i < o.adults; i++) this.passengers.push(this.pax('ADULT', null));
      for (const age of o.childAges) this.passengers.push(this.pax('CHILD', age));
      this.offer.set(o);
    } catch (err) {
      this.loadError.set(err instanceof Error ? err.message : 'Offre introuvable.');
    }
  }

  private pax(type: 'ADULT' | 'CHILD', age: number | null): PaxGroup {
    return new FormGroup({
      type: new FormControl(type, { nonNullable: true }),
      civility: new FormControl('Mrs', { nonNullable: true }),
      firstName: new FormControl('', { nonNullable: true }),
      lastName: new FormControl('', { nonNullable: true }),
      age: new FormControl<number | null>(age),
    });
  }

  async submit(): Promise<void> {
    this.error.set(null);
    const customer = this.form.controls.customer.getRawValue();
    // Adult 1 defaults to the customer if left empty.
    const first = this.passengers.at(0);
    if (first && !first.controls.firstName.value.trim() && !first.controls.lastName.value.trim()) {
      first.patchValue({ civility: customer.civility, firstName: customer.firstName, lastName: customer.lastName });
    }
    const missingPax = this.passengers.controls.some((p) => !p.controls.firstName.value.trim() || !p.controls.lastName.value.trim());
    if (this.form.controls.customer.invalid) {
      this.error.set('Vérifiez vos coordonnées (nom, prénom, e-mail et téléphone).');
      return;
    }
    if (missingPax) {
      this.error.set('Indiquez le prénom et le nom de chaque voyageur.');
      return;
    }
    if (!this.form.controls.consent.value) {
      this.error.set('Merci d’accepter le traitement de vos données pour cette réservation.');
      return;
    }
    const passengers: PassengerIn[] = this.passengers.getRawValue().map((p) => ({
      type: p.type,
      civility: p.type === 'ADULT' ? (p.civility as PassengerIn['civility']) : null,
      firstName: p.firstName.trim(),
      lastName: p.lastName.trim(),
    }));
    this.sending.set(true);
    try {
      const r = await this.api.create({
        offerId: this.offerParam()!,
        board: Number(this.boardParam() ?? 0),
        room: Number(this.roomParam() ?? 0),
        customer,
        passengers,
        comment: this.form.controls.comment.value,
        consent: true,
      });
      this.done.set(r.reference);
      window.scrollTo({ top: 0 });
    } catch (err) {
      const e = ApiError.from(err);
      const fields = Object.values(e.fieldErrors);
      this.error.set(fields.length ? `${e.message} ${fields.join(' · ')}` : e.message);
    } finally {
      this.sending.set(false);
    }
  }
}
