import { Injectable, Signal, WritableSignal, signal } from '@angular/core';
import { SEED_CATALOG } from '../data/mock-catalog';
import { CatalogData, CatalogKey } from '../models/travel.models';

const STORAGE_KEY = 'gezenthi.catalog.v1';

export class StorageFullError extends Error {}

/**
 * Admin-managed catalogue (hotels, trips, Omra, flights, services).
 *
 * PROTOTYPE PERSISTENCE: data lives in this browser's localStorage, so each
 * browser has its own copy. When the Spring Boot API exists, replace load/persist
 * with HTTP calls; the public signature (list/get/save/remove) stays the same.
 */
@Injectable({ providedIn: 'root' })
export class CatalogStore {
  private readonly state: { [K in CatalogKey]: WritableSignal<CatalogData[K]> };

  constructor() {
    const initial = this.load();
    this.state = {
      hotels: signal(initial.hotels),
      trips: signal(initial.trips),
      omra: signal(initial.omra),
      flights: signal(initial.flights),
      services: signal(initial.services),
    };
  }

  list<K extends CatalogKey>(key: K): Signal<CatalogData[K]> {
    return this.state[key].asReadonly();
  }

  get<K extends CatalogKey>(key: K, id: string): CatalogData[K][number] | undefined {
    return (this.state[key]() as CatalogData[K][number][]).find((item) => item.id === id);
  }

  /** Inserts a new item (no id / unknown id) or replaces the existing one. */
  save<K extends CatalogKey>(key: K, item: CatalogData[K][number]): void {
    const current = this.state[key]() as CatalogData[K][number][];
    const exists = current.some((i) => i.id === item.id);
    const next = exists ? current.map((i) => (i.id === item.id ? item : i)) : [item, ...current];
    this.commit(key, next as CatalogData[K]);
  }

  remove<K extends CatalogKey>(key: K, id: string): void {
    const next = (this.state[key]() as CatalogData[K][number][]).filter((i) => i.id !== id);
    this.commit(key, next as CatalogData[K]);
  }

  /** Restores the original demo content. */
  reset(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* storage unavailable: in-memory reset only */
    }
    const seed = structuredClone(SEED_CATALOG);
    (Object.keys(seed) as CatalogKey[]).forEach((k) => this.state[k].set(seed[k] as never));
  }

  private commit<K extends CatalogKey>(key: K, value: CatalogData[K]): void {
    const previous = this.state[key]();
    this.state[key].set(value);
    try {
      this.persist();
    } catch (err) {
      this.state[key].set(previous); // keep UI and storage consistent
      throw err;
    }
  }

  private persist(): void {
    const snapshot = Object.fromEntries((Object.keys(this.state) as CatalogKey[]).map((k) => [k, this.state[k]()]));
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
    } catch (err) {
      if (err instanceof DOMException && err.name === 'QuotaExceededError') {
        throw new StorageFullError('Stockage du navigateur plein : utilisez des images plus légères ou des liens URL.');
      }
      throw err;
    }
  }

  private load(): CatalogData {
    const seed = structuredClone(SEED_CATALOG);
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return seed;
      const saved = JSON.parse(raw) as Partial<CatalogData>;
      // Fall back per collection so a missing/corrupt key never breaks the site.
      return {
        hotels: Array.isArray(saved.hotels) ? saved.hotels : seed.hotels,
        trips: Array.isArray(saved.trips) ? saved.trips : seed.trips,
        omra: Array.isArray(saved.omra) ? saved.omra : seed.omra,
        flights: Array.isArray(saved.flights) ? saved.flights : seed.flights,
        services: Array.isArray(saved.services) ? saved.services : seed.services,
      };
    } catch {
      return seed;
    }
  }
}
