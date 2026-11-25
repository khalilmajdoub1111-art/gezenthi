import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AGENCY_CONTACT } from '../../core/data/navigation';
import { ApiError } from '../../core/services/api-error';
import { whatsappLink } from '../../core/services/booking.service';
import { INQUIRY_TYPE_LABELS, InquiryService } from '../../core/services/inquiry.service';
import { Icon } from '../icon/icon';

/**
 * "Demande de devis" dialog, opened from any offer (trips, Omra, visa, flights, CTN, hotels
 * without online price). Collects contact + trip details + consent, sends to the API.
 */
@Component({
  selector: 'app-quote-dialog',
  imports: [ReactiveFormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'close()' },
  template: `
    @if (service.dialog(); as ctx) {
      <div class="fixed inset-0 z-[80] flex items-end justify-center bg-brand-950/60 p-0 backdrop-blur-sm sm:items-center sm:p-4" (click)="close()">
        <div class="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-t-[28px] bg-white shadow-2xl sm:rounded-[28px]"
             role="dialog" aria-modal="true" aria-labelledby="quote-title" (click)="$event.stopPropagation()">
          <div class="flex items-start justify-between gap-4 border-b border-slate-100 p-6">
            <div>
              <p class="text-xs font-bold uppercase tracking-wider text-gold-600">Demande de devis · {{ labels[ctx.type] }}</p>
              <h2 id="quote-title" class="mt-1 font-display text-xl font-bold text-brand-950">{{ ctx.subject }}</h2>
              <p class="mt-1 text-sm text-slate-500">Gratuit et sans engagement : un conseiller vous répond rapidement.</p>
            </div>
            <button type="button" (click)="close()" class="grid size-10 shrink-0 place-items-center rounded-full hover:bg-slate-100" aria-label="Fermer">
              <app-icon name="close" [size]="20" />
            </button>
          </div>

          @if (done(); as ref) {
            <div class="p-8 text-center">
              <span class="mx-auto grid size-14 place-items-center rounded-full bg-emerald-50 text-emerald-600"><app-icon name="check" [size]="28" [stroke]="2.5" /></span>
              <p class="mt-4 font-display text-lg font-bold text-brand-950">Demande envoyée !</p>
              <p class="mt-1 text-slate-600">Votre référence : <strong class="text-brand-700">{{ ref }}</strong></p>
              <p class="mt-2 text-sm text-slate-500">Un récapitulatif vous est envoyé par e-mail.</p>
              <div class="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
                <a [href]="whatsappAgency(ref)" target="_blank" rel="noopener"
                   class="inline-flex items-center justify-center gap-2 rounded-full bg-emerald-500 px-5 py-2.5 font-semibold text-white hover:bg-emerald-600">
                  <app-icon name="whatsapp" [size]="18" /> Nous écrire sur WhatsApp
                </a>
                <button type="button" (click)="close()" class="rounded-full bg-brand-600 px-5 py-2.5 font-semibold text-white hover:bg-brand-700">Fermer</button>
              </div>
            </div>
          } @else {
            <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4 p-6" novalidate>
              <div class="grid grid-cols-2 gap-3">
                <label class="text-xs font-bold uppercase tracking-wider text-slate-500">Départ / arrivée
                  <input type="date" formControlName="dateFrom" [min]="today" [class]="input + ' mt-1'" />
                </label>
                <label class="text-xs font-bold uppercase tracking-wider text-slate-500">Retour (facultatif)
                  <input type="date" formControlName="dateTo" [min]="form.controls.dateFrom.value || today" [class]="input + ' mt-1'" />
                </label>
                <label class="text-xs font-bold uppercase tracking-wider text-slate-500">Adultes
                  <input type="number" min="1" max="20" formControlName="adults" [class]="input + ' mt-1'" />
                </label>
                <label class="text-xs font-bold uppercase tracking-wider text-slate-500">Enfants
                  <input type="number" min="0" max="10" formControlName="children" [class]="input + ' mt-1'" />
                </label>
              </div>
              <div class="grid gap-3 sm:grid-cols-[110px_1fr_1fr]">
                <select formControlName="civility" [class]="input" aria-label="Civilité">
                  <option value="Mrs">Mme</option><option value="Mr">M.</option><option value="Ms">Mlle</option>
                </select>
                <input formControlName="firstName" placeholder="Prénom" autocomplete="given-name" [class]="input" />
                <input formControlName="lastName" placeholder="Nom" autocomplete="family-name" [class]="input" />
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <input formControlName="email" type="email" placeholder="E-mail" autocomplete="email" [class]="input" />
                <input formControlName="phone" type="tel" placeholder="Téléphone (+216…)" autocomplete="tel" [class]="input" />
              </div>
              <textarea formControlName="message" rows="3" maxlength="1500" placeholder="Votre demande (budget, ville de départ, chambre…)" [class]="input"></textarea>

              <label class="flex cursor-pointer items-start gap-3 text-sm text-slate-700">
                <input type="checkbox" formControlName="whatsappOptIn" class="mt-0.5 size-5 accent-emerald-600" />
                <span>Je souhaite aussi être contacté(e) sur <strong>WhatsApp</strong> (facultatif).</span>
              </label>
              <label class="flex cursor-pointer items-start gap-3 text-sm text-slate-700">
                <input type="checkbox" formControlName="consent" class="mt-0.5 size-5 accent-brand-600" />
                <span>J’accepte que Gezenthi Travel utilise ces informations uniquement pour répondre à ma demande.
                  Elles ne sont jamais revendues.</span>
              </label>

              @if (error()) {
                <p class="rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</p>
              }
              <button type="submit" [disabled]="sending()"
                      class="w-full rounded-2xl bg-gradient-to-br from-gold-300 to-gold-400 py-3.5 font-display font-bold text-brand-950 shadow-lg shadow-gold-400/40 transition hover:-translate-y-0.5 disabled:opacity-60">
                {{ sending() ? 'Envoi…' : 'Envoyer ma demande' }}
              </button>
              <p class="text-center text-xs text-slate-500">Ou appelez-nous : <a [href]="contact.phoneHref" class="font-semibold text-brand-700">{{ contact.phone }}</a></p>
            </form>
          }
        </div>
      </div>
    }
  `,
})
export class QuoteDialog {
  protected readonly service = inject(InquiryService);
  protected readonly contact = AGENCY_CONTACT;
  protected readonly labels = INQUIRY_TYPE_LABELS;
  protected readonly today = new Date().toISOString().slice(0, 10);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly done = signal<string | null>(null);
  protected readonly input =
    'w-full rounded-xl bg-slate-50 px-4 py-3 text-sm normal-case tracking-normal text-brand-950 ring-1 ring-slate-200 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-400';

  protected readonly form = new FormGroup({
    dateFrom: new FormControl('', { nonNullable: true }),
    dateTo: new FormControl('', { nonNullable: true }),
    adults: new FormControl(2, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(20)] }),
    children: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(10)] }),
    civility: new FormControl('Mrs', { nonNullable: true }),
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[+0-9 ()./-]{6,30}$/)] }),
    message: new FormControl('', { nonNullable: true }),
    whatsappOptIn: new FormControl(false, { nonNullable: true }),
    consent: new FormControl(false, { nonNullable: true }),
  });

  constructor() {
    // Each time the dialog opens: reset the result and pre-fill dates/travellers from the context.
    effect(() => {
      const ctx = this.service.dialog();
      if (!ctx) return;
      untracked(() => {
        this.done.set(null);
        this.error.set(null);
        this.form.patchValue({
          dateFrom: ctx.dateFrom ?? '', dateTo: ctx.dateTo ?? '', adults: ctx.adults ?? 2, children: ctx.children ?? 0,
          message: '', consent: false,
        });
      });
    });
  }

  protected close(): void {
    this.service.close();
  }

  protected whatsappAgency(ref: string): string {
    return whatsappLink(this.contact.whatsapp, `Bonjour Gezenthi, je viens d’envoyer la demande de devis ${ref}.`);
  }

  async submit(): Promise<void> {
    const ctx = this.service.dialog();
    if (!ctx) return;
    this.error.set(null);
    const v = this.form.getRawValue();
    if (this.form.controls.firstName.invalid || this.form.controls.lastName.invalid || this.form.controls.email.invalid || this.form.controls.phone.invalid) {
      this.error.set('Indiquez votre prénom, nom, e-mail et téléphone.');
      return;
    }
    if (this.form.controls.adults.invalid || this.form.controls.children.invalid) {
      this.error.set('Nombre de voyageurs invalide.');
      return;
    }
    if (!v.consent) {
      this.error.set('Merci d’accepter le traitement de vos données pour répondre à votre demande.');
      return;
    }
    this.sending.set(true);
    try {
      const r = await this.service.create({
        type: ctx.type, subject: ctx.subject, offerRef: ctx.offerRef ?? null,
        dateFrom: v.dateFrom || null, dateTo: v.dateTo || null, adults: Number(v.adults), children: Number(v.children),
        message: v.message, whatsappOptIn: v.whatsappOptIn, consent: true,
        customer: { civility: v.civility, firstName: v.firstName.trim(), lastName: v.lastName.trim(), email: v.email.trim(), phone: v.phone.trim() },
      });
      this.done.set(r.reference);
    } catch (err) {
      const e = ApiError.from(err);
      const fields = Object.values(e.fieldErrors);
      this.error.set(fields.length ? `${e.message} ${fields.join(' · ')}` : e.message);
    } finally {
      this.sending.set(false);
    }
  }
}
