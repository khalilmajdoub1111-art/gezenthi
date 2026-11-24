import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
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
            <input type="date" class="w-full bg-transparent text-sm font-semibold text-brand-950 focus:outline-none" />
          </span>
        </label>

        <label class="flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 ring-brand-200 transition focus-within:bg-white focus-within:ring-2">
          <app-icon name="calendar" [size]="20" class="text-brand-600" />
          <span class="flex-1">
            <span class="block text-[11px] font-bold uppercase tracking-wider text-slate-500">{{ isFlight() ? 'Retour' : 'Départ' }}</span>
            <input type="date" class="w-full bg-transparent text-sm font-semibold text-brand-950 focus:outline-none" />
          </span>
        </label>

        <label class="flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 ring-brand-200 transition focus-within:bg-white focus-within:ring-2">
          <app-icon name="users" [size]="20" class="text-brand-600" />
          <span class="flex-1">
            <span class="block text-[11px] font-bold uppercase tracking-wider text-slate-500">Voyageurs</span>
            <select class="w-full bg-transparent text-sm font-semibold text-brand-950 focus:outline-none">
              <option>1 adulte</option>
              <option selected>2 adultes</option>
              <option>2 adultes, 1 enfant</option>
              <option>2 adultes, 2 enfants</option>
              <option>Groupe (5+)</option>
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

      @if (notice()) {
        <p class="mx-3 mb-2 rounded-xl bg-brand-50 px-4 py-2 text-sm text-brand-800" role="status">
          Prototype : la recherche sera connectée au moteur de réservation lors de la prochaine étape.
        </p>
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

  private readonly activeId = signal('tn');
  protected readonly active = computed(() => this.tabs.find((t) => t.id === this.activeId())!);
  protected readonly isFlight = computed(() => this.activeId() === 'flights');
  protected readonly notice = signal(false);

  select(id: string): void {
    this.activeId.set(id);
    this.notice.set(false);
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    this.notice.set(true);
  }
}
