import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { AGENCY_CONTACT } from '../../core/data/navigation';
import { HotelPage as HotelPageData, HotelResult, HotelSearchService } from '../../core/services/hotel-search.service';
import { InquiryService } from '../../core/services/inquiry.service';
import { Icon } from '../../shared/icon/icon';

/**
 * /hotel/:ref?checkIn&checkOut&adults&children
 * Hotel content (photos, description, map, facilities) + live rooms & prices for the chosen dates.
 */
@Component({
  selector: 'app-hotel-page',
  imports: [Icon, DecimalPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block bg-sand min-h-screen', '(document:keydown.escape)': 'lightbox.set(null)' },
  template: `
    @if (error()) {
      <div class="mx-auto max-w-3xl px-4 py-24 text-center">
        <p class="text-lg text-slate-600">{{ error() }}</p>
        <a routerLink="/" class="mt-6 inline-block font-semibold text-brand-600">← Retour à l’accueil</a>
      </div>
    } @else if (!hotel()) {
      <div class="grid place-items-center py-32 text-slate-500">
        <span class="size-10 animate-spin rounded-full border-4 border-brand-100 border-t-brand-600"></span>
      </div>
    } @else {
      @let h = hotel()!;
      <section class="mx-auto max-w-7xl px-4 pt-8 sm:px-6">
        <button type="button" (click)="back()" class="text-sm text-slate-500 hover:text-brand-600">← Retour</button>
        <div class="mt-3 flex flex-wrap items-end justify-between gap-4">
          <div>
            @if (h.stars) { <p class="text-gold-400">{{ '★'.repeat(h.stars) }}</p> }
            <h1 class="font-display text-3xl font-bold text-brand-950 sm:text-4xl">{{ h.name }}</h1>
            <p class="mt-1 flex items-center gap-1.5 text-slate-600">
              <app-icon name="pin" [size]="16" /> {{ h.address || h.city }}
            </p>
          </div>
          @if (h.origin === 'agence') {
            <span class="rounded-full bg-gold-400 px-4 py-1.5 text-sm font-bold text-brand-950">Sélection Gezenthi</span>
          }
        </div>

        <!-- Gallery -->
        <div class="mt-6 grid h-[260px] gap-2 overflow-hidden rounded-[28px] sm:h-[420px] sm:grid-cols-4 sm:grid-rows-2">
          @if (h.images.length) {
            @for (img of h.images.slice(0, 5); track img; let i = $index) {
              <button type="button" (click)="lightbox.set(i)"
                      class="group relative overflow-hidden bg-slate-200"
                      [class]="i === 0 ? 'sm:col-span-2 sm:row-span-2' : 'hidden sm:block'">
                <img [src]="img" [alt]="h.name + ' — photo ' + (i + 1)" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
                @if (i === 4 && h.images.length > 5) {
                  <span class="absolute inset-0 grid place-items-center bg-brand-950/60 font-semibold text-white">+{{ h.images.length - 5 }} photos</span>
                }
              </button>
            }
          } @else {
            <div class="grid place-items-center bg-slate-100 text-slate-300 sm:col-span-4 sm:row-span-2"><app-icon name="hotel" [size]="64" /></div>
          }
        </div>
      </section>

      <section class="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_400px]">
        <div class="space-y-8">
          @if (h.description) {
            <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100 sm:p-8">
              <h2 class="font-display text-xl font-semibold text-brand-950">À propos de l’hôtel</h2>
              <div class="prose-hotel mt-4 space-y-3 leading-relaxed text-slate-700" [innerHTML]="h.description"></div>
            </div>
          }

          @if (h.facilities.length || h.tags.length || h.themes.length) {
            <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100 sm:p-8">
              <h2 class="font-display text-xl font-semibold text-brand-950">Équipements & points forts</h2>
              <div class="mt-4 flex flex-wrap gap-2">
                @for (t of h.tags.concat(h.themes); track $index) {
                  <span class="rounded-full bg-gold-50 px-3 py-1.5 text-sm font-medium text-gold-600 ring-1 ring-gold-200">{{ t }}</span>
                }
                @for (f of h.facilities; track f) {
                  <span class="flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1.5 text-sm text-brand-800">
                    <app-icon name="check" [size]="14" [stroke]="2.4" /> {{ f }}
                  </span>
                }
              </div>
            </div>
          }

          <div class="grid gap-6 sm:grid-cols-2">
            @if (h.checkIn || h.checkOut || h.boards.length) {
              <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
                <h2 class="font-display text-lg font-semibold text-brand-950">Infos pratiques</h2>
                <ul class="mt-3 space-y-2 text-sm text-slate-700">
                  @if (h.checkIn) { <li><strong>Arrivée :</strong> à partir de {{ h.checkIn }}</li> }
                  @if (h.checkOut) { <li><strong>Départ :</strong> avant {{ h.checkOut }}</li> }
                  @if (h.boards.length) { <li><strong>Pensions :</strong> {{ h.boards.join(', ') }}</li> }
                </ul>
              </div>
            }
            @if (mapUrl(); as url) {
              <div class="overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-100">
                <iframe [src]="url" title="Carte" class="h-56 w-full border-0" loading="lazy"></iframe>
              </div>
            }
          </div>

          @if (h.notes) {
            <div class="rounded-3xl bg-white p-6 text-sm text-slate-600 shadow-sm ring-1 ring-slate-100" [innerHTML]="h.notes"></div>
          }
        </div>

        <!-- Availability -->
        <aside class="lg:sticky lg:top-24 lg:self-start">
          <div class="rounded-3xl bg-white p-6 shadow-lg shadow-brand-950/5 ring-1 ring-slate-100">
            <h2 class="font-display text-lg font-semibold text-brand-950">Prix & disponibilités</h2>
            <ol class="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
              <li><strong class="text-brand-700">1.</strong> Vos dates</li>
              <li><strong class="text-brand-700">2.</strong> Choisissez une chambre</li>
              <li><strong class="text-brand-700">3.</strong> Vos coordonnées — un conseiller confirme, aucun paiement en ligne</li>
            </ol>
            <form class="mt-4 grid grid-cols-2 gap-3" (submit)="$event.preventDefault(); checkAvailability()">
              <label class="text-xs font-bold uppercase tracking-wider text-slate-500">Arrivée
                <input type="date" [min]="today" [value]="dates().checkIn" (input)="setDate('checkIn', $any($event.target).value)"
                       class="mt-1 w-full rounded-xl bg-slate-50 px-3 py-2.5 text-sm font-semibold normal-case tracking-normal text-brand-950 ring-1 ring-slate-200" />
              </label>
              <label class="text-xs font-bold uppercase tracking-wider text-slate-500">Départ
                <input type="date" [min]="dates().checkIn || today" [value]="dates().checkOut" (input)="setDate('checkOut', $any($event.target).value)"
                       class="mt-1 w-full rounded-xl bg-slate-50 px-3 py-2.5 text-sm font-semibold normal-case tracking-normal text-brand-950 ring-1 ring-slate-200" />
              </label>
              <label class="col-span-2 text-xs font-bold uppercase tracking-wider text-slate-500">Adultes
                <select (change)="setAdults($any($event.target).value)"
                        class="mt-1 w-full rounded-xl bg-slate-50 px-3 py-2.5 text-sm font-semibold normal-case tracking-normal text-brand-950 ring-1 ring-slate-200">
                  @for (n of [1, 2, 3, 4]; track n) { <option [value]="n" [selected]="n === dates().adults">{{ n }} adulte(s)</option> }
                </select>
              </label>
              <button type="submit" [disabled]="checking()"
                      class="col-span-2 flex items-center justify-center gap-2 rounded-xl bg-brand-600 py-3 font-semibold text-white transition hover:bg-brand-700 disabled:opacity-60">
                <app-icon name="search" [size]="18" /> {{ checking() ? 'Recherche…' : 'Voir les prix' }}
              </button>
            </form>

            @if (availabilityError()) {
              <p class="mt-4 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ availabilityError() }}</p>
            } @else if (checked() && !offer()) {
              <div class="mt-4 rounded-xl bg-gold-50 px-4 py-4 text-sm text-slate-700 ring-1 ring-gold-200">
                <p>Pas de tarif en ligne pour ces dates : cet hôtel est <strong>disponible sur demande</strong>.</p>
                <button type="button" (click)="askQuote()"
                        class="mt-3 w-full rounded-xl bg-gold-400 py-2.5 font-semibold text-brand-950 hover:bg-gold-300">
                  Demander un devis pour cet hôtel
                </button>
              </div>
            }

            @if (offer(); as o) {
              <div class="mt-5 space-y-4">
                @for (b of o.boards; track $index; let bi = $index) {
                  <div>
                    <p class="text-xs font-bold uppercase tracking-wider text-gold-600">{{ b.name }}</p>
                    <ul class="mt-2 space-y-2">
                      @for (r of b.rooms; track $index; let ri = $index) {
                        <li class="rounded-2xl p-3 ring-1 ring-slate-100">
                          <div class="flex items-start justify-between gap-2">
                            <span class="text-sm font-medium text-brand-950">{{ r.name }}</span>
                            <strong class="whitespace-nowrap text-brand-700">{{ r.totalPrice | number: '1.0-0' }} {{ cur(o.currency) }}</strong>
                          </div>
                          <div class="mt-2 flex items-center justify-between gap-2">
                            <span class="text-xs" [class]="r.refundable ? 'text-emerald-600' : 'text-rose-600'">
                              {{ r.refundable ? 'Annulable' + (r.cancellationDeadline ? ' jusqu’au ' + r.cancellationDeadline : '') : 'Non remboursable' }}
                            </span>
                            @if (r.available) {
                              <a [routerLink]="['/reservation']" [queryParams]="{ offer: o.offerId, board: bi, room: ri }"
                                 class="rounded-full bg-gold-400 px-4 py-1.5 text-xs font-bold text-brand-950 transition hover:bg-gold-300">Réserver</a>
                            } @else {
                              <span class="text-xs text-slate-400">Sur demande</span>
                            }
                          </div>
                        </li>
                      }
                    </ul>
                  </div>
                }
                <p class="text-xs text-slate-500">Prix total du séjour, taxes incluses. Confirmation par un conseiller Gezenthi.</p>
              </div>
            }
            @if (!(checked() && !offer())) {
              <button type="button" (click)="askQuote()" class="mt-4 w-full text-center text-sm font-medium text-brand-600 hover:underline">
                Dates pas encore fixées ? Demandez un devis
              </button>
            }
          </div>
        </aside>
      </section>

      <!-- Lightbox -->
      @if (lightbox() !== null) {
        <div class="fixed inset-0 z-[70] grid place-items-center bg-brand-950/90 p-4" (click)="lightbox.set(null)">
          <img [src]="h.images[lightbox()!]" [alt]="h.name" class="max-h-[85vh] max-w-full rounded-2xl object-contain" (click)="$event.stopPropagation()" />
          <button type="button" class="absolute right-5 top-5 grid size-11 place-items-center rounded-full bg-white/10 text-white" aria-label="Fermer" (click)="lightbox.set(null)">
            <app-icon name="close" [size]="22" />
          </button>
          @if (h.images.length > 1) {
            <button type="button" class="absolute left-4 grid size-11 place-items-center rounded-full bg-white/10 text-white" aria-label="Photo précédente"
                    (click)="$event.stopPropagation(); step(-1, h.images.length)"><app-icon name="chevron-right" [size]="22" class="rotate-180" /></button>
            <button type="button" class="absolute right-4 grid size-11 place-items-center rounded-full bg-white/10 text-white" aria-label="Photo suivante"
                    (click)="$event.stopPropagation(); step(1, h.images.length)"><app-icon name="chevron-right" [size]="22" /></button>
            <p class="absolute bottom-5 text-sm text-white/70">{{ lightbox()! + 1 }} / {{ h.images.length }}</p>
          }
        </div>
      }
    }
  `,
})
export class HotelPage {
  readonly ref = input.required<string>();
  readonly checkInParam = input<string | undefined>(undefined, { alias: 'checkIn' });
  readonly checkOutParam = input<string | undefined>(undefined, { alias: 'checkOut' });
  readonly adultsParam = input<string | undefined>(undefined, { alias: 'adults' });
  readonly childrenParam = input<string | undefined>(undefined, { alias: 'children' });

  private readonly api = inject(HotelSearchService);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly inquiries = inject(InquiryService);
  protected readonly contact = AGENCY_CONTACT;
  protected readonly today = new Date().toISOString().slice(0, 10);

  protected readonly hotel = signal<HotelPageData | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly lightbox = signal<number | null>(null);
  protected readonly dates = signal({ checkIn: '', checkOut: '', adults: 2 });
  protected readonly offer = signal<HotelResult | null>(null);
  protected readonly checking = signal(false);
  protected readonly checked = signal(false);
  protected readonly availabilityError = signal<string | null>(null);

  /** OpenStreetMap embed from numeric coordinates only (validated before trusting the URL). */
  protected readonly mapUrl = computed<SafeResourceUrl | null>(() => {
    const h = this.hotel();
    const lat = Number(h?.latitude);
    const lon = Number(h?.longitude);
    if (!h?.latitude || !h?.longitude || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return null;
    const d = 0.012;
    const url = `https://www.openstreetmap.org/export/embed.html?bbox=${lon - d},${lat - d},${lon + d},${lat + d}&layer=mapnik&marker=${lat},${lon}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  });

  constructor() {
    effect(() => {
      const ref = this.ref();
      untracked(() => void this.load(ref));
    });
  }

  private async load(ref: string): Promise<void> {
    this.hotel.set(null);
    this.error.set(null);
    this.offer.set(null);
    this.checked.set(false);
    try {
      const hotel = await this.api.hotel(ref);
      this.hotel.set(hotel);
      // No dates in the URL: suggest some (in 30 days, 3 nights) so the visitor sees how it works.
      const fromUrl = !!(this.checkInParam() && this.checkOutParam());
      const checkIn = this.checkInParam() ?? isoDate(addDays(new Date(), 30));
      const checkOut = this.checkOutParam() ?? isoDate(addDays(new Date(checkIn + 'T00:00:00'), 3));
      this.dates.set({ checkIn, checkOut, adults: Math.min(4, Math.max(1, Number(this.adultsParam() ?? 2) || 2)) });
      // Agency hotels: show prices right away. Partner hotels: only when the visitor asks (supplier API call).
      if (fromUrl || hotel.origin === 'agence') await this.checkAvailability();
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Hôtel introuvable.');
    }
  }

  protected setDate(field: 'checkIn' | 'checkOut', value: string): void {
    this.dates.update((d) => {
      const next = { ...d, [field]: value };
      if (field === 'checkIn' && value && (!d.checkOut || d.checkOut <= value)) {
        const t = new Date(value + 'T00:00:00');
        t.setDate(t.getDate() + 3);
        next.checkOut = `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`;
      }
      return next;
    });
  }

  protected setAdults(value: string): void {
    this.dates.update((d) => ({ ...d, adults: Number(value) }));
  }

  protected async checkAvailability(): Promise<void> {
    const d = this.dates();
    if (!d.checkIn || !d.checkOut) {
      this.availabilityError.set('Choisissez vos dates.');
      return;
    }
    this.checking.set(true);
    this.availabilityError.set(null);
    try {
      const children = (this.childrenParam() ?? '').split(',').filter(Boolean).map(Number);
      this.offer.set(await this.api.availability(this.ref(), { ...d, children }));
      this.checked.set(true);
    } catch (err) {
      this.availabilityError.set(err instanceof Error ? err.message : 'Recherche impossible.');
    } finally {
      this.checking.set(false);
    }
  }

  /** Quote request for this hotel, with the dates/travellers currently selected. */
  protected askQuote(): void {
    const h = this.hotel();
    if (!h) return;
    const d = this.dates();
    this.inquiries.open({
      type: 'HOTEL', subject: `${h.name} — ${h.city}`, offerRef: h.ref,
      dateFrom: d.checkIn || null, dateTo: d.checkOut || null, adults: d.adults,
      children: (this.childrenParam() ?? '').split(',').filter(Boolean).length,
    });
  }

  protected step(delta: number, count: number): void {
    this.lightbox.update((i) => (i === null ? 0 : (i + delta + count) % count));
  }

  protected cur(c: string): string {
    return c === 'TND' ? 'DT' : c;
  }

  protected back(): void {
    history.length > 1 ? history.back() : void this.router.navigateByUrl('/');
  }
}

function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

/** Local date as YYYY-MM-DD (no timezone shift). */
function isoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}