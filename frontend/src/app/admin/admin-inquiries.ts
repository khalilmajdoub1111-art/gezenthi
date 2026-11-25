import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiError } from '../core/services/api-error';
import { toWhatsAppDigits, whatsappLink } from '../core/services/booking.service';
import {
  INQUIRY_STATUS_LABELS, INQUIRY_TYPE_LABELS, InquiryDetail, InquiryService, InquiryStatus, InquirySummary,
} from '../core/services/inquiry.service';
import { Icon } from '../shared/icon/icon';

const STATUS_STYLE: Record<InquiryStatus, string> = {
  NEW: 'bg-gold-100 text-gold-600',
  IN_PROGRESS: 'bg-brand-50 text-brand-700',
  DONE: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-slate-100 text-slate-500',
};

/** Admin: quote requests list. */
@Component({
  selector: 'app-admin-inquiries',
  imports: [RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <h1 class="font-display text-3xl font-bold text-brand-950">Demandes de devis</h1>
    <p class="mt-1 text-slate-500">Voyages, Omra, visa, vols, CTN et hôtels sans prix en ligne. Données personnelles : accès administration uniquement.</p>
    <div class="no-scrollbar mt-6 flex gap-2 overflow-x-auto">
      @for (f of filters; track f.label) {
        <button type="button" (click)="filter(f.value)" class="shrink-0 rounded-full px-4 py-1.5 text-sm font-semibold transition"
                [class]="active() === f.value ? 'bg-brand-600 text-white' : 'bg-white text-brand-900 ring-1 ring-slate-200 hover:ring-brand-300'">{{ f.label }}</button>
      }
    </div>
    @if (error()) { <p class="mt-6 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">{{ error() }}</p> }
    <div class="mt-6 overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-100">
      @for (q of rows(); track q.id) {
        <a [routerLink]="['/admin/devis', q.id]" class="flex flex-col gap-2 border-b border-slate-100 p-4 transition last:border-0 hover:bg-slate-50 md:flex-row md:items-center md:gap-6">
          <div class="md:w-40">
            <p class="font-mono text-sm font-semibold text-brand-950">{{ q.reference }}</p>
            <p class="text-xs text-slate-400">{{ q.createdAt | date: 'dd/MM/yyyy HH:mm' }}</p>
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate font-semibold text-brand-950">{{ q.customerName }}</p>
            <p class="truncate text-sm text-slate-500">{{ types[q.type] }} · {{ q.subject }}{{ q.dateFrom ? ' · ' + (q.dateFrom | date: 'dd/MM/yy') : '' }}</p>
          </div>
          <span class="text-sm text-slate-500">{{ q.adults }} ad.{{ q.children ? ' + ' + q.children + ' enf.' : '' }}</span>
          <span class="self-start rounded-full px-3 py-1 text-xs font-semibold md:self-auto" [class]="style[q.status]">{{ labels[q.status] }}</span>
        </a>
      } @empty {
        <p class="p-12 text-center text-slate-500">{{ loading() ? 'Chargement…' : 'Aucune demande pour le moment.' }}</p>
      }
    </div>
  `,
})
export class AdminInquiries {
  private readonly api = inject(InquiryService);
  protected readonly labels = INQUIRY_STATUS_LABELS;
  protected readonly types = INQUIRY_TYPE_LABELS;
  protected readonly style = STATUS_STYLE;
  protected readonly rows = signal<InquirySummary[]>([]);
  protected readonly active = signal<InquiryStatus | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filters: { label: string; value: InquiryStatus | null }[] = [
    { label: 'Toutes', value: null },
    ...(Object.keys(INQUIRY_STATUS_LABELS) as InquiryStatus[]).map((s) => ({ label: INQUIRY_STATUS_LABELS[s], value: s })),
  ];

  constructor() {
    void this.load();
  }

  protected filter(s: InquiryStatus | null): void {
    this.active.set(s);
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.rows.set(await this.api.list(this.active()));
      this.error.set(null);
    } catch (err) {
      this.error.set(ApiError.from(err).message);
    } finally {
      this.loading.set(false);
    }
  }
}

/** Admin: one quote request — customer, request, status, notes, history, notifications, WhatsApp. */
@Component({
  selector: 'app-admin-inquiry-detail',
  imports: [RouterLink, DatePipe, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <a routerLink="/admin/devis" class="text-sm text-slate-500 hover:text-brand-600">← Demandes de devis</a>
    @if (error()) { <p class="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</p> }
    @if (q(); as q) {
      <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
        <h1 class="font-display text-3xl font-bold text-brand-950">{{ q.reference }}</h1>
        <span class="rounded-full px-4 py-1.5 text-sm font-semibold" [class]="style[q.status]">{{ labels[q.status] }}</span>
      </div>
      <p class="text-sm text-slate-500">Reçue le {{ q.createdAt | date: 'dd/MM/yyyy à HH:mm' }} · consentement le {{ q.consentAt | date: 'dd/MM/yyyy HH:mm' }}</p>

      <div class="mt-6 grid gap-6 lg:grid-cols-[1fr_360px]">
        <div class="space-y-6">
          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Demande</h2>
            <p class="mt-2 font-semibold">{{ types[q.type] }} — {{ q.subject }}</p>
            <dl class="mt-3 grid gap-2 text-sm sm:grid-cols-2">
              <div><dt class="text-slate-500">Dates</dt><dd class="font-medium">{{ q.dateFrom ? (q.dateFrom | date: 'dd/MM/yyyy') : 'À préciser' }}{{ q.dateTo ? ' → ' + (q.dateTo | date: 'dd/MM/yyyy') : '' }}</dd></div>
              <div><dt class="text-slate-500">Voyageurs</dt><dd class="font-medium">{{ q.adults }} adulte(s){{ q.children ? ', ' + q.children + ' enfant(s)' : '' }}</dd></div>
            </dl>
            @if (q.message) { <p class="mt-3 rounded-xl bg-slate-50 p-3 text-sm text-slate-700">« {{ q.message }} »</p> }
          </section>
          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Client</h2>
            <p class="mt-2 font-semibold">{{ civ(q.customerCivility) }} {{ q.customerFirstName }} {{ q.customerLastName }}</p>
            <p class="text-sm"><a [href]="'mailto:' + q.customerEmail" class="text-brand-700 hover:underline">{{ q.customerEmail }}</a> ·
              <a [href]="'tel:' + q.customerPhone" class="text-brand-700 hover:underline">{{ q.customerPhone }}</a></p>
          </section>
          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Historique</h2>
            <ol class="mt-4 space-y-3 border-l-2 border-slate-100 pl-4">
              @for (e of q.events; track $index) {
                <li class="text-sm">
                  <p class="text-xs text-slate-400">{{ e.at | date: 'dd/MM/yyyy HH:mm' }} · {{ e.actor }}</p>
                  <p><strong>{{ e.action === 'CREATED' ? 'Demande créée' : e.action === 'STATUS' ? 'Statut modifié' : 'Note' }}</strong>{{ e.details ? ' — ' + e.details : '' }}</p>
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
                <button type="button" (click)="setStatus(s)" [disabled]="busy() || q.status === s"
                        class="rounded-xl px-3 py-2 text-sm font-semibold ring-1 ring-slate-200 transition hover:bg-slate-50 disabled:opacity-100"
                        [class]="q.status === s ? style[s] + ' ring-0' : ''">{{ labels[s] }}</button>
              }
            </div>
          </section>
          <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Contact & notifications</h2>
            @if (whatsapp(q); as link) {
              <a [href]="link" target="_blank" rel="noopener"
                 class="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-500 py-3 text-sm font-semibold text-white hover:bg-emerald-600">
                <app-icon name="whatsapp" [size]="16" /> WhatsApp au client (message prêt)
              </a>
            }
            @if (q.whatsappOptIn) { <p class="mt-2 text-xs text-emerald-700">Le client accepte d’être contacté sur WhatsApp.</p> }
            <ul class="mt-4 space-y-2 text-xs">
              @for (n of q.notifications; track $index) {
                <li class="flex items-start justify-between gap-2 rounded-xl bg-slate-50 px-3 py-2">
                  <span><strong>E-mail</strong> · {{ n.audience === 'AGENCY' ? 'agence' : 'client' }} · {{ n.recipient }}
                    @if (n.error) { <span class="block text-slate-500">{{ n.error }}</span> }</span>
                  <span class="shrink-0 rounded-full px-2 py-0.5 font-semibold"
                        [class]="n.status === 'SENT' ? 'bg-emerald-100 text-emerald-700' : n.status === 'FAILED' ? 'bg-rose-100 text-rose-700' : 'bg-slate-200 text-slate-600'">
                    {{ n.status === 'SENT' ? 'Envoyé' : n.status === 'FAILED' ? 'Échec' : 'Non envoyé' }}</span>
                </li>
              } @empty { <li class="text-slate-500">Aucune notification.</li> }
            </ul>
          </section>
        </div>
      </div>
    }
  `,
})
export class AdminInquiryDetail {
  readonly id = input.required<string>();
  private readonly api = inject(InquiryService);
  protected readonly labels = INQUIRY_STATUS_LABELS;
  protected readonly types = INQUIRY_TYPE_LABELS;
  protected readonly style = STATUS_STYLE;
  protected readonly statuses = Object.keys(INQUIRY_STATUS_LABELS) as InquiryStatus[];
  protected readonly q = signal<InquiryDetail | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly note = signal('');

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.run(() => this.api.detail(id)));
    });
  }

  protected setStatus(s: InquiryStatus): void {
    void this.run(() => this.api.setStatus(this.id(), s, this.note()), true);
  }

  protected addNote(): void {
    void this.run(() => this.api.addNote(this.id(), this.note()), true);
  }

  protected whatsapp(q: InquiryDetail): string | null {
    const to = toWhatsAppDigits(q.customerPhone);
    if (!to) return null;
    return whatsappLink(to, `Bonjour ${this.civ(q.customerCivility)} ${q.customerLastName}, ici Gezenthi Travel au sujet de votre demande ${q.reference} (${INQUIRY_TYPE_LABELS[q.type]} — ${q.subject}).`);
  }

  protected civ(c: string | null): string {
    return c === 'Mr' ? 'M.' : c === 'Ms' ? 'Mlle' : c === 'Mrs' ? 'Mme' : '';
  }

  private async run(action: () => Promise<InquiryDetail>, clearNote = false): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.q.set(await action());
      if (clearNote) this.note.set('');
    } catch (err) {
      this.error.set(ApiError.from(err).message);
    } finally {
      this.busy.set(false);
    }
  }
}
