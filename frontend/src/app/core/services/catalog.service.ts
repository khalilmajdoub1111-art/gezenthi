import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { HERO_IMAGES, PROMO_IMAGES, SERVICES, TESTIMONIALS } from '../data/mock-catalog';
import { CatalogData, ServiceItem, Testimonial } from '../models/travel.models';

const EMPTY: CatalogData = { hotels: [], trips: [], omra: [], flights: [], services: [] };

/**
 * Read-only catalogue for the PUBLIC site, from GET /api/public/catalog
 * (published items only, filtered by the server). Loaded once per visit.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly data = signal<CatalogData>(EMPTY);
  readonly status = signal<'loading' | 'ready' | 'error'>('loading');

  readonly hotels = computed(() => this.data().hotels);
  readonly trips = computed(() => this.data().trips);
  readonly omra = computed(() => this.data().omra);
  readonly flights = computed(() => this.data().flights);
  readonly services = computed(() => this.data().services);

  constructor() {
    this.http.get<CatalogData>('/api/public/catalog').subscribe({
      next: (catalog) => {
        this.data.set(catalog);
        this.status.set('ready');
      },
      error: () => this.status.set('error'),
    });
  }

  heroImages(): string[] {
    return HERO_IMAGES;
  }

  promoImages(): typeof PROMO_IMAGES {
    return PROMO_IMAGES;
  }

  serviceShortcuts(): ServiceItem[] {
    return SERVICES;
  }

  testimonials(): Testimonial[] {
    return TESTIMONIALS;
  }
}
