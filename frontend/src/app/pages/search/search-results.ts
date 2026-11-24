import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AGENCY_CONTACT } from '../../core/data/navigation';
import { HotelResult, HotelSearchService, SearchResponse } from '../../core/services/hotel-search.service';
import { Icon } from '../../shared/icon/icon';
import { HeroSearch } from '../home/hero-search/hero-search';

/**
 * /recherche?city=…&checkIn=…&checkOut=…&adults=…&children=8,12
 * Shows the unified results (agency offers + live partner offers). Booking is "on request" for now.
 */
@Component({
  selector: 'app-search-results',
  imports: [Icon, DecimalPipe, DatePipe, RouterLink, HeroSearch],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block bg-sand min-h-screen' },
  template: `
    <section class="bg-brand-950 pb-10 pt-8">
      <div class="mx-auto max-w-7xl px-4 sm:px-6">
        <a routerLink="/" class="text-sm text-brand-200 hover:text-gold-300">← Accueil</a>
        <h1 class="mt-2 font-display text-3xl font-bold text-white">
          Hôtels à {{ city() || '…' }}
        </h1>
        @if (result(); as r) {
          <p class="mt-1 text-brand-200">
            {{ r.checkIn | date: 'd MMM' }} → {{ r.checkOut | date: 'd MMM y' }} · {{ r.nights }} nuit(s) · {{ adults() }} adulte(s)
            @if (childAges().length) { · {{ childAges().length }} enfant(s) }
          </p>
        }
        <div class="mt-6"><app-hero-search /></div>
      </div>
    </section>

    <section class="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      @if (loading()) {
        <div class="grid place-items-center gap-3 py-20 text-slate-500">
          <span class="size-10 animate-spin rounded-full border-4 border-brand-100 border-t-brand-600"></span>
          Recherche des meilleures offres…
        </div>
      } @else if (error()) {
        <p class="rounded-2xl bg-rose-50 px-4 py-3 text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</p>
      } @else if (result(); as r) {
        @for (w of r.warnings; track w) {
          <p class="mb-4 rounded-2xl bg-gold-50 px-4 py-3 text-sm text-slate-700 ring-1 ring-gold-200">{{ w }}</p>
        }
        <p class="mb-6 text-slate-600"><strong class="text-brand-950">{{ r.hotels.length }}</strong> hôtel(s) disponible(s)</p>

        <div class="space-y-5">
          @for (h of r.hotels; track h.offerId) {
            <article class="grid overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-100 md:grid-cols-[280px_1fr]">
              <div class="relative aspect-[4/3] bg-slate-100 md:aspect-auto">
                @if (h.image) {
                  <img [src]="h.image" [alt]="h.name" loading="lazy" class="absolute inset-0 h-full w-full object-cover" />
                } @else {
                  <div class="absolute inset-0 grid place-items-center text-slate-300"><app-icon name="hotel" [size]="48" /></div>
                }
                @if (h.origin === 'agence') {
                  <span class="absolute left-3 top-3 rounded-full bg-gold-400 px-3 py-1 text-xs font-bold text-brand-950">Sélection Gezenthi</span>
                }
              </div>
              <div class="flex flex-col p-6">
                <div class="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    @if (h.stars) {
                      <p class="text-gold-400">{{ '★'.repeat(h.stars) }}</p>
                    }
                    <h2 class="font-display text-xl font-semibold text-brand-950">{{ h.name }}</h2>
                    <p class="flex items-center gap-1 text-sm text-slate-500"><app-icon name="pin" [size]="14" /> {{ h.city }}</p>
                  </div>
                  <div class="text-right">
                    <p class="text-xs text-slate-500">Total séjour dès</p>
                    <p class="font-display text-2xl font-bold text-brand-700">{{ h.fromPrice | number: '1.0-0' }} {{ h.currency === 'TND' ? 'DT' : h.currency }}</p>
                  </div>
                </div>

                <button type="button" (click)="toggle(h.offerId)" class="mt-4 self-start text-sm font-semibold text-brand-600 hover:underline">
                  {{ open() === h.offerId ? 'Masquer les chambres' : 'Voir les chambres et pensions (' + roomCount(h) + ')' }}
                </button>

                @if (open() === h.offerId) {
                  <div class="mt-4 space-y-4">
                    @for (b of h.boards; track $index) {
                      <div>
                        <p class="text-xs font-bold uppercase tracking-wider text-gold-600">{{ b.name }}</p>
                        <ul class="mt-2 divide-y divide-slate-100 rounded-2xl ring-1 ring-slate-100">
                          @for (room of b.rooms; track $index) {
                            <li class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm">
                              <span class="font-medium text-brand-950">{{ room.name }}</span>
                              <span class="flex items-center gap-3">
                                <span class="text-xs" [class]="room.refundable ? 'text-emerald-600' : 'text-rose-600'">
                                  {{ room.refundable ? 'Annulable' + (room.cancellationDeadline ? ' jusqu’au ' + room.cancellationDeadline : '') : 'Non remboursable' }}
                                </span>
                                @if (!room.available) { <span class="text-xs text-slate-400">Complet</span> }
                                <strong class="text-brand-700">{{ room.totalPrice | number: '1.0-0' }} {{ h.currency === 'TND' ? 'DT' : h.currency }}</strong>
                              </span>
                            </li>
                          }
                        </ul>
                      </div>
                    }
                  </div>
                }

                <div class="mt-auto flex flex-wrap items-center justify-between gap-3 pt-5">
                  <p class="text-xs text-slate-500">Réservation confirmée par un conseiller Gezenthi.</p>
                  <a [href]="contact.phoneHref" class="inline-flex items-center gap-2 rounded-full bg-gold-400 px-5 py-2.5 text-sm font-semibold text-brand-950 transition hover:bg-gold-300">
                    <app-icon name="phone" [size]="16" /> Demander cette offre
                  </a>
                </div>
              </div>
            </article>
          } @empty {
            <div class="rounded-3xl bg-white p-12 text-center text-slate-500">
              Aucune disponibilité trouvée pour ces dates. Essayez d’autres dates ou contactez-nous au {{ contact.phone }}.
            </div>
          }
        </div>
      }
    </section>
  `,
})
export class SearchResults {
  /** Query params bound by the router. */
  readonly city = input<string>('');
  readonly checkIn = input<string>('');
  readonly checkOut = input<string>('');
  readonly adultsParam = input<string>('2', { alias: 'adults' });
  readonly childrenParam = input<string>('', { alias: 'children' });

  private readonly searchApi = inject(HotelSearchService);
  protected readonly contact = AGENCY_CONTACT;
  protected readonly result = signal<SearchResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly open = signal<string | null>(null);

  protected readonly adults = computed(() => Math.min(6, Math.max(1, Number(this.adultsParam()) || 2)));
  protected readonly childAges = computed(() =>
    this.childrenParam().split(',').map((s) => s.trim()).filter(Boolean).map(Number).filter((n) => Number.isInteger(n)),
  );

  constructor() {
    effect(() => {
      const criteria = {
        city: this.city(),
        checkIn: this.checkIn(),
        checkOut: this.checkOut(),
        adults: this.adults(),
        children: this.childAges(),
      };
      if (!criteria.city || !criteria.checkIn || !criteria.checkOut) {
        this.error.set('Choisissez une destination et des dates.');
        return;
      }
      void this.run(criteria);
    });
  }

  private async run(criteria: Parameters<HotelSearchService['search']>[0]): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    this.open.set(null);
    try {
      this.result.set(await this.searchApi.search(criteria));
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Recherche impossible.');
    } finally {
      this.loading.set(false);
    }
  }

  protected toggle(id: string): void {
    this.open.update((o) => (o === id ? null : id));
  }

  protected roomCount(h: HotelResult): number {
    return h.boards.reduce((n, b) => n + b.rooms.length, 0);
  }
}
