import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService, BookingStatus, BookingSummary, STATUS_LABELS } from '../core/services/booking.service';
import { ApiError } from '../core/services/api-error';

export const STATUS_STYLE: Record<BookingStatus, string> = {
  NEW: 'bg-gold-100 text-gold-600',
  IN_PROGRESS: 'bg-brand-50 text-brand-700',
  PRICE_CHECKED: 'bg-sky-50 text-sky-700',
  CONFIRMED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-slate-100 text-slate-500',
  REJECTED: 'bg-rose-50 text-rose-700',
};

/** Admin: booking requests list with status filter. */
@Component({
  selector: 'app-admin-bookings',
  imports: [RouterLink, DatePipe, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <h1 class="font-display text-3xl font-bold text-brand-950">Réservations</h1>
    <p class="mt-1 text-slate-500">Demandes envoyées depuis le site. Données personnelles : accès réservé à l’administration.</p>

    <div class="no-scrollbar mt-6 flex gap-2 overflow-x-auto">
      @for (f of filters; track f.label) {
        <button type="button" (click)="filter(f.value)"
                class="shrink-0 rounded-full px-4 py-1.5 text-sm font-semibold transition"
                [class]="active() === f.value ? 'bg-brand-600 text-white' : 'bg-white text-brand-900 ring-1 ring-slate-200 hover:ring-brand-300'">{{ f.label }}</button>
      }
    </div>

    @if (error()) {
      <p class="mt-6 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">{{ error() }}</p>
    }

    <div class="mt-6 overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-100">
      @for (b of rows(); track b.id) {
        <a [routerLink]="['/admin/reservations', b.id]" class="flex flex-col gap-2 border-b border-slate-100 p-4 transition last:border-0 hover:bg-slate-50 md:flex-row md:items-center md:gap-6">
          <div class="md:w-40">
            <p class="font-mono text-sm font-semibold text-brand-950">{{ b.reference }}</p>
            <p class="text-xs text-slate-400">{{ b.createdAt | date: 'dd/MM/yyyy HH:mm' }}</p>
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate font-semibold text-brand-950">{{ b.customerName }}</p>
            <p class="truncate text-sm text-slate-500">{{ b.hotelName }} · {{ b.city }} · {{ b.checkIn | date: 'dd/MM' }} → {{ b.checkOut | date: 'dd/MM/yy' }}</p>
          </div>
          <span class="text-xs font-semibold" [class]="b.origin === 'partenaire' ? 'text-sky-700' : 'text-gold-600'">{{ b.origin === 'partenaire' ? 'Partenaire' : 'Agence' }}</span>
          <strong class="text-brand-700 md:w-28 md:text-right">{{ b.totalPrice | number: '1.0-0' }} {{ b.currency === 'TND' ? 'DT' : b.currency }}</strong>
          <span class="self-start rounded-full px-3 py-1 text-xs font-semibold md:self-auto" [class]="style[b.status]">{{ labels[b.status] }}</span>
        </a>
      } @empty {
        <p class="p-12 text-center text-slate-500">{{ loading() ? 'Chargement…' : 'Aucune demande pour le moment.' }}</p>
      }
    </div>
  `,
})
export class AdminBookings {
  private readonly api = inject(BookingService);
  protected readonly labels = STATUS_LABELS;
  protected readonly style = STATUS_STYLE;
  protected readonly rows = signal<BookingSummary[]>([]);
  protected readonly active = signal<BookingStatus | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filters: { label: string; value: BookingStatus | null }[] = [
    { label: 'Toutes', value: null },
    ...(Object.keys(STATUS_LABELS) as BookingStatus[]).map((s) => ({ label: STATUS_LABELS[s], value: s })),
  ];

  constructor() {
    void this.load();
  }

  protected filter(s: BookingStatus | null): void {
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
