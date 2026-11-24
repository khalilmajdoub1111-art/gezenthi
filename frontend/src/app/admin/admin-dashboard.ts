import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogStore } from '../core/services/catalog-store';
import { Icon } from '../shared/icon/icon';
import { ADMIN_ENTITIES } from './admin-entities';

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div>
      <h1 class="font-display text-3xl font-bold text-brand-950">Tableau de bord</h1>
      <p class="mt-1 text-slate-500">Tout ce que vous publiez ici apparaît immédiatement sur le site.</p>
    </div>

    @if (store.loadError(); as err) {
      <div class="mt-6 flex flex-col gap-3 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200 sm:flex-row sm:items-center sm:justify-between" role="alert">
        <span>{{ err }}</span>
        <button type="button" (click)="reload()" class="inline-flex items-center gap-2 self-start rounded-lg bg-white px-3 py-1.5 font-semibold ring-1 ring-rose-200">
          <app-icon name="refresh" [size]="14" /> Réessayer
        </button>
      </div>
    }

    <div class="mt-8 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
      @for (c of cards(); track c.key) {
        <div class="flex flex-col rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
          <div class="flex items-start justify-between">
            <span class="grid size-12 place-items-center rounded-2xl bg-brand-50 text-brand-600"><app-icon [name]="c.icon" [size]="24" /></span>
            <span class="text-right">
              <span class="block font-display text-3xl font-bold text-brand-950">{{ store.loading() && !c.total ? '…' : c.total }}</span>
              <span class="text-xs text-slate-500">{{ c.published }} publié(s)</span>
            </span>
          </div>
          <h2 class="mt-4 font-display text-lg font-semibold text-brand-950">{{ c.label }}</h2>
          <p class="text-sm text-slate-500">{{ c.description }}</p>
          <div class="mt-5 flex gap-2">
            <a [routerLink]="['/admin', c.key]" class="flex-1 rounded-xl bg-slate-100 py-2.5 text-center text-sm font-semibold text-brand-900 transition hover:bg-slate-200">Gérer</a>
            <a [routerLink]="['/admin', c.key, 'new']" class="flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-700">
              <app-icon name="plus" [size]="16" [stroke]="2.4" /> Ajouter
            </a>
          </div>
        </div>
      }
    </div>
  `,
})
export class AdminDashboard {
  protected readonly store = inject(CatalogStore);

  protected readonly cards = computed(() =>
    ADMIN_ENTITIES.map((e) => {
      const items = this.store.list(e.key)() as { published: boolean }[];
      return { ...e, total: items.length, published: items.filter((i) => i.published).length };
    }),
  );

  constructor() {
    this.reload();
  }

  protected reload(): void {
    void this.store.refresh(...ADMIN_ENTITIES.map((e) => e.key));
  }
}
