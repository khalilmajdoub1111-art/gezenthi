import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CatalogKey } from '../core/models/travel.models';
import { CatalogStore } from '../core/services/catalog-store';
import { Icon } from '../shared/icon/icon';
import { ColumnDef, findEntity } from './admin-entities';

type Row = Record<string, unknown> & { id: string; published: boolean };

@Component({
  selector: 'app-entity-list',
  imports: [RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    @if (config(); as cfg) {
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <a routerLink="/admin" class="text-sm text-slate-500 hover:text-brand-600">← Tableau de bord</a>
          <h1 class="mt-1 font-display text-3xl font-bold text-brand-950">{{ cfg.label }}</h1>
          <p class="mt-1 text-slate-500">{{ cfg.description }} · {{ rows().length }} au total</p>
        </div>
        <a [routerLink]="['/admin', cfg.key, 'new']"
           class="inline-flex items-center gap-2 self-start rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white shadow-lg shadow-brand-600/25 transition hover:bg-brand-700">
          <app-icon name="plus" [size]="18" [stroke]="2.4" /> Ajouter un {{ cfg.singular }}
        </a>
      </div>

      @if (flash()) {
        <div class="mt-6 flex items-center gap-2 rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700 ring-1 ring-emerald-200" role="status">
          <app-icon name="check" [size]="18" [stroke]="2.4" /> {{ flash() }}
        </div>
      }
      @if (error()) {
        <div class="mt-6 rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</div>
      }

      <div class="mt-6 flex items-center gap-3 rounded-2xl bg-white px-4 py-3 ring-1 ring-slate-200 focus-within:ring-2 focus-within:ring-brand-300">
        <app-icon name="search" [size]="18" class="text-slate-400" />
        <input type="search" placeholder="Rechercher…" [value]="query()" (input)="query.set($any($event.target).value)"
               class="flex-1 bg-transparent text-sm focus:outline-none" />
      </div>

      <div class="mt-4 overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-100">
        @for (row of filtered(); track row.id) {
          <div class="flex flex-col gap-4 border-b border-slate-100 p-4 last:border-0 md:flex-row md:items-center">
            <div class="flex min-w-0 flex-1 items-center gap-4">
              @if (cfg.imageField) {
                @if (row[cfg.imageField]) {
                  <img [src]="row[cfg.imageField]" alt="" class="size-16 shrink-0 rounded-xl object-cover" />
                } @else {
                  <span class="grid size-16 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-400"><app-icon [name]="cfg.icon" [size]="22" /></span>
                }
              }
              <div class="min-w-0">
                <p class="truncate font-semibold text-brand-950">{{ row[cfg.titleField] }}</p>
                <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500">
                  @for (col of cfg.columns; track col.key) {
                    <span><span class="text-slate-400">{{ col.label }} :</span> {{ format(row[col.key], col) }}</span>
                  }
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button type="button" (click)="togglePublished(row)"
                      class="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition"
                      [class]="row.published ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'"
                      [attr.aria-label]="(row.published ? 'Masquer ' : 'Publier ') + row[cfg.titleField]">
                <app-icon [name]="row.published ? 'eye' : 'eye-off'" [size]="14" /> {{ row.published ? 'Publié' : 'Masqué' }}
              </button>
              <a [routerLink]="['/admin', cfg.key, row.id]" class="grid size-9 place-items-center rounded-xl text-slate-500 transition hover:bg-brand-50 hover:text-brand-600" [attr.aria-label]="'Modifier ' + row[cfg.titleField]">
                <app-icon name="edit" [size]="18" />
              </a>
              <button type="button" (click)="remove(row)" class="grid size-9 place-items-center rounded-xl text-slate-500 transition hover:bg-rose-50 hover:text-rose-600" [attr.aria-label]="'Supprimer ' + row[cfg.titleField]">
                <app-icon name="trash" [size]="18" />
              </button>
            </div>
          </div>
        } @empty {
          <div class="p-12 text-center text-slate-500">
            {{ query() ? 'Aucun résultat pour « ' + query() + ' ».' : 'Aucune offre pour le moment.' }}
          </div>
        }
      </div>
    }
  `,
})
export class EntityList {
  /** Bound from the :entity route parameter. */
  readonly entity = input.required<string>();

  private readonly store = inject(CatalogStore);
  private readonly router = inject(Router);

  protected readonly config = computed(() => findEntity(this.entity()));
  protected readonly rows = computed(() => {
    const cfg = this.config();
    return cfg ? (this.store.list(cfg.key)() as unknown as Row[]) : [];
  });
  protected readonly query = signal('');
  protected readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.rows();
    return this.rows().filter((r) => Object.values(r).some((v) => typeof v === 'string' && !v.startsWith('data:') && v.toLowerCase().includes(q)));
  });
  protected readonly flash = signal<string | null>((history.state as { flash?: string } | null)?.flash ?? null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (!this.config()) void this.router.navigateByUrl('/admin');
    });
  }

  protected format(value: unknown, col: ColumnDef): string {
    if (value === undefined || value === null || value === '') return '—';
    switch (col.format) {
      case 'price': return `${value} DT`;
      case 'stars': return '★'.repeat(Number(value));
      case 'days': return `${value} jours`;
      default: return String(value);
    }
  }

  protected togglePublished(row: Row): void {
    this.write(() => this.store.save(this.key(), { ...row, published: !row.published } as never));
  }

  protected remove(row: Row): void {
    const title = String(row[this.config()!.titleField]);
    if (confirm(`Supprimer « ${title} » ? Cette action est définitive.`)) {
      this.write(() => this.store.remove(this.key(), row.id));
      this.flash.set(`« ${title} » a été supprimé.`);
    }
  }

  private key(): CatalogKey {
    return this.config()!.key;
  }

  private write(action: () => void): void {
    this.error.set(null);
    try {
      action();
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Enregistrement impossible.');
    }
  }
}
