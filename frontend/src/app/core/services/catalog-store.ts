import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, WritableSignal, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CatalogData, CatalogKey } from '../models/travel.models';
import { ApiError } from './api-error';

type Item<K extends CatalogKey> = CatalogData[K][number];

/**
 * Admin-side catalogue, backed by the Spring Boot API (/api/admin/{key}).
 * Holds one signal per offer type; every write goes to the server first and
 * the local list is only updated with what the server returned.
 */
@Injectable({ providedIn: 'root' })
export class CatalogStore {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin';

  private readonly state: { [K in CatalogKey]: WritableSignal<CatalogData[K]> } = {
    hotels: signal([]),
    trips: signal([]),
    omra: signal([]),
    flights: signal([]),
    services: signal([]),
  };
  readonly loadError = signal<string | null>(null);
  readonly loading = signal(false);

  /** Current list for a type (call refresh() to fetch it from the server). */
  list<K extends CatalogKey>(key: K): Signal<CatalogData[K]> {
    return this.state[key].asReadonly();
  }

  async refresh(...keys: CatalogKey[]): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all(
        keys.map(async (key) => {
          const items = await firstValueFrom(this.http.get<CatalogData[typeof key]>(`${this.base}/${key}`));
          this.state[key].set(items as never);
        }),
      );
      this.loadError.set(null);
    } catch (err) {
      this.loadError.set(ApiError.from(err).message);
    } finally {
      this.loading.set(false);
    }
  }

  async get<K extends CatalogKey>(key: K, id: string): Promise<Item<K>> {
    try {
      return await firstValueFrom(this.http.get<Item<K>>(`${this.base}/${key}/${encodeURIComponent(id)}`));
    } catch (err) {
      throw ApiError.from(err);
    }
  }

  /** Creates (no id) or updates (with id). Returns the item as saved by the server. */
  async save<K extends CatalogKey>(key: K, item: Partial<Item<K>>): Promise<Item<K>> {
    const { id, updatedAt: _ignored, ...body } = item as Item<K>;
    try {
      const saved = id
        ? await firstValueFrom(this.http.put<Item<K>>(`${this.base}/${key}/${encodeURIComponent(id)}`, body))
        : await firstValueFrom(this.http.post<Item<K>>(`${this.base}/${key}`, body));
      const current = this.state[key]() as Item<K>[];
      const next = current.some((i) => i.id === saved.id) ? current.map((i) => (i.id === saved.id ? saved : i)) : [saved, ...current];
      this.state[key].set(next as CatalogData[K]);
      return saved;
    } catch (err) {
      throw ApiError.from(err);
    }
  }

  async remove<K extends CatalogKey>(key: K, id: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete<void>(`${this.base}/${key}/${encodeURIComponent(id)}`));
      this.state[key].update((list) => (list as Item<K>[]).filter((i) => i.id !== id) as CatalogData[K]);
    } catch (err) {
      throw ApiError.from(err);
    }
  }
}
