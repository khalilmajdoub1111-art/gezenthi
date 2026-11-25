import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiError } from '../core/services/api-error';
import {
  BookingDetail, BookingService, BookingStatus, STATUS_LABELS, SupplierCheckResult, toWhatsAppDigits, whatsappLink,
} from '../core/services/booking.service';
import { Icon } from '../shared/icon/icon';
import { STATUS_STYLE } from './admin-bookings';

/** Admin: one booking request — customer, passengers, workflow, supplier actions, full history. */
@Component({
  selector: 'app-admin-booking-detail',
  imports: [RouterLink, DatePipe, DecimalPipe, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <a routerLink="/admin/reservations" class="text-sm text-slate-500 hover:text-brand-600">← Réservations</a>
    @if (error()) {
      <p class="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</p>
    }
    @if (b(); as b) {
      <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
        <h1 class="font-display text-3xl font-bold text-brand-950">{{ b.reference }}</h1>
        <span class="rounded-full px-4 py-1.5 text-sm font-semibold" [class]="style[b.status]">{{ labels[b.status] }}</span>
      </div>
      <p class="text-sm text-slate-500">Reçue le {{ b.createdAt | date: 'dd/MM/yyyy à HH:mm' }} · consentement données le {{ b.consentAt | date: 'dd/MM/yyyy HH:mm' }}</p>

      <div class="mt-6 grid gap-6 lg:grid-cols-[1fr_360px]">
        <div class="space-y-6">
          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Séjour</h2>
            <p class="mt-2 font-semibold">{{ b.hotelName }} <span class="text-gold-400">{{ b.stars ? '★'.repeat(b.stars) : '' }}</span></p>
            <p class="text-sm text-slate-500">{{ b.city }} · {{ b.origin === 'partenaire' ? 'Hôtel partenaire (' + b.supplierName + ')' : 'Offre agence' }}</p>
            <dl class="mt-4 grid gap-2 text-sm sm:grid-cols-2">
              <div><dt class="text-slate-500">Dates</dt><dd class="font-medium">{{ b.checkIn | date: 'dd/MM/yyyy' }} → {{ b.checkOut | date: 'dd/MM/yyyy' }}</dd></div>
              <div><dt class="text-slate-500">Occupation</dt><dd class="font-medium">{{ b.adults }} adulte(s){{ b.childAges.length ? ', enfants : ' + b.childAges.join(', ') + ' ans' : '' }}</dd></div>
              <div><dt class="text-slate-500">Chambre</dt><dd class="font-medium">{{ b.roomName }}</dd></div>
              <div><dt class="text-slate-500">Pension</dt><dd class="font-medium">{{ b.boardName }}</dd></div>
              <div><dt class="text-slate-500">Prix affiché au client</dt><dd class="font-display text-lg font-bold text-brand-700">{{ b.totalPrice | number: '1.0-3' }} {{ b.currency }}</dd></div>
              <div><dt class="text-slate-500">Annulation</dt><dd class="font-medium">{{ b.refundable ? 'Annulable' + (b.cancellationDeadline ? ' jusqu’au ' + b.cancellationDeadline : '') : 'Non remboursable' }}</dd></div>
            </dl>
          </section>

          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Client</h2>
            <p class="mt-2 font-semibold">{{ civ(b.customerCivility) }} {{ b.customerFirstName }} {{ b.customerLastName }}</p>
            <p class="text-sm"><a [href]="'mailto:' + b.customerEmail" class="text-brand-700 hover:underline">{{ b.customerEmail }}</a> ·
              <a [href]="'tel:' + b.customerPhone" class="text-brand-700 hover:underline">{{ b.customerPhone }}</a></p>
            @if (b.comment) { <p class="mt-3 rounded-xl bg-slate-50 p-3 text-sm text-slate-700">« {{ b.comment }} »</p> }
            <h3 class="mt-5 text-sm font-bold uppercase tracking-wider text-slate-500">Voyageurs</h3>
            <ul class="mt-2 divide-y divide-slate-100 text-sm">
              @for (p of b.passengers; track p.position) {
                <li class="py-2">{{ p.type === 'ADULT' ? civ(p.civility) : 'Enfant' }} {{ p.firstName }} {{ p.lastName }}{{ p.age !== null ? ' (' + p.age + ' ans)' : '' }}{{ p.position === 1 ? ' — titulaire' : '' }}</li>
              }
            </ul>
          </section>

          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Historique</h2>
            <ol class="mt-4 space-y-3 border-l-2 border-slate-100 pl-4">
              @for (e of b.events; track $index) {
                <li class="text-sm">
                  <p class="text-xs text-slate-400">{{ e.at | date: 'dd/MM/yyyy HH:mm' }} · {{ e.actor }}</p>
                  <p><strong>{{ actionLabel(e.action) }}</strong>{{ e.details ? ' — ' + e.details : '' }}</p>
                </li>
              }
            </ol>
            <div class="mt-5 flex gap-2">
              <input [value]="note()" (input)="note.set($any($event.target).value)" placeholder="Ajouter une note interne…" maxlength="1000"
                     class="flex-1 rounded-xl bg-slate-50 px-4 py-2.5 text-sm ring-1 ring-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-400" />
              <button type="button" (click)="addNote()" [disabled]="!note().trim() || busy()"
                      class="rounded-xl bg-slate-100 px-4 py-2.5 text-sm font-semibold text-brand-900 hover:bg-slate-200 disabled:opacity-50">Ajouter</button>
            </div>
          </section>
        </div>

        <div class="space-y-6">
          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Statut</h2>
            <div class="mt-4 grid grid-cols-2 gap-2">
              @for (s of statuses; track s) {
                <button type="button" (click)="setStatus(s)" [disabled]="busy() || b.status === s"
                        class="rounded-xl px-3 py-2 text-sm font-semibold ring-1 ring-slate-200 transition hover:bg-slate-50 disabled:cursor-default disabled:opacity-100"
                        [class]="b.status === s ? style[s] + ' ring-0' : ''">{{ labels[s] }}</button>
              }
            </div>
            <p class="mt-3 text-xs text-slate-500">La note ci-contre (si remplie) est jointe au changement de statut.</p>
          </section>

          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Notifications</h2>
            <div class="mt-3 flex flex-wrap gap-2 text-xs font-semibold">
              <span class="rounded-full px-3 py-1" [class]="b.channels.emailReady ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                E-mail {{ b.channels.emailReady ? 'actif' : 'non configuré' }}</span>
              <span class="rounded-full px-3 py-1" [class]="b.channels.whatsappReady ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                WhatsApp auto {{ b.channels.whatsappReady ? 'actif' : 'non configuré' }}</span>
              @if (b.whatsappOptIn) { <span class="rounded-full bg-emerald-50 px-3 py-1 text-emerald-700">Client OK WhatsApp</span> }
            </div>

            @if (customerWhatsApp(b); as link) {
              <a [href]="link" target="_blank" rel="noopener"
                 class="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-500 py-3 text-sm font-semibold text-white hover:bg-emerald-600">
                <app-icon name="whatsapp" [size]="16" /> WhatsApp au client (message prêt)
              </a>
              <p class="mt-1 text-xs text-slate-500">Ouvre WhatsApp avec un message pré-rempli selon le statut ; vous l’envoyez vous-même.</p>
            }

            <ul class="mt-4 space-y-2 text-xs">
              @for (n of b.notifications; track $index) {
                <li class="flex items-start justify-between gap-2 rounded-xl bg-slate-50 px-3 py-2">
                  <span>
                    <strong>{{ n.channel === 'EMAIL' ? 'E-mail' : 'WhatsApp' }}</strong> · {{ n.audience === 'AGENCY' ? 'agence' : 'client' }} ·
                    {{ templateLabel(n.template) }} · {{ n.recipient }}
                    @if (n.error) { <span class="block text-slate-500">{{ n.error }}</span> }
                  </span>
                  <span class="shrink-0 rounded-full px-2 py-0.5 font-semibold"
                        [class]="n.status === 'SENT' ? 'bg-emerald-100 text-emerald-700' : n.status === 'FAILED' ? 'bg-rose-100 text-rose-700' : 'bg-slate-200 text-slate-600'">
                    {{ n.status === 'SENT' ? 'Envoyé' : n.status === 'FAILED' ? 'Échec' : 'Non envoyé' }}</span>
                </li>
              } @empty {
                <li class="text-slate-500">Aucune notification pour le moment.</li>
              }
            </ul>
          </section>

          @if (b.origin === 'partenaire') {
            <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
              <h2 class="font-display text-lg font-semibold text-brand-950">Fournisseur : {{ b.supplierName }}</h2>
              <button type="button" (click)="check()" [disabled]="busy()"
                      class="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 py-3 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50">
                <app-icon name="shield" [size]="16" /> Vérifier prix et disponibilité
              </button>
              <p class="mt-2 text-xs text-slate-500">Pré-réservation : le fournisseur vérifie, <strong>rien n’est réservé</strong>.</p>
              @if (checkResult(); as c) {
                <div class="mt-4 rounded-2xl p-4 text-sm ring-1" [class]="c.available ? 'bg-emerald-50 ring-emerald-200' : 'bg-rose-50 ring-rose-200'">
                  <p class="font-semibold">{{ c.available ? 'Disponible' : 'Non disponible' }}{{ c.onRequest ? ' (sur demande)' : '' }}</p>
                  @if (c.supplierPrice !== null) {
                    <p>Prix fournisseur : <strong>{{ c.supplierPrice | number: '1.0-3' }} {{ c.currency }}</strong> · prix client : {{ c.sellPrice | number: '1.0-3' }}</p>
                  }
                  @if (c.message) { <p class="mt-1 text-slate-600">{{ c.message }}</p> }
                </div>
              }
              @if (b.supplierPrice !== null) {
                <p class="mt-3 text-xs text-slate-500">Dernier prix fournisseur vérifié : {{ b.supplierPrice | number: '1.0-3' }}</p>
              }

              <button type="button" (click)="book()" [disabled]="busy() || !b.supplierBookingEnabled || !!b.supplierBookingId"
                      class="mt-5 w-full rounded-xl py-3 text-sm font-semibold ring-1 transition disabled:cursor-not-allowed disabled:opacity-50"
                      [class]="b.supplierBookingEnabled ? 'bg-gold-400 text-brand-950 ring-gold-400 hover:bg-gold-300' : 'bg-slate-50 text-slate-500 ring-slate-200'">
                Réserver chez le fournisseur
              </button>
              @if (!b.supplierBookingEnabled) {
                <p class="mt-2 text-xs text-slate-500">Désactivé (sécurité) tant que le passage en production n’est pas validé avec le fournisseur.
                  Réservez par téléphone/e-mail puis passez la demande en « Confirmée ».</p>
              }
              @if (b.supplierBookingId) {
                <p class="mt-3 text-sm">N° fournisseur : <strong>{{ b.supplierBookingId }}</strong> ({{ b.supplierState }})
                  @if (b.voucherUrl) { · <a [href]="b.voucherUrl" target="_blank" rel="noopener" class="text-brand-700 underline">Voucher</a> }</p>
              }
            </section>
          }
        </div>
      </div>
    }
  `,
})
export class AdminBookingDetail {
  readonly id = input.required<string>();
  private readonly api = inject(BookingService);
  protected readonly labels = STATUS_LABELS;
  protected readonly style = STATUS_STYLE;
  protected readonly statuses = Object.keys(STATUS_LABELS) as BookingStatus[];
  protected readonly b = signal<BookingDetail | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly note = signal('');
  protected readonly checkResult = signal<SupplierCheckResult | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.run(() => this.api.detail(id)));
    });
  }

  protected setStatus(s: BookingStatus): void {
    void this.run(() => this.api.setStatus(this.id(), s, this.note()), true);
  }

  protected addNote(): void {
    void this.run(() => this.api.addNote(this.id(), this.note()), true);
  }

  protected async check(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.checkResult.set(await this.api.supplierCheck(this.id()));
      this.b.set(await this.api.detail(this.id()));
    } catch (err) {
      this.error.set(ApiError.from(err).message);
    } finally {
      this.busy.set(false);
    }
  }

  protected book(): void {
    if (!confirm('Créer une VRAIE réservation chez le fournisseur ? Elle pourra être facturée.')) return;
    void this.run(() => this.api.supplierBook(this.id()));
  }

  private async run(action: () => Promise<BookingDetail>, clearNote = false): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.b.set(await action());
      if (clearNote) this.note.set('');
    } catch (err) {
      this.error.set(ApiError.from(err).message);
      try {
        this.b.set(await this.api.detail(this.id()));
      } catch {
        /* keep previous */
      }
    } finally {
      this.busy.set(false);
    }
  }

  /** wa.me link to the customer with a message adapted to the current status. */
  protected customerWhatsApp(b: BookingDetail): string | null {
    const to = toWhatsAppDigits(b.customerPhone);
    if (!to) return null;
    const hello = `Bonjour ${this.civ(b.customerCivility)} ${b.customerLastName}, ici Gezenthi Travel.`;
    const stay = `${b.hotelName} (${b.city}), du ${this.d(b.checkIn)} au ${this.d(b.checkOut)}`;
    const text = {
      CONFIRMED: `${hello} Votre réservation ${b.reference} est confirmée : ${stay}. Excellent séjour !`,
      CANCELLED: `${hello} Votre réservation ${b.reference} (${stay}) a été annulée. Contactez-nous pour toute question.`,
      REJECTED: `${hello} Nous ne pouvons malheureusement pas confirmer la demande ${b.reference} (${stay}). Pouvons-nous vous proposer une alternative ?`,
    }[b.status as 'CONFIRMED' | 'CANCELLED' | 'REJECTED']
      ?? `${hello} Nous avons bien reçu votre demande ${b.reference} : ${stay}. Nous revenons vers vous très vite pour la confirmer.`;
    return whatsappLink(to, text);
  }

  private d(iso: string): string {
    return new Date(iso + 'T00:00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' });
  }

  protected templateLabel(t: string): string {
    return ({
      BOOKING_RECEIVED: 'demande reçue',
      AGENCY_NEW_BOOKING: 'alerte nouvelle demande',
      STATUS_CONFIRMED: 'confirmation',
      STATUS_CANCELLED: 'annulation',
      STATUS_REJECTED: 'refus',
    } as Record<string, string>)[t] ?? t;
  }

  protected civ(c: string | null): string {
    return c === 'Mr' ? 'M.' : c === 'Ms' ? 'Mlle' : c === 'Mrs' ? 'Mme' : '';
  }

  protected actionLabel(a: string): string {
    return ({
      CREATED: 'Demande créée',
      STATUS: 'Statut modifié',
      NOTE: 'Note',
      SUPPLIER_CHECK: 'Vérification fournisseur',
      SUPPLIER_BOOKED: 'Réservée chez le fournisseur',
      SUPPLIER_BOOK_REFUSED: 'Réservation fournisseur refusée',
    } as Record<string, string>)[a] ?? a;
  }
}
