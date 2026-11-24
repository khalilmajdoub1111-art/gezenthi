import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Hotel } from '../../core/models/travel.models';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-hotel-card',
  imports: [Icon, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <article
      class="group flex h-full flex-col overflow-hidden rounded-3xl bg-white shadow-[0_8px_30px_rgb(12,52,89,0.08)] ring-1 ring-slate-100 transition duration-500 hover:-translate-y-1.5 hover:shadow-[0_20px_50px_rgb(12,52,89,0.18)]"
    >
      <div class="relative aspect-[4/3] overflow-hidden">
        <img
          [src]="hotel().image"
          [alt]="hotel().name"
          loading="lazy"
          class="h-full w-full object-cover transition duration-700 group-hover:scale-110"
        />
        <div class="absolute inset-0 bg-gradient-to-t from-brand-950/60 via-transparent to-transparent"></div>
        @if (discount()) {
          <span class="absolute left-4 top-4 rounded-full bg-gold-400 px-3 py-1 text-xs font-bold text-brand-950 shadow">
            -{{ discount() }}%
          </span>
        }
        <button
          type="button"
          aria-label="Ajouter aux favoris"
          class="absolute right-4 top-4 grid size-9 place-items-center rounded-full bg-white/85 text-brand-700 backdrop-blur transition hover:bg-white hover:text-rose-500"
        >
          <app-icon name="heart" [size]="18" />
        </button>
        <div class="absolute bottom-4 left-4 flex items-center gap-1.5 text-sm font-medium text-white">
          <app-icon name="pin" [size]="16" /> {{ hotel().city }}
        </div>
      </div>

      <div class="flex flex-1 flex-col p-5">
        <div class="flex items-center gap-0.5 text-gold-400">
          @for (s of starArray(); track $index) {
            <svg viewBox="0 0 24 24" class="size-4 fill-current"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
          }
        </div>
        <h3 class="mt-2 font-display text-lg font-semibold leading-snug text-brand-950">{{ hotel().name }}</h3>

        <div class="mt-2 flex items-center gap-2 text-sm">
          <span class="rounded-lg bg-brand-600 px-2 py-0.5 font-bold text-white">{{ hotel().rating | number: '1.1-1' }}</span>
          <span class="font-medium text-slate-700">{{ ratingLabel() }}</span>
          <span class="text-slate-400">· {{ hotel().reviews | number: '1.0-0' }} avis</span>
        </div>

        <div class="mt-3 flex flex-wrap gap-1.5">
          <span class="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-medium text-brand-700">{{ hotel().board }}</span>
          @for (tag of hotel().tags; track tag) {
            <span class="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">{{ tag }}</span>
          }
        </div>

        <div class="mt-auto flex items-end justify-between pt-5">
          <div>
            <p class="text-xs text-slate-500">À partir de</p>
            <p class="font-display text-2xl font-bold text-brand-700">
              {{ hotel().pricePerNight }} <span class="text-sm font-semibold">DT</span>
              @if (hotel().oldPrice) {
                <span class="ml-1 text-sm font-medium text-slate-400 line-through">{{ hotel().oldPrice }} DT</span>
              }
            </p>
            <p class="text-xs text-slate-500">par nuit / personne</p>
          </div>
          <a
            href="#hotels-tunisie"
            class="grid size-11 place-items-center rounded-full bg-gold-400 text-brand-950 transition group-hover:bg-brand-600 group-hover:text-white"
            [attr.aria-label]="'Voir ' + hotel().name"
          >
            <app-icon name="arrow-right" [size]="20" />
          </a>
        </div>
      </div>
    </article>
  `,
})
export class HotelCard {
  readonly hotel = input.required<Hotel>();

  protected readonly starArray = computed(() => Array.from({ length: this.hotel().stars }));
  protected readonly discount = computed(() => {
    const { oldPrice, pricePerNight } = this.hotel();
    return oldPrice ? Math.round((1 - pricePerNight / oldPrice) * 100) : 0;
  });
  protected readonly ratingLabel = computed(() => {
    const r = this.hotel().rating;
    return r >= 9 ? 'Exceptionnel' : r >= 8.5 ? 'Fabuleux' : r >= 8 ? 'Très bien' : 'Bien';
  });
}
