import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { IconName } from '../../../core/models/travel.models';
import { Icon } from '../../../shared/icon/icon';

interface SearchTab {
  id: string;
  label: string;
  icon: IconName;
  wherePlaceholder: string;
  suggestions: string[];
}

/**
 * Search widget (UI only). Nothing is sent anywhere yet: submit is intercepted
 * and only shows a notice. It will call the booking API once the backend exists.
 */
@Component({
  selector: 'app-hero-search',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="rounded-[28px] bg-white/95 p-2 shadow-[0_30px_80px_-20px_rgb(7,31,55,0.45)] ring-1 ring-white/60 backdrop-blur-xl">
      <div class="no-scrollbar flex gap-1 overflow-x-auto p-1" role="tablist">
        @for (tab of tabs; track tab.id) {
          <button
            type="button"
            role="tab"
            [attr.aria-selected]="active().id === tab.id"
            (click)="select(tab.id)"
            class="flex shrink-0 items-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-semibold transition"
            [class]="active().id === tab.id ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30' : 'text-brand-900 hover:bg-brand-50'"
          >
            <app-icon [name]="tab.icon" [size]="18" /> {{ tab.label }}
          </button>
        }
      </div>

      <form class="grid gap-2 p-2 md:grid-cols-[1.6fr_1fr_1fr_1fr_auto]" (submit)="onSubmit($event)">
        <label class="group flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 ring-brand-200 transition focus-within:bg-white focus-within:ring-2">
          <app-icon name="pin" [size]="22" class="text-brand-600" />
          <span class="flex-1">
            <span class="block text-[11px] font-bold uppercase tracking-wider text-slate-500">Destination</span>
            <input
              [attr.list]="'dest-' + active().id"
              [placeholder]="active().wherePlaceholder"
              [value]="destination()"
              (input)="destination.set($any($event.target).value)"
              autocomplete="off"
              class="w-full bg-transparent text-[15px] font-semibold text-brand-950 placeholder:font-medium placeholder:text-slate-400 focus:outline-none"
            />
            <datalist [id]="'dest-' + active().id">
              @for (s of active().suggestions; track s) {
                <option [value]="s"></option>
              }
            </datalist>
          </span>
        </label>

        <label class="flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 ring-brand-200 transition focus-within:bg-white focus-within:ring-2">
          <app-icon name="calendar" [size]="20" class="text-brand-600" />
          <span class="flex-1">
            <span class="block text-[11px] font-bold uppercase tracking-wider text-slate-500">{{ isFlight() ? 'Aller' : 'Arrivée' }}</span>
            <input type="date" [min]="today" [value]="checkIn()" (input)="setCheckIn($any($event.target).value)"
                   class="w-full bg-transparent text-sm font-semibold text-brand-950 focus:outline-none" />
          </span>
        </label>

        <label class="flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 ring-brand-200 transition focus-within:bg-white focus-within:ring-2">
          <app-icon name="calendar" [size]="20" class="text-brand-600" />
          <span class="flex-1">
            <span class="block text-[11px] font-bold uppercase tracking-wider text-slate-500">{{ isFlight() ? 'Retour' : 'Départ' }}</span>
            <input type="date" [min]="checkIn() || today" [value]="checkOut()" (input)="checkOut.set($any($event.target).value)"
                   class="w-full bg-transparent text-sm font-semibold text-brand-950 focus:outline-none" />
          </span>
        </label>

        <label class="flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 ring-brand-200 transition focus-within:bg-white focus-within:ring-2">
          <app-icon name="users" [size]="20" class="text-brand-600" />
          <span class="flex-1">
            <span class="block text-[11px] font-bold uppercase tracking-wider text-slate-500">Voyageurs</span>
            <select (change)="travellers.set($any($event.target).value)" class="w-full bg-transparent text-sm font-semibold text-brand-950 focus:outline-none">
              @for (t of travellerOptions; track t.value) {
                <option [value]="t.value" [selected]="t.value === travellers()">{{ t.label }}</option>
              }
            </select>
          </span>
        </label>

        <button
          type="submit"
          class="flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-br from-gold-300 to-gold-400 px-7 py-4 font-display font-bold text-brand-950 shadow-lg shadow-gold-400/40 transition hover:-translate-y-0.5 hover:shadow-xl hover:shadow-gold-400/50"
        >
          <app-icon name="search" [size]="20" [stroke]="2.4" /> Rechercher
        </button>
      </form>

      @if (notice(); as n) {
        <p class="mx-3 mb-2 rounded-xl bg-brand-50 px-4 py-2 text-sm text-brand-800" role="status">{{ n }}</p>
      }
    </div>
  `,
})
export class HeroSearch {
  protected readonly tabs: SearchTab[] = [
    { id: 'tn', label: 'Hôtels Tunisie', icon: 'beach', wherePlaceholder: 'Hammamet, Djerba, Sousse…',
      suggestions: ['Hammamet', 'Djerba', 'Sousse', 'Monastir', 'Mahdia', 'Tunis', 'Tabarka', 'Tozeur'] },
    { id: 'world', label: 'Hôtels monde', icon: 'hotel', wherePlaceholder: 'Ville ou hôtel',
      suggestions: ['Istanbul', 'Paris', 'Dubaï', 'Rome', 'Barcelone', 'Le Caire'] },
    { id: 'trips', label: 'Voyages organisés', icon: 'globe', wherePlaceholder: 'Turquie, Égypte, Asie…',
      suggestions: ['Turquie', 'Égypte', 'Maroc', 'Thaïlande', 'Europe'] },
    { id: 'flights', label: 'Vols', icon: 'plane', wherePlaceholder: 'Tunis → où ?',
      suggestions: ['Paris', 'Istanbul', 'Dubaï', 'Doha', 'Rome', 'Montréal'] },
    { id: 'omra', label: 'Omra', icon: 'moon', wherePlaceholder: 'Programme Omra',
      suggestions: ['Omra 15 jours', 'Omra Ramadan', 'Omra économique'] },
  ];

  /** value = "adults" or "adults-childAge,childAge" (child ages are approximated until age pickers exist). */
  protected readonly travellerOptions = [
    { value: '1', label: '1 adulte' },
    { value: '2', label: '2 adultes' },
    { value: '3', label: '3 adultes' },
    { value: '2-8', label: '2 adultes, 1 enfant' },
    { value: '2-8,5', label: '2 adultes, 2 enfants' },
  ];

  /** Pre-fills the form (e.g. on the results page) with the current search. */
  readonly initial = input<{ city: string; checkIn: string; checkOut: string; adults: number; children: number[] } | null>(null);

  private readonly router = inject(Router);
  protected readonly today = isoDate(new Date());
  private readonly activeId = signal('tn');
  protected readonly active = computed(() => this.tabs.find((t) => t.id === this.activeId())!);
  protected readonly isFlight = computed(() => this.activeId() === 'flights');
  protected readonly notice = signal<string | null>(null);
  protected readonly destination = signal('');
  protected readonly checkIn = signal('');
  protected readonly checkOut = signal('');
  protected readonly travellers = signal('2');

  constructor() {
    effect(() => {
      const init = this.initial();
      if (!init) return;
      untracked(() => {
        this.destination.set(init.city);
        this.checkIn.set(init.checkIn);
        this.checkOut.set(init.checkOut);
        const value = init.children.length ? `${init.adults}-${init.children.join(',')}` : String(init.adults);
        if (!this.travellerOptions.some((o) => o.value === value)) {
          this.travellerOptions.push({ value, label: `${init.adults} adulte(s)${init.children.length ? ', ' + init.children.length + ' enfant(s)' : ''}` });
        }
        this.travellers.set(value);
      });
    });
  }

  select(id: string): void {
    this.activeId.set(id);
    this.notice.set(null);
  }

  protected setCheckIn(value: string): void {
    this.checkIn.set(value);
    // Default stay: 7 nights, and never a departure before the arrival.
    if (value && (!this.checkOut() || this.checkOut() <= value)) {
      const d = new Date(value + 'T00:00:00');
      d.setDate(d.getDate() + 7);
      this.checkOut.set(isoDate(d));
    }
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (this.activeId() !== 'tn' && this.activeId() !== 'world') {
      this.notice.set('Recherche en ligne bientôt disponible pour cette rubrique : contactez-nous pour un devis.');
      return;
    }
    if (!this.destination().trim() || !this.checkIn() || !this.checkOut()) {
      this.notice.set('Choisissez une destination, une date d’arrivée et une date de départ.');
      return;
    }
    const [adults, children = ''] = this.travellers().split('-');
    void this.router.navigate(['/recherche'], {
      queryParams: { city: this.destination().trim(), checkIn: this.checkIn(), checkOut: this.checkOut(), adults, children: children || null },
    });
  }
}

/** Local date as YYYY-MM-DD (no timezone shift). */
function isoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
