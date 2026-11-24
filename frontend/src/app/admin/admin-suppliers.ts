import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../core/services/api-error';
import { Icon } from '../shared/icon/icon';

interface SupplierStatus {
  code: string;
  name: string;
  type: string;
  configured: boolean;
  enabled: boolean;
  bookingEnabled: boolean;
  markupPercent: number;
  countries: string[];
  cachedHotels: number;
  lastSync: { status: string; hotels: number; message: string; started_at: string; triggered_by: string } | null;
}

/** Back-office: hotel suppliers (B2B APIs) — status, connection test, static data sync. */
@Component({
  selector: 'app-admin-suppliers',
  imports: [Icon, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <h1 class="font-display text-3xl font-bold text-brand-950">Fournisseurs</h1>
    <p class="mt-1 max-w-2xl text-slate-500">
      Les API des fournisseurs (TunisiaBeds…) alimentent la recherche d’hôtels avec des prix en temps réel.
      Les identifiants se configurent dans le fichier privé du serveur, jamais ici.
    </p>

    @if (loadError()) {
      <p class="mt-6 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200" role="alert">{{ loadError() }}</p>
    }

    <div class="mt-8 space-y-5">
      @for (s of suppliers(); track s.code) {
        <section class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
          <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
            <div>
              <h2 class="font-display text-xl font-semibold text-brand-950">{{ s.name }}</h2>
              <p class="text-sm text-slate-500">API {{ s.type }} · pays : {{ s.countries.join(', ') }} · marge {{ s.markupPercent }} %</p>
              <div class="mt-3 flex flex-wrap gap-2 text-xs font-semibold">
                <span class="rounded-full px-3 py-1" [class]="s.configured ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'">
                  {{ s.configured ? 'Identifiants configurés' : 'Identifiants manquants' }}
                </span>
                <span class="rounded-full px-3 py-1" [class]="s.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                  {{ s.enabled ? 'Actif dans la recherche' : 'Désactivé dans la recherche' }}
                </span>
                <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-600">{{ s.cachedHotels }} hôtels synchronisés</span>
                <span class="rounded-full px-3 py-1" [class]="s.bookingEnabled ? 'bg-gold-100 text-gold-600' : 'bg-slate-100 text-slate-500'">
                  Réservation API : {{ s.bookingEnabled ? 'activée' : 'désactivée (sécurité)' }}
                </span>
              </div>
            </div>
            <div class="flex shrink-0 gap-2">
              <button type="button" (click)="run(s.code, 'test')" [disabled]="busy() !== null"
                      class="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold text-brand-900 ring-1 ring-slate-200 transition hover:bg-slate-50 disabled:opacity-50">
                <app-icon name="shield" [size]="16" /> {{ busy() === s.code + 'test' ? 'Test…' : 'Tester la connexion' }}
              </button>
              <button type="button" (click)="run(s.code, 'sync')" [disabled]="busy() !== null"
                      class="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-700 disabled:opacity-50">
                <app-icon name="refresh" [size]="16" /> {{ busy() === s.code + 'sync' ? 'Synchronisation…' : 'Synchroniser les hôtels' }}
              </button>
            </div>
          </div>

          @if (messages()[s.code]; as m) {
            <p class="mt-4 rounded-2xl px-4 py-3 text-sm ring-1" role="status"
               [class]="m.ok ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' : 'bg-rose-50 text-rose-700 ring-rose-200'">{{ m.message }}</p>
          }
          @if (s.lastSync; as l) {
            <p class="mt-4 text-xs text-slate-500">
              Dernière synchronisation : {{ l.started_at | date: 'dd/MM/yyyy HH:mm' }} — {{ l.status }} — {{ l.message }} (par {{ l.triggered_by }})
            </p>
          }
        </section>
      } @empty {
        @if (!loadError()) { <p class="text-slate-500">Chargement…</p> }
      }
    </div>
  `,
})
export class AdminSuppliers {
  private readonly http = inject(HttpClient);
  protected readonly suppliers = signal<SupplierStatus[]>([]);
  protected readonly loadError = signal<string | null>(null);
  protected readonly busy = signal<string | null>(null);
  protected readonly messages = signal<Record<string, { ok: boolean; message: string }>>({});

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      this.suppliers.set(await firstValueFrom(this.http.get<SupplierStatus[]>('/api/admin/suppliers')));
    } catch (err) {
      this.loadError.set(ApiError.from(err).message);
    }
  }

  protected async run(code: string, action: 'test' | 'sync'): Promise<void> {
    this.busy.set(code + action);
    try {
      const r = await firstValueFrom(this.http.post<{ ok: boolean; message: string }>(`/api/admin/suppliers/${code}/${action}`, {}));
      this.messages.update((m) => ({ ...m, [code]: r }));
      await this.load();
    } catch (err) {
      this.messages.update((m) => ({ ...m, [code]: { ok: false, message: ApiError.from(err).message } }));
    } finally {
      this.busy.set(null);
    }
  }
}
